/*
 * Copyright Chris Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package org.jmin.bee.pool;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.jmin.bee.BeeDataSourceConfig;
import org.jmin.bee.pool.util.ConnectionUtil;
import org.jmin.bee.pool.util.SystemClock;

/**
 * JDBC Connection Pool Implementation
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public class ConnectionPool{
	// pool state
	private final int STATE_UNINIT = 0;
	private final int STATE_NORMAL = 1;
	private final int STATE_CLOSED = 2;
	private volatile int state=STATE_UNINIT;
	// pool configuration
	private BeeDataSourceConfig poolInfo;
	// idle connection scan timer
	private Timer connectionIdleCheckTimer;
	// hook object will be called when JVM exit
	private ConnectionPoolHook connectionPoolHook;
	// a test SQL to check connection active state
	private boolean connecitonTestSQLIsNull;
	
	private TransferPolicy transferPolicy;
	private ConnectionFactory connectionFactory;
	private final AtomicInteger conCurSize = new AtomicInteger(0);
	private final AtomicInteger waiterSize = new AtomicInteger(0);
	private final AtomicBoolean isSurpportSetQueryTimeout = new AtomicBoolean(true);
	private final CopyOnWriteArrayList<PooledConnection> connList=new CopyOnWriteArrayList<PooledConnection>();
	private final SynchronousQueue<PooledConnection> transferQueue = new SynchronousQueue<PooledConnection>(true);
	private final ThreadLocal<WeakReference<PooledConnectionBorrower>> borrowerThreadLocal = new ThreadLocal<WeakReference<PooledConnectionBorrower>>();

	//timeout for holding by borrower but not active：milliseconds
	private final long MAX_IDLE_TIME_IN_USING = 600000;
	// a connection pool check exception when visit
	private final SQLException PoolFailStateException = new SQLException("Pool not initialize or closed");
	// a connection pool check exception when
	private final SQLException PoolCloseStateException = new SQLException("Pool has closed");
	// exception will be thrown to borrower,where request timeout
	private final SQLException ConnectionRequestTimeoutException = new SQLException("Request timeout");
	
	/**
	 * initialize pool with configuration
	 * 
	 * @param poolInfo data source configuration
	 * @throws SQLException check configuration fail or to create initiated connection 
	 */
	public ConnectionPool(BeeDataSourceConfig poolInfo) throws SQLException {
		if (poolInfo == null)
			throw new SQLException("Connection info can't be null");
	 
		if (this.state == STATE_UNINIT) {
			poolInfo.check();
			this.checkProxyClasss();
			this.poolInfo = poolInfo;
			this.poolInfo.setInited(true);
			this.connecitonTestSQLIsNull = ConnectionUtil.isNull(poolInfo.getValidationQuerySQL());
			
			this.connectionIdleCheckTimer = new Timer(true);
			this.connectionIdleCheckTimer.schedule(new PooledConnectionIdleTask(this), 60000, 180000);
			this.connectionPoolHook = new ConnectionPoolHook(this);
			Runtime.getRuntime().addShutdownHook(this.connectionPoolHook);
			this.state = STATE_NORMAL;
 
			String mode = "";
			if (poolInfo.isFairMode()) {
				mode = "fair";
				this.transferPolicy = new FairTransferPolicy(this);
			} else {
				mode = "compete";
				this.transferPolicy = new CompeteTransferPolicy(this);
			}
			
			this.connectionFactory = new ConnectionFactory(poolInfo.getDriverURL(),poolInfo.getJdbcProperties(),poolInfo.getJdbcConnectionDriver());
			this.createInitConnections();
			System.out.println("BeeCP has been startup{init size:" + this.conCurSize.get() + ",max size:" + poolInfo.getPoolMaxSize() + ",mode:" + mode + "}");
		} else {
			throw new SQLException("Pool has been initialized");
		}
	}

	/**
	 * check some proxy class whether exists
	 */
	private void checkProxyClasss() throws SQLException {
		try {
			ClassLoader classLoader = this.getClass().getClassLoader();
			Class.forName("org.jmin.bee.pool.ProxyConnectionImpl", true, classLoader);
			Class.forName("org.jmin.bee.pool.ProxyStatementImpl", true, classLoader);
			Class.forName("org.jmin.bee.pool.ProxyPsStatementImpl", true, classLoader);
			Class.forName("org.jmin.bee.pool.ProxyCsStatementImpl", true, classLoader);
			Class.forName("org.jmin.bee.pool.ProxyResultSetImpl", true, classLoader);
		} catch (ClassNotFoundException e) {
			throw new SQLException("Some pool jdbc proxy classes are missed");
		}
	}
	
	protected boolean existWaiting() {
		return this.isNormal() && this.waiterSize.get() > 0;
	}

	protected void clearAllWaiting() {
		this.waiterSize.set(0);
		this.transferQueue.clear();
	}

	private boolean isNormal() {
		return (this.state == STATE_NORMAL);
	}

	private void checkPool() throws SQLException {
		if (!this.isNormal())
			throw PoolFailStateException;
	}
	
	/**
	 * check connection state,when
	 * @return if the checked connection is active then return true,otherwise false     
	 */
	private boolean isActivePooledConnection(final PooledConnection pooledConnection) {
		long timeout = SystemClock.currentTimeMillis() - pooledConnection.getLastActiveTime();
		if (timeout > poolInfo.getMaxInactiveTimeToCheck()) {
			final Connection connecton = pooledConnection.getPhisicConnection();
			if (this.connecitonTestSQLIsNull) {
				try {
					return connecton.isValid(this.poolInfo.getValidationQueryTimeout());
				} catch (SQLException e) {
					return false;
				}
			} else {
				Statement st = null;
				try {
					st = connecton.createStatement();
					this.setsetQueryTimeout(st);
					st.execute(this.poolInfo.getValidationQuerySQL());
					return true;
				} catch (SQLException e) {
					return false;
				} finally {
					ConnectionUtil.close(st);
				}
			}
		}
		return true;
	}

	private void setsetQueryTimeout(Statement st)throws SQLException {
		if (this.isSurpportSetQueryTimeout.get()) {
			try {
				st.setQueryTimeout(this.poolInfo.getValidationQueryTimeout());
			} catch (SQLException e) {
				this.isSurpportSetQueryTimeout.set(false);
				throw e;
			}
		}
	}

	/**
	 * @param poolConnection
	 *            need check when borrower take it
	 * @return if is valid,then return true,otherwise false;
	 */
	private boolean checkOnBorrowed(PooledConnection poolConnection,List<PooledConnection> badConList) {
		if (this.isActivePooledConnection(poolConnection)) {
			return true;
		} else {
			this.conCurSize.decrementAndGet();
			poolConnection.setConnectionState(PooledConnectionState.CLOSED);
			poolConnection.closePhisicConnection();
			badConList.add(poolConnection);
			return false;
		}
	}
	
	/**
	 * create some idle connections to pool when pool initialization
	 * 
	 * @throws SQLException
	 *             error occurred in creating connections
	 */
	private void createInitConnections() throws SQLException {
		int size = this.poolInfo.getPoolInitSize();
		ArrayList<PooledConnection> tempList = new ArrayList<PooledConnection>(size);
		try {
			for (int i = 0; i < size; i++) {
				Connection con = this.connectionFactory.createConnection();
				tempList.add(new PooledConnection(con, this.poolInfo.getPreparedStatementCacheSize(), this));
			}
		} catch (SQLException e) {
			Iterator<PooledConnection> itor = tempList.iterator();
			while (itor.hasNext()) {
				PooledConnection selectedPooledConnection = itor.next();
				selectedPooledConnection.setConnectionState(PooledConnectionState.CLOSED);
				selectedPooledConnection.closePhisicConnection();
				itor.remove();
			}
			throw e;
		}
		this.connList.addAll(tempList);
		this.conCurSize.set(tempList.size());
	}
	
	/**
	 * search an idle connection for borrower
	 * @return return a connection to borrower when exists idle,otherwise return  null   
	 */
	private PooledConnection searchOneIdleConnection() {
		for (final PooledConnection pooledConnection:this.connList) {
			if (pooledConnection.compareAndSet(PooledConnectionState.IDLE, PooledConnectionState.USING)) {
				return pooledConnection;
			}
		}
		return null;
	}

	/**
	 * add some new connections to pool and return the first to current borrower
	 */
	private PooledConnection createOneNewConneciton()throws SQLException {
		if (this.conCurSize.incrementAndGet() <= poolInfo.getPoolMaxSize()) {
			try {
				Connection con = this.connectionFactory.createConnection();
				PooledConnection pooledCon = new PooledConnection(con, poolInfo.getPreparedStatementCacheSize(), this);
				pooledCon.setConnectionState(PooledConnectionState.USING);
				this.connList.add(pooledCon);
				return pooledCon;
			} catch (SQLException e) {
				this.conCurSize.decrementAndGet();
				throw e;
			}
		} else {
			this.conCurSize.decrementAndGet();
		}
		return null;
	}

	/**
	 * borrow a connection from pool
	 * @return If exists idle connection in pool,then return one;if not, waiting until other borrower release
	 * @throws SQLException if pool is closed or waiting timeout,then throw exception
	 */
	public Connection getConnection() throws SQLException {
		return this.getConnection(poolInfo.getBorrowerMaxWaitTime());
	}

	/**
	 * borrow a connection from pool
	 * 
	 * @param maxWaitMillTime
	 *            max wait time for borrower
	 * @return If exists idle connection in pool,then return one;if not, waiting
	 *         until other borrower release
	 * @throws SQLException
	 *             if pool is closed or waiting timeout,then throw exception
	 */
	public Connection getConnection(final long maxWaitMillTime) throws SQLException {
		this.checkPool();
		PooledConnection pooledCon = null;
		PooledConnection selectedPooledCon = null;
		PooledConnectionBorrower borrower= null;
		WeakReference<PooledConnectionBorrower> borrowerRef=this.borrowerThreadLocal.get();
		if(borrowerRef != null)borrower = borrowerRef.get();
		if(borrower == null) {
			borrower = new PooledConnectionBorrower();
			this.borrowerThreadLocal.set(new WeakReference<PooledConnectionBorrower>(borrower));
		}
		List<PooledConnection> badConList=borrower.getBadConnectionList();
		PooledConnection lastUsedPooledCon=borrower.getLastUsedConnection();
		
		/**
		 * timeoutNanos=maxTimeoutNanos-(System.nanoTime()-requestNanos)
		 * =(maxTimeoutNanos + requestNanos)-System.nanoTime()
		 * =(tempNanosTimeout = maxTimeoutNanos+requestNanos)-System.nanoTime()
		 * =(tempNanosTimeout - System.nanoTime())
		 * 
		 * if(timeoutNanos>0)then {......}
		 */
		 long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(maxWaitMillTime);
		 final long tempNanosTimeout = timeoutNanos + System.nanoTime();
		 try{
			 do {
				 //step 1:try to reuse last connection
				if (lastUsedPooledCon != null
						&& lastUsedPooledCon.compareAndSet(PooledConnectionState.IDLE, PooledConnectionState.USING)){
					if(this.checkOnBorrowed(lastUsedPooledCon,badConList)) {
						pooledCon = lastUsedPooledCon;
						break;
					}else{
						lastUsedPooledCon=null;
						borrower.setLastUsedConnection(null);
					}
				}
				
				// step 2:try to search one idle
				if ((selectedPooledCon= this.searchOneIdleConnection()) != null && this.checkOnBorrowed(selectedPooledCon,badConList)) {
					pooledCon = selectedPooledCon;
					break;
				}
				
				// step 3:try to create one
				if (this.conCurSize.get() < poolInfo.getPoolMaxSize() && (pooledCon=this.createOneNewConneciton()) != null) {
					break;
				}
				
				// step 4:just waiting one transfered from other
				if ((timeoutNanos=tempNanosTimeout - System.nanoTime())<=0)timeoutNanos=1000;
				if ((selectedPooledCon = this.waitOneFromOtherTransfer(timeoutNanos)) != null
						&& transferPolicy.tryCatchReleasedConnection(selectedPooledCon)
						&& this.checkOnBorrowed(selectedPooledCon,badConList)) {
					
					pooledCon = selectedPooledCon;
					break;
				}
			} while (this.isNormal() && tempNanosTimeout - System.nanoTime() > 0);//while 
			
			if(!this.isNormal())throw PoolCloseStateException;
		
			if (pooledCon != null) {
				borrower.setLastUsedConnection(pooledCon);
				ProxyConnection proxyConnection = ProxyConnectionFactory.createProxyConnection(pooledCon);
				pooledCon.bindProxyConnection(proxyConnection);
				pooledCon.updateLastActivityTime();
				return proxyConnection;
			} else {
				throw ConnectionRequestTimeoutException;
			}
		}finally{
			if (!badConList.isEmpty()) {
				if(this.isNormal())
				  this.connList.removeAll(badConList);
				badConList.clear();
			}
		}
	}

	/**
	 * borrower thread will call this method to waiting until other borrower
	 * transfer one to it,during max allowable time
	 * 
	 * @param timeoutNanos
	 *            max waiting Nanoseconds
	 * @return if take a transfered connection then return it;if timeout or
	 *         thread interrupted, then return null
	 */
	protected PooledConnection waitOneFromOtherTransfer(long timeoutNanos) {
		try {
			this.waiterSize.incrementAndGet();
			return this.transferQueue.poll(timeoutNanos, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			return null;
		} finally {
			this.waiterSize.decrementAndGet();
		}
	}
	
	/**
	 * the method called to return a used connection to pool. if exists waiting
	 * borrowers at releasing moment,then try to transfer the connection to one
	 * of them
	 * 
	 * @param pooledConnection
	 *            target connection need release
	 * @throws SQLException
	 *             if error occurred,then throws exception
	 */
	public void releasePooledConnection(final PooledConnection pooledConnection) throws SQLException {
		 this.transferPolicy.tryTransferToWaiter(pooledConnection);
	}

	/**
	 * inner timer will call the method to clear some idle timeout connections
	 * or dead connections,or long time not active connections in using state
	 */
	public void closeIdleTimeoutConnection() {
		if (this.isNormal() && !this.existWaiting()) {
			LinkedList<PooledConnection> badConList = new LinkedList<PooledConnection>();
			for (final PooledConnection pooledConnection:this.connList) {
				final int state = pooledConnection.getConnectionState();
				if (state == PooledConnectionState.IDLE) {
					final boolean isDead = !this.isActivePooledConnection(pooledConnection);
					final boolean isTimeout = ((SystemClock.currentTimeMillis() - pooledConnection.getLastActiveTime() >= poolInfo.getConnectionIdleTimeout()));
					if ((isDead || isTimeout) && (pooledConnection.compareAndSet(state, PooledConnectionState.CLOSED))) {
						this.conCurSize.decrementAndGet();
						pooledConnection.closePhisicConnection();
						badConList.add(pooledConnection);
					}

				} else if (state == PooledConnectionState.USING) {
					final boolean isDead = !this.isActivePooledConnection(pooledConnection);
					final boolean isTimeout = ((SystemClock.currentTimeMillis() - pooledConnection.getLastActiveTime() >= MAX_IDLE_TIME_IN_USING));
					if ((isDead || isTimeout) && (pooledConnection.compareAndSet(state, PooledConnectionState.CLOSED))) {
						this.conCurSize.decrementAndGet();
						pooledConnection.closePhisicConnection();
						badConList.add(pooledConnection);
					}
				} else if (state == PooledConnectionState.CLOSED) {
					pooledConnection.closePhisicConnection();
					badConList.add(pooledConnection);
				}
			}
		 
			if (!badConList.isEmpty()) {
				this.connList.removeAll(badConList);
				badConList.clear();
				badConList = null;
			}
		}
	}

	/**
	 * resource release on pool closing
	 */
	public void destroy() {
		if (this.isNormal()) {
			this.state = STATE_CLOSED;
			this.connectionIdleCheckTimer.cancel();
			this.clearAllWaiting();
			
			for (final PooledConnection pooledConnection:this.connList) {
				pooledConnection.setConnectionState(PooledConnectionState.CLOSED);
				pooledConnection.closePhisicConnection();
			}
			this.connList.clear();
			this.conCurSize.set(0);
			try {
				Runtime.getRuntime().removeShutdownHook(this.connectionPoolHook);
			} catch (Throwable e) {}
			System.out.println("BeeCP has been shutdown");
		}
	}

	/**
	 * a inner task to scan idle timeout connections or dead
	 */
	private class PooledConnectionIdleTask extends TimerTask {
		private ConnectionPool poolReference;

		public PooledConnectionIdleTask(ConnectionPool connectionPool) {
			this.poolReference = connectionPool;
		}

		public void run() {
			this.poolReference.closeIdleTimeoutConnection();
		}
	}

	/**
	 * Hook when JVM exit
	 */
	 class ConnectionPoolHook extends Thread {
		private ConnectionPool pool;

		public ConnectionPoolHook(ConnectionPool connectionPool) {
			this.pool = connectionPool;
		}

		public void run() {
			pool.destroy();
		}
	}
	
	 class ConnectionFactory {
		private String jdbcURL;
		private Properties jdbcProperties;
		private Driver jdbcConnectionDriver;
		public ConnectionFactory(String jdbcURL,Properties jdbcProperties,Driver jdbcConnectionDriver) throws SQLException {
			this.jdbcURL=jdbcURL;
			this.jdbcProperties=jdbcProperties;
			this.jdbcConnectionDriver=jdbcConnectionDriver;
		}
		public Connection createConnection() throws SQLException {
			if (jdbcConnectionDriver!=null) {
				return jdbcConnectionDriver.connect(jdbcURL,this.jdbcProperties);
			} else {
				return DriverManager.getConnection(jdbcURL,this.jdbcProperties);
			}
		}
	}
	
	/**
	 * Connection transfer
	 */
	interface TransferPolicy {
		public void tryTransferToWaiter(PooledConnection con);
		public boolean tryCatchReleasedConnection(PooledConnection con);
	}
	class FairTransferPolicy implements TransferPolicy {
		private ConnectionPool pool;
		public FairTransferPolicy(ConnectionPool pool) {
			this.pool = pool;
		}
		public void tryTransferToWaiter(PooledConnection pooledConnection) {
			int failTimes = 0;
			while (pool.existWaiting()) {
				if (pool.transferQueue.offer(pooledConnection)) {
					return;
				} else if (++failTimes%50==0) {
 					LockSupport.parkNanos(10);
				} else {
					Thread.yield();
				}
			}
			pooledConnection.setConnectionState(PooledConnectionState.IDLE);
		}
		public boolean tryCatchReleasedConnection(PooledConnection pooledConnection) {
			return true;
		}
	}

	class CompeteTransferPolicy implements TransferPolicy {
		private ConnectionPool pool;
		public CompeteTransferPolicy(ConnectionPool pool) {
			this.pool = pool;
		}
		public void tryTransferToWaiter(PooledConnection pooledConnection) {
			int failTimes=0,state=0;
			pooledConnection.setConnectionState(PooledConnectionState.IDLE);
			while (pool.existWaiting()) {
				state = pooledConnection.getConnectionState();
				if(state==PooledConnectionState.USING){
					return;
				}else if (state==PooledConnectionState.IDLE && pool.transferQueue.offer(pooledConnection)){ 
					return;
				} else if (++failTimes%50==0) {
					LockSupport.parkNanos(10);
				} else {
					Thread.yield();
				}
			}
		}
		public boolean tryCatchReleasedConnection(PooledConnection pooledConnection) {
			return pooledConnection.compareAndSet(PooledConnectionState.IDLE, PooledConnectionState.USING);
		}
	}
}
