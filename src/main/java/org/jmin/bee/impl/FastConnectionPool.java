/*
 * Copyright (C) Chris Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmin.bee.impl;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.jmin.bee.BeeConnectionPool;
import org.jmin.bee.BeeDataSourceConfig;
import org.jmin.bee.impl.util.ConnectionUtil;

/**
 * JDBC Connection Pool Implementation
 * 
 * @author Chris
 * @version 1.0
 */
public class FastConnectionPool implements BeeConnectionPool{
   //pool state 
	private volatile int state;
	private final int STATE_UNINIT=0;
	private final int STATE_NORMAL=1;
	private final int STATE_CLOSED=2; 
	
	// pool configuration
	private BeeDataSourceConfig poolInfo;
	// idle connection scan timer
	private Timer connectionIdleCheckTimer;
	// hook object will be called when JVM exit
	private ConnectionPoolHook connectionPoolHook;
	// a test SQL to check connection active state
	private boolean connecitonTestSQLIsNull;
	
	// pooled connection instance size
	private final AtomicInteger curPoolSize = new AtomicInteger(0);
	// pooled connection stored list
	private final CopyOnWriteArrayList<PooledConnection> conList = new CopyOnWriteArrayList<PooledConnection>();
	// a cache to store borrower used connection
	private final ThreadLocal<WeakReference<PooledConnection>> borrowThreadLocal = new ThreadLocal<WeakReference<PooledConnection>>();
	// waiter size
	private final AtomicInteger waitingSize = new AtomicInteger(0);
	// transfer queue
	private final SynchronousQueue<PooledConnection> transferQueue = new SynchronousQueue<PooledConnection>(true);
	
	//notify an asynchronized thread to remove bad connections to 'conList'
	private final ReentrantLock removeBadConnectionLock = new ReentrantLock();
	// notification Condition
	private final Condition removeBadConnectionCondtion = removeBadConnectionLock.newCondition();
	
	// an asynchronized thread to remove bad connection to 'conList'
	private final AsynRemovePooledConnectionThread asynRemovePooledConnectionThread = new AsynRemovePooledConnectionThread(this);

	// timeout for holding by borrower but not active
	protected final long MAX_IDLE_TIME_IN_USING = 600000;
	// a connection pool check exception where visit
	private final SQLException PoolFailStateException = new SQLException("Connection pool not initialize or closed" );
	// exception will be thrown to borrower,where request timeout
	private final SQLException ConnectionRequestTimeoutException = new SQLException("Connection request timeout");
	private volatile boolean isSurpportSetQueryTimeout=true;
	/**
	 * initialize pool by configuration
	 */
	public void init(BeeDataSourceConfig poolInfo) throws SQLException {
		if (poolInfo == null)
			throw new SQLException("Connection info can't be null");
		if (poolInfo.getConnectionFactory() == null)
			throw new SQLException("Connection factory can't be null");

		if (this.state==STATE_UNINIT) {
			poolInfo.check();
			this.poolInfo = poolInfo;
			this.poolInfo.setInited(true);
			this.connecitonTestSQLIsNull=ConnectionUtil.isNull(poolInfo.getConnectionValidateSQL());
			
			this.createInitConnection();
			this.asynRemovePooledConnectionThread.start();
			this.connectionIdleCheckTimer = new Timer(true);
			this.connectionIdleCheckTimer.schedule(new PooledConnectionIdleTask(this), 60000, 180000);
			this.connectionPoolHook = new ConnectionPoolHook(this);
			Runtime.getRuntime().addShutdownHook(this.connectionPoolHook);
			this.state = STATE_NORMAL;
		} else {
			throw new SQLException("Connection pool has been initliazed");
		}
	}

	/**
	 * create some connections in pool where pool initialization
	 */
	private void createInitConnection() throws SQLException {
		final LinkedList<PooledConnection> tempList = new LinkedList<PooledConnection>();
		try {
			for (int i = 0, n = this.poolInfo.getPoolInitSize(); i < n; i++) {
				Connection con = this.poolInfo.getConnectionFactory().createConnection();
				tempList.add(new PooledConnection(con, this.poolInfo.getPreparedStatementCacheSize()));
			}
		} catch (SQLException e) {
			Iterator<PooledConnection> itor = tempList.iterator();
			while (itor.hasNext()) {
				PooledConnection tempPooledConnection = itor.next();
				tempPooledConnection.setState(PooledConnectionState.CLOSED);
				tempPooledConnection.closePhisicConnection();
				itor.remove();
			}
			throw new SQLException(e);
		}

		this.conList.addAll(tempList);
		this.curPoolSize.set(tempList.size());
	}
	
	
	/**
	 * clear waiters 
	 */
	protected void clearWaiters() {
		this.waitingSize.set(0);
	}
	
	/**
	 * check whether exist waiters
	 */
	protected boolean existWaiters() {
		return this.waitingSize.get()>0;
	}
	
	/**
	 * check pool state
	 */
	private boolean isNormal(){
		return (this.state == STATE_NORMAL);
	}
	
	/**
	 * check pool state
	 */
	private boolean isClosed(){
		return (this.state == STATE_CLOSED);
	}
	
	/**
	 * check pool state for borrower
	 */
	private void checkPool() throws SQLException {
		if (this.state != STATE_NORMAL)
			throw PoolFailStateException;
	}

	/**
	 * try to search a idle connection for borrower
	 */
	private PooledConnection searchOneIdleConnection() {
		for (final PooledConnection pooledConnection : this.conList) {
			if (pooledConnection.compareAndSet(PooledConnectionState.IDLE, PooledConnectionState.USING)) {
				return pooledConnection;
			}
		}
		return null;
	}
	
	/**
	 * test pool connection is whether active
	 */
	private boolean isActivePooledConnection(final PooledConnection pooledConnection) {
		long timeout = ConnectionUtil.getTimeMillis()-pooledConnection.getLastActiveTime();
		if (timeout > poolInfo.getNeedCheckTimeAfterActive()) {
			final Connection connecton = pooledConnection.getPhisicConnection();
			if (connecitonTestSQLIsNull) {
				try {
					return connecton.isValid(this.poolInfo.getConnectionValidateTimeout());
				} catch (SQLException e) {
					return false;
				}
			} else {
				Statement st = null;
				try {
					st = connecton.createStatement();
				  this.setsetQueryTimeout(st);
					st.execute(this.poolInfo.getConnectionValidateSQL());
					return true;
				} catch (SQLException e) {
					return false;
				} finally {
					try {
						if(st != null)
							st.close();
					} catch (Throwable e) {}
				}
			}
		}
		return true;
	}

	private void setsetQueryTimeout(Statement st) {
		if(isSurpportSetQueryTimeout){
			try {
				st.setQueryTimeout(this.poolInfo.getConnectionValidateTimeout());
			} catch (SQLException e) {
				isSurpportSetQueryTimeout = false;
			}
		}
	}
	
	/**
	 * borrow connection
	 */
	public Connection getConnection() throws SQLException {
		return this.getConnection(poolInfo.getBorrowerMaxWaitTime());
	}

	/**
	 * main logic for connection borrow
	 */
	public Connection getConnection(final long maxWaitTime) throws SQLException {
		this.checkPool();
		boolean needRemoveBad=false;
		boolean isFromThreadLocal = false;
		PooledConnection pooledConnection = null;
		PooledConnection tempPooledConnection = null;
		PooledConnection usedPooledConnection = null;
		final long maxWaitNanoTime = TimeUnit.MILLISECONDS.toNanos(maxWaitTime);
		WeakReference<PooledConnection> weakReference = this.borrowThreadLocal.get();
		if (weakReference != null) {
			usedPooledConnection = weakReference.get();
		}
		
		final long requestTime = ConnectionUtil.getNanoTime();// request time
		final long nanoTimeTemp = maxWaitNanoTime + requestTime;
		long curMaxWaitNanoTime = nanoTimeTemp - ConnectionUtil.getNanoTime();// A+C-B=A-(B-C)
		
		do {
			if (!this.existWaiters()) {// no waiters then search
				if (usedPooledConnection != null
						&& (usedPooledConnection.compareAndSet(PooledConnectionState.IDLE, PooledConnectionState.USING))) {
					if (this.isActivePooledConnection(usedPooledConnection)) {
						isFromThreadLocal = true;
						pooledConnection = usedPooledConnection;
						break;
					} else {
						this.curPoolSize.decrementAndGet();
						usedPooledConnection.setState(PooledConnectionState.CLOSED);
						this.borrowThreadLocal.set(null);
						usedPooledConnection = null;
						needRemoveBad=true;
					}
				}

				if ((tempPooledConnection = this.searchOneIdleConnection()) == null) {
					if (this.curPoolSize.get() < this.poolInfo.getPoolMaxSize()) {// try to create new connection																																 
						if (this.curPoolSize.incrementAndGet() > this.poolInfo.getPoolMaxSize()) {
							this.curPoolSize.decrementAndGet();
						} else {
							Connection con = this.poolInfo.getConnectionFactory().createConnection();				
							pooledConnection = new PooledConnection(con, this.poolInfo.getPreparedStatementCacheSize());
							pooledConnection.setState(PooledConnectionState.USING);
							this.conList.add(pooledConnection);
							break;
						}
					}
				} else{ 
					if (this.isActivePooledConnection(tempPooledConnection)) {
						pooledConnection = tempPooledConnection;
						break;
					} else {
						this.curPoolSize.decrementAndGet();
						tempPooledConnection.setState(PooledConnectionState.CLOSED);
						needRemoveBad = true;
					}
				}
			}

			// not find a connection,then wait
			curMaxWaitNanoTime = nanoTimeTemp - ConnectionUtil.getNanoTime();
			if ((tempPooledConnection = this.waitTransferFromOthers((curMaxWaitNanoTime>0)?curMaxWaitNanoTime:0)) != null) {
				if (this.isActivePooledConnection(tempPooledConnection)) {
					pooledConnection = tempPooledConnection;
					break;
				} else {
					this.curPoolSize.decrementAndGet();
					tempPooledConnection.setState(PooledConnectionState.CLOSED);
					needRemoveBad=true;
				}
			}
		} while ((curMaxWaitNanoTime = nanoTimeTemp - ConnectionUtil.getNanoTime()) > 0);  
 
		try {
			if (pooledConnection != null) {
				if(!isFromThreadLocal)this.borrowThreadLocal.set(new WeakReference<PooledConnection>(pooledConnection));
				ProxyConnection proxyConnection = ProxyConnectionFactory.createProxyConnection(pooledConnection, this);
				pooledConnection.setProxyConnection(proxyConnection);
				return proxyConnection;
			} else {
				throw ConnectionRequestTimeoutException;
			}
		} finally {
			if(needRemoveBad) 
				this.notifyToAsynRemoveThread();
		}
	}
	
	/**
	 * wait transfer
	 */
	public PooledConnection waitTransferFromOthers(final long curMaxWaitTime) {
		this.waitingSize.incrementAndGet();
		try {
			return this.transferQueue.poll(curMaxWaitTime, TimeUnit.NANOSECONDS);
		} catch (Exception e) {
			return null;
		}finally{
			this.waitingSize.decrementAndGet();
		}
	}
	
	/**
	 * release connection by borrower
	 */
	public void releasePooledConnection(final PooledConnection pooledConnection) throws SQLException {// 外部归还
		if (this.isNormal()) {
			pooledConnection.resetConnectionAfterRelease();
			pooledConnection.setProxyConnection(null);
			pooledConnection.updateLastActivityTime();
			this.afterReleasePooledConnection(pooledConnection);
		}
	}
	
	/**
	 * action after pooled connection release
	 */
	public void afterReleasePooledConnection(final PooledConnection pooledConnection) {
		if(this.waitingSize.get() > 0){
		  this.transferQueue.offer(pooledConnection);
		}else{
			pooledConnection.setState(PooledConnectionState.IDLE);
		}	
	}
	
	/**
	 * notify to remove new connection in 'conList'
	 */
	private void notifyToAsynRemoveThread() {
		try {
			this.removeBadConnectionLock.lock();
			this.removeBadConnectionCondtion.signal();
		} finally {
			this.removeBadConnectionLock.unlock();
		}
	}

	/**
	 * remove idle timeout connection from pool
	 */
	public void closeIdleTimeoutConnection() {
		if (this.isNormal() && !this.existWaiters()) {
			LinkedList<PooledConnection> badList = new LinkedList<PooledConnection>();
			for (final PooledConnection pooledConnection : this.conList) {
				final int state = pooledConnection.getState();
				if (state == PooledConnectionState.IDLE) {
					final boolean isDead = !this.isActivePooledConnection(pooledConnection);
					final boolean isTimeout = ((ConnectionUtil.getTimeMillis()- pooledConnection.getLastActiveTime() >= poolInfo.getConnectionIdleTimeout()));
					if ((isDead || isTimeout)
							&& (pooledConnection.compareAndSet(state, PooledConnectionState.CLOSED))) {
						this.curPoolSize.decrementAndGet();
						pooledConnection.closePhisicConnection();
						badList.add(pooledConnection);
					}

				} else if (state == PooledConnectionState.USING) {
					final boolean isDead = !this.isActivePooledConnection(pooledConnection);
					final boolean isTimeout = ((ConnectionUtil.getTimeMillis() - pooledConnection.getLastActiveTime() >= MAX_IDLE_TIME_IN_USING));
					if ((isDead || isTimeout)
							&& (pooledConnection.compareAndSet(state, PooledConnectionState.CLOSED))) {
						this.curPoolSize.decrementAndGet();
						pooledConnection.closePhisicConnection();
						badList.add(pooledConnection);
					}
				} else if (state == PooledConnectionState.CLOSED) {
					pooledConnection.closePhisicConnection();
					badList.add(pooledConnection);
				}
			}

			if (!badList.isEmpty()) {
				while(!this.conList.removeAll(badList));
				badList.clear();
				badList=null;
			}
		}
	}

	/**
	 * destroy pool:close all connections
	 */
	public void destroy() {
		if (this.isNormal()) {
			this.state = STATE_CLOSED;
			this.connectionIdleCheckTimer.cancel();
			this.notifyToAsynRemoveThread();
			this.clearWaiters();
			PooledConnection pooledConnection = null;
			
			Iterator<PooledConnection> itor = this.conList.iterator();
			while (itor.hasNext()) {
				pooledConnection = itor.next();
				pooledConnection.setState(PooledConnectionState.CLOSED);
				pooledConnection.closePhisicConnection();
			}

			this.conList.clear();
			this.curPoolSize.set(0);
			try {
				Runtime.getRuntime().removeShutdownHook(this.connectionPoolHook);
			} catch (Throwable e) {
			}
		}
	}
	
	/**
	 * asynchronized thread to remove bad connections from 'conList'
	 */
	private class AsynRemovePooledConnectionThread extends Thread {
		private FastConnectionPool pool;
		public AsynRemovePooledConnectionThread(final FastConnectionPool pool) {
			this.pool = pool;
			this.setDaemon(true);
		}
		public void run() {
			while (this.pool.isNormal()) {
				try {
					this.pool.removeBadConnectionLock.lock();
					this.pool.removeBadConnectionCondtion.await();
				} catch (InterruptedException e) {
				} finally {
					this.pool.removeBadConnectionLock.unlock();
				}
				if (this.pool.isClosed())break;
				
				PooledConnection pooledConnection=null;
				Iterator<PooledConnection> itor=	this.pool.conList.iterator();
				while(itor.hasNext()){
					pooledConnection=itor.next();
					if(pooledConnection.getState() == PooledConnectionState.CLOSED){
						this.pool.conList.remove(pooledConnection);
						pooledConnection.closePhisicConnection();
					}
				}
		  }
	  }
	}
	
	/**
	 * idle scan timer task
	 */
	private class PooledConnectionIdleTask extends TimerTask {
		private FastConnectionPool poolReference;
		public PooledConnectionIdleTask(FastConnectionPool connectionPool) {
			this.poolReference = connectionPool;
		}
		public void run() {
			this.poolReference.closeIdleTimeoutConnection();
		}
	}

	/**
	 * Hook when JVM exit
	 */
	private class ConnectionPoolHook extends Thread {
		private FastConnectionPool connectionPool;
		public ConnectionPoolHook(FastConnectionPool connectionPool) {
			this.connectionPool = connectionPool;
		}
		public void run() {
			connectionPool.destroy();
		}
	}
}
