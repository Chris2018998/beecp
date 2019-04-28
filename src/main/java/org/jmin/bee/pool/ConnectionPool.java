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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
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
	private Timer connectionIdleCheckTimer;
	private ConnectionPoolHook connectionPoolHook;
	private boolean connecitonTestSQLIsNull;
	protected final BeeDataSourceConfig poolInfo;
	
	private final boolean isFairMode;
	private final Semaphore takeSemaphore;
	private final TransferPolicy transferPolicy;
	private final ConnectionFactory connectionFactory;
	private final AtomicInteger conCurSize = new AtomicInteger(0);
	private final AtomicInteger waiterSize = new AtomicInteger(0);
	private final PooledConnectionArray conArray=new PooledConnectionArray();
	private final BlockingQueue<PooledConnection> transferQueue = new SynchronousQueue<PooledConnection>(true);	
	private final ThreadLocal<WeakReference<Borrower>> borrowerThreadLocal = new ThreadLocal<WeakReference<Borrower>>();
	
	private final long MAX_IDLE_TIME_IN_USING = 600000L;
	private final SystemClock systemClock=SystemClock.clock;
	private final TimeUnit MillSecondTimeUnit=TimeUnit.MILLISECONDS;
	private SQLException PoolCloseStateException = new SQLException("Pool has been closed");
	private SQLException ConnectionRequestTimeoutException = new SQLException("Request timeout");
	
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
			
			this.isFairMode=poolInfo.isFairMode();
			this.takeSemaphore=new Semaphore(this.poolInfo.getPoolMaxSize()*2,true);
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
		return this.waiterSize.get() > 0;
	}
	protected boolean isNormal() {
		return (this.state == STATE_NORMAL);
	}
	protected boolean isClosed() {
		return (this.state == STATE_CLOSED);
	}
	private void checkPool() throws SQLException {
		if (this.isClosed())
			throw PoolCloseStateException;
	}
	public Map<String,Integer> getPoolSnapshot(){
		int waiterSize = this.waiterSize.get();
		int conCurSize = this.conCurSize.get();
		int conIdleSize=0;
		
		for (PooledConnection pooledConnection:conArray.getArray()) {
			if (pooledConnection.getConnectionState() == PooledConnectionState.IDLE)
				conIdleSize++;
		}
		Map<String,Integer> snapshotMap = new LinkedHashMap<String,Integer>();
		snapshotMap.put("PoolMaxSize", this.poolInfo.getPoolMaxSize());
		snapshotMap.put("ConCurSize", conCurSize);
		snapshotMap.put("ConIdleSize", conIdleSize);
		snapshotMap.put("WaiterSize", waiterSize);
		return snapshotMap;
	}
	
	/**
	 * check connection state,when
	 * @return if the checked connection is active then return true,otherwise false     
	 */
	private boolean isActivePooledConnection(PooledConnection pooledConnection) {
		if (systemClock.currentTimeMillis()-pooledConnection.getLastActiveTime()-poolInfo.getMaxInactiveTimeToCheck()>0) {
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
					this.setsetQueryTimeout(pooledConnection,st);
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

	private void setsetQueryTimeout(PooledConnection pooledConnection,Statement st)throws SQLException {
		if (pooledConnection.isSurpportSetQueryTimeout()) {
			try {
				st.setQueryTimeout(this.poolInfo.getValidationQueryTimeout());
			} catch (SQLException e) {
				pooledConnection.setSurpportSetQueryTimeout(false);
				throw e;
			}
		}
	}

	/**
	 * @param poolConnection
	 *            need check when borrower take it
	 * @param  badConList    
	 * 			collect bad connections    
	 * @return if is valid,then return true,otherwise false;
	 */
	private boolean checkOnBorrowed(PooledConnection poolConnection,List<PooledConnection> badConList) {
		if (this.isActivePooledConnection(poolConnection)) {
			return true;
		} else {
			this.conCurSize.decrementAndGet();
			poolConnection.setConnectionState(PooledConnectionState.CLOSED);
			poolConnection.removeFromPool();
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
				selectedPooledConnection.removeFromPool();
				itor.remove();
			}
			throw e;
		}
		this.conArray.addAll(tempList);
		this.conCurSize.set(tempList.size());
	}

	/**
	 * borrow a connection from pool
	 * @return If exists idle connection in pool,then return one;if not, waiting until other borrower release
	 * @throws SQLException if pool is closed or waiting timeout,then throw exception
	 */
	public Connection getConnection() throws SQLException {
		return this.getConnection(this.poolInfo.getBorrowerMaxWaitTime());
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
		boolean acquired=false;
		Borrower borrower=null;
		PooledConnection pooledCon=null;
		PooledConnection tempPooledCon=null;
		
		WeakReference<Borrower> borrowerRef = this.borrowerThreadLocal.get();
		if (borrowerRef != null)borrower = borrowerRef.get();
		if (borrower == null) {
			borrower = new Borrower();
			this.borrowerThreadLocal.set(new WeakReference<Borrower>(borrower));
		}
		List<PooledConnection> badConList = borrower.getBadConnectionList();
		
		try {
			if ((tempPooledCon = borrower.getLastUsedConnection()) != null // step1
					&& tempPooledCon.compareAndSet(PooledConnectionState.IDLE, PooledConnectionState.USING)) {
				if (this.checkOnBorrowed(tempPooledCon, badConList)) {
					pooledCon = tempPooledCon;
				} else {
					borrower.setLastUsedConnection(null);
				}
			}
			
			if (pooledCon == null) {// try to search one/create one
				long timeout = maxWaitMillTime;
				final long targetTimeoutPoint=systemClock.currentTimeMillis()+timeout;
				try{acquired=!this.isFairMode && this.takeSemaphore.tryAcquire(timeout,MillSecondTimeUnit);}catch (InterruptedException e){}
				
				do {
					if ((pooledCon = this.searchOneConnection(badConList)) != null) //step2
						break;
					if ((pooledCon = this.createOneConneciton()) != null)//step3
						break;
					
					if ((timeout = targetTimeoutPoint - systemClock.currentTimeMillis()) <= 0)
						break;
					if ((tempPooledCon = this.waitRelease(timeout, borrower)) != null
							&& this.transferPolicy.tryCatchReleasedConnection(tempPooledCon) 
						    && this.checkOnBorrowed(tempPooledCon, badConList)) {// step4
							pooledCon = tempPooledCon;
							break;
					}
				} while (this.isNormal() && targetTimeoutPoint - systemClock.currentTimeMillis()>0);// while
			}
		} finally {
			if (acquired)this.takeSemaphore.release();
			if (!badConList.isEmpty()) {
				this.conArray.removeAll(badConList);
				badConList.clear();
			}
		}
	 
		if (pooledCon != null) {
			borrower.setLastUsedConnection(pooledCon);
			ProxyConnection proxyConnection = ProxyConnectionFactory.createProxyConnection(pooledCon);
			pooledCon.bindProxyConnection(proxyConnection);
			pooledCon.updateLastActivityTime();
			return proxyConnection;
		} else if (this.isClosed())
			throw PoolCloseStateException;
		else
			throw ConnectionRequestTimeoutException;
	}
	
	private PooledConnection searchOneConnection(List<PooledConnection> badConList) {
		for (PooledConnection pooledConnection:this.conArray.getArray()) {
			if (pooledConnection.compareAndSet(PooledConnectionState.IDLE, PooledConnectionState.USING)
					&& this.checkOnBorrowed(pooledConnection, badConList)) {
				return pooledConnection;
			}
		}
		return null;
	}
	private PooledConnection createOneConneciton() throws SQLException {
		 final int PoolMaxSize = poolInfo.getPoolMaxSize();	
		 final int PreparedStatementCacheSize=poolInfo.getPreparedStatementCacheSize();
		 
		if (this.conCurSize.get() < PoolMaxSize) {
			if (this.conCurSize.incrementAndGet() <= PoolMaxSize) {
				try {
					Connection con = this.connectionFactory.createConnection();
					PooledConnection pooledCon = new PooledConnection(con,PreparedStatementCacheSize,this);
					pooledCon.setConnectionState(PooledConnectionState.USING);
					this.conArray.add(pooledCon);
					return pooledCon;
				} catch (SQLException e) {
					this.conCurSize.decrementAndGet();
					throw e;
				}
			} else {
				this.conCurSize.decrementAndGet();
			}
		}
		return null;
	}
	protected PooledConnection waitRelease(long timeout,Borrower borrower) {
		try {
			this.waiterSize.incrementAndGet();
			return this.transferQueue.poll(timeout,MillSecondTimeUnit);
		} catch (InterruptedException e) {
			return null;
		} finally {
			this.waiterSize.decrementAndGet();
		}
	}
	
	/**
	 * transfer released connection to a waiter
	 * 
	 * @param pooledConnection need transfered connection
	 * @return if transfer successful then return true,otherwise false
	 */
	protected boolean transferToWaiter(PooledConnection pooledConnection) { 
		return this.transferQueue.offer(pooledConnection);
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
			for (PooledConnection pooledConnection:conArray.getArray()) {
				final int state = pooledConnection.getConnectionState();
				if (state == PooledConnectionState.IDLE) {
					final boolean isDead = !this.isActivePooledConnection(pooledConnection);
					final boolean isTimeout = ((systemClock.currentTimeMillis() - pooledConnection.getLastActiveTime()-poolInfo.getConnectionIdleTimeout()>=0));
					if ((isDead || isTimeout) && (pooledConnection.compareAndSet(state, PooledConnectionState.CLOSED))) {
						this.conCurSize.decrementAndGet();
						pooledConnection.removeFromPool();
						badConList.add(pooledConnection);
					}

				} else if (state == PooledConnectionState.USING) {
					final boolean isDead = !this.isActivePooledConnection(pooledConnection);
					final boolean isTimeout = ((systemClock.currentTimeMillis() - pooledConnection.getLastActiveTime()-MAX_IDLE_TIME_IN_USING >=0));
					if ((isDead || isTimeout) && (pooledConnection.compareAndSet(state, PooledConnectionState.CLOSED))) {
						this.conCurSize.decrementAndGet();
						pooledConnection.removeFromPool();
						badConList.add(pooledConnection);
					}
				} else if (state == PooledConnectionState.CLOSED) {
					pooledConnection.removeFromPool();
					badConList.add(pooledConnection);
				}
			}
		 
			if (!badConList.isEmpty()) {
				this.conArray.removeAll(badConList);
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
			
			while (this.existWaiting()) 
			 LockSupport.parkNanos(10);
			
			//clear all connections
			LinkedList<PooledConnection> badConList = new LinkedList<PooledConnection>();
			while (this.conArray.size() > 0) {
				PooledConnection[]  connListArray = conArray.getArray();
				for (PooledConnection pooledConnection : connListArray) {
					if (pooledConnection.compareAndSet(PooledConnectionState.IDLE, PooledConnectionState.CLOSED)) {
						pooledConnection.removeFromPool();
						badConList.add(pooledConnection);
					} else if (pooledConnection.getConnectionState() == PooledConnectionState.CLOSED) {
						badConList.add(pooledConnection);
					} else if (pooledConnection.getConnectionState() == PooledConnectionState.USING) {
						final boolean isDead = !this.isActivePooledConnection(pooledConnection);
						final boolean isTimeout = ((systemClock.currentTimeMillis()-pooledConnection.getLastActiveTime()-MAX_IDLE_TIME_IN_USING >=0));
						if ((isDead || isTimeout) && (pooledConnection.compareAndSet(PooledConnectionState.USING, PooledConnectionState.CLOSED))) {
							pooledConnection.removeFromPool();
							badConList.add(pooledConnection);
						}
					}
				}//for
				
				if(badConList.size()>0){
					this.conArray.removeAll(badConList);
					badConList.clear();
				}
				if (this.conArray.size() > 0)
					LockSupport.parkNanos(1000);
			}//while
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
	static class FairTransferPolicy implements TransferPolicy {
		private ConnectionPool pool;
		public FairTransferPolicy(ConnectionPool pool) {
			this.pool = pool;
		}
		public void tryTransferToWaiter(PooledConnection pooledConnection) {
			int failTimes = 0;
			while (pool.existWaiting()) {
				if (pool.transferToWaiter(pooledConnection)) {
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

	static class CompeteTransferPolicy implements TransferPolicy {
		private ConnectionPool pool;
		public CompeteTransferPolicy(ConnectionPool pool) {
			this.pool = pool;
		}
		public void tryTransferToWaiter(PooledConnection pooledConnection) {
			int failTimes=0;
			pooledConnection.setConnectionState(PooledConnectionState.IDLE);
			while (pool.existWaiting() && pooledConnection.getConnectionState()== PooledConnectionState.IDLE) {
				if (this.pool.transferToWaiter(pooledConnection)) {
					return;
				} else if (++failTimes % 50 == 0) {
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
