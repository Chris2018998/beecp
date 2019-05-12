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
	private final int STATE_UNINIT = 0;
	private final int STATE_NORMAL = 1;
	private final int STATE_CLOSED = 2;
	private volatile int state=STATE_UNINIT;
	private Timer connectionIdleCheckTimer;
	private ConnectionPoolHook connectionPoolHook;
	private boolean connecitonTestSQLIsNull;
	protected final BeeDataSourceConfig poolInfo;
	
	private final boolean isCompeteMode;
	private final Semaphore takeSemaphore;
	private final TransferPolicy transferPolicy;
	private final ConnectionFactory connectionFactory;
	private final AtomicInteger conCurSize = new AtomicInteger(0);
	protected final AtomicInteger waiterSize = new AtomicInteger(0);
	private final PooledConnectionArray conArray=new PooledConnectionArray();
	private final SynchronousQueue<PooledConnection> transferQueue = new SynchronousQueue<PooledConnection>(true);	
	private final ThreadLocal<WeakReference<Borrower>> borrowerThreadLocal = new ThreadLocal<WeakReference<Borrower>>();
	private volatile boolean isSurpportSetQueryTimeout=true;
	private final long MAX_IDLE_TIME_IN_USING = 600000L;
	private final SystemClock systemClock=SystemClock.clock;
	private final TimeUnit ClockTimeUnit=TimeUnit.MILLISECONDS;
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
	 
		if (state == STATE_UNINIT) {
			poolInfo.check();
			checkProxyClasss();
			this.poolInfo = poolInfo;
			poolInfo.setInited(true);
			connecitonTestSQLIsNull = ConnectionUtil.isNull(poolInfo.getValidationQuerySQL());

			connectionIdleCheckTimer = new Timer(true);
			connectionIdleCheckTimer.schedule(new PooledConnectionIdleTask(this), 60000, 180000);
			connectionPoolHook = new ConnectionPoolHook(this);
			Runtime.getRuntime().addShutdownHook(connectionPoolHook);
			state = STATE_NORMAL;
 
			String mode = "";
			if (poolInfo.isFairMode()) {
				mode = "fair";
				transferPolicy = new FairTransferPolicy(this);
			} else {
				mode = "compete";
				transferPolicy = new CompeteTransferPolicy(this);
			}
			
			isCompeteMode=!poolInfo.isFairMode();
			takeSemaphore=new Semaphore(poolInfo.getPoolMaxSize(),true);
			connectionFactory = new ConnectionFactory(poolInfo.getDriverURL(),poolInfo.getJdbcProperties(),poolInfo.getJdbcConnectionDriver());
			createInitConnections();
			System.out.println("BeeCP has been startup{init size:" + conCurSize.get() + ",max size:" + poolInfo.getPoolMaxSize() + ",mode:" + mode + "}");
		} else {
			throw new SQLException("Pool has been initialized");
		}
	}

	/**
	 * check some proxy class whether exists
	 */
	private void checkProxyClasss() throws SQLException {
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			Class.forName("org.jmin.bee.pool.ProxyConnectionImpl", true, classLoader);
			Class.forName("org.jmin.bee.pool.ProxyStatementImpl", true, classLoader);
			Class.forName("org.jmin.bee.pool.ProxyPsStatementImpl", true, classLoader);
			Class.forName("org.jmin.bee.pool.ProxyCsStatementImpl", true, classLoader);
			Class.forName("org.jmin.bee.pool.ProxyResultSetImpl", true, classLoader);
		} catch (ClassNotFoundException e) {
			throw new SQLException("Some pool jdbc proxy classes are missed");
		}
	}
	
	protected int getWaiterSize() {
		return waiterSize.get();
	}
	protected boolean existWaiter() {
		return getWaiterSize()> 0;
	}
	protected boolean isNormal() {
		return (state == STATE_NORMAL);
	}
	protected boolean isClosed() {
		return (state == STATE_CLOSED);
	}
	private void checkPool() throws SQLException {
		if (isClosed())
			throw PoolCloseStateException;
	}
	public Map<String,Integer> getPoolSnapshot(){
		int waiterSize = this.waiterSize.get();
		int conCurSize = this.conCurSize.get();
		int conIdleSize=0;
		
		if(waiterSize==0){
			for (PooledConnection pooledConnection:conArray.getArray()) {
				if (pooledConnection.getConnectionState() == PooledConnectionState.IDLE)
					conIdleSize++;
			}
		}
		
		Map<String,Integer> snapshotMap = new LinkedHashMap<String,Integer>();
		snapshotMap.put("PoolMaxSize", poolInfo.getPoolMaxSize());
		snapshotMap.put("ConCurSize",conCurSize);
		snapshotMap.put("ConIdleSize",conIdleSize);
		snapshotMap.put("WaiterSize",waiterSize);
		return snapshotMap;
	}
	
	/**
	 * check connection state,when
	 * @return if the checked connection is active then return true,otherwise false     
	 */
	private final boolean isActivePooledConnection(PooledConnection pooledConnection) {
		if (systemClock.currentTimeMillis()-pooledConnection.getLastActiveTime()-poolInfo.getMaxInactiveTimeToCheck()>0) {
			final Connection connecton = pooledConnection.getPhisicConnection();
			if (connecitonTestSQLIsNull) {
				try {
					return connecton.isValid(poolInfo.getValidationQueryTimeout());
				} catch (SQLException e) {
					return false;
				}
			} else {
				Statement st = null;
				try {
					st = connecton.createStatement();
					setsetQueryTimeout(st);
					st.execute(poolInfo.getValidationQuerySQL());
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

	private final void setsetQueryTimeout(Statement st) {
		if(isSurpportSetQueryTimeout){
			try {
				st.setQueryTimeout(poolInfo.getValidationQueryTimeout());
			} catch (SQLException e) {
				isSurpportSetQueryTimeout=false;
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
	private final boolean checkOnBorrowed(PooledConnection poolConnection,List<PooledConnection> badConList) {
		if (isActivePooledConnection(poolConnection)) 
			return true;
		
		conCurSize.decrementAndGet();
		poolConnection.setConnectionState(PooledConnectionState.CLOSED);
		poolConnection.closePhysicalConnection();
		badConList.add(poolConnection);
		return false;
	}
	
	/**
	 * create some idle connections to pool when pool initialization
	 * 
	 * @throws SQLException
	 *             error occurred in creating connections
	 */
	private void createInitConnections() throws SQLException {
		int size = poolInfo.getPoolInitSize();
		ArrayList<PooledConnection> tempList = new ArrayList<PooledConnection>(size);
		try {
			for (int i = 0; i < size; i++) {
				Connection con = connectionFactory.createConnection();
				tempList.add(new PooledConnection(con, poolInfo.getPreparedStatementCacheSize(), this));
			}
		} catch (SQLException e) {
			Iterator<PooledConnection> itor = tempList.iterator();
			while (itor.hasNext()) {
				PooledConnection selectedPooledConnection = itor.next();
				selectedPooledConnection.setConnectionState(PooledConnectionState.CLOSED);
				selectedPooledConnection.closePhysicalConnection();
				itor.remove();
			}
			throw e;
		}
		conArray.addAll(tempList);
		conCurSize.set(tempList.size());
	}

	/**
	 * borrow a connection from pool
	 * @return If exists idle connection in pool,then return one;if not, waiting until other borrower release
	 * @throws SQLException if pool is closed or waiting timeout,then throw exception
	 */
	public Connection getConnection() throws SQLException {
		return getConnection(poolInfo.getBorrowerMaxWaitTime());
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
		checkPool();
		boolean acquired=false;
		Borrower borrower=null;
		PooledConnection pooledCon=null;
		PooledConnection tempPooledCon=null;
		
		WeakReference<Borrower> borrowerRef = borrowerThreadLocal.get();
		if (borrowerRef != null)borrower = borrowerRef.get();
		if (borrower == null) {
			borrower = new Borrower();
			borrowerThreadLocal.set(new WeakReference<Borrower>(borrower));
		}
		List<PooledConnection> badConList = borrower.getBadConnectionList();
		
		try {
			if ((tempPooledCon = borrower.getLastUsedConnection()) != null //step1: try to reuse one
					&& tempPooledCon.compareAndSet(PooledConnectionState.IDLE, PooledConnectionState.USING)) {
				if (checkOnBorrowed(tempPooledCon, badConList)) {
					pooledCon = tempPooledCon;
				} else {
					borrower.setLastUsedConnection(null);
				}
			}
			
			if (pooledCon == null) {
				long timeout = maxWaitMillTime;
				final long targetTimeoutPoint=systemClock.currentTimeMillis()+timeout;
				try{if(isCompeteMode)acquired=takeSemaphore.tryAcquire(timeout,ClockTimeUnit);}catch (InterruptedException e){}
				
				 for(;;){
					if ((pooledCon = searchOneConnection(badConList))!=null)//step2:try to search one 
						break;
					if ((pooledCon = createOneConneciton()) != null)//step3:try to create one
						break;
					
					if ((timeout=targetTimeoutPoint-systemClock.currentTimeMillis())<=0)break;
					if ((tempPooledCon=takeOneTransferConnection(timeout,ClockTimeUnit,borrower))!= null){//step4:wait for transfered one
						if (transferPolicy.tryCatchReleasedConnection(tempPooledCon)&& 
								checkOnBorrowed(tempPooledCon, badConList)) {
								pooledCon = tempPooledCon;
								break;
						}
					}
					
					if(isClosed())break;
				}//for
			}
		} finally {
			if (acquired)takeSemaphore.release();
			if (!badConList.isEmpty()) {
				conArray.removeAll(badConList);
				badConList.clear();
			}
		}
	 
		if (pooledCon != null) {
			borrower.setLastUsedConnection(pooledCon);
			ProxyConnection proxyConnection = ProxyConnectionFactory.createProxyConnection(pooledCon);
			pooledCon.bindProxyConnection(proxyConnection);
			pooledCon.updateLastActivityTime();
			return proxyConnection;
		} else if (isClosed())
			throw PoolCloseStateException;
		else
			throw ConnectionRequestTimeoutException;
	}
	
	private final PooledConnection searchOneConnection(List<PooledConnection> badConList) {
		for (PooledConnection pooledConnection:conArray.getArray()) {
			if (pooledConnection.compareAndSet(PooledConnectionState.IDLE, PooledConnectionState.USING)
					&&checkOnBorrowed(pooledConnection, badConList)) {
				return pooledConnection;
			}
		}
		return null;
	}
	private final PooledConnection createOneConneciton() throws SQLException {
	    final int PoolMaxSize = poolInfo.getPoolMaxSize();	
		final int PreparedStatementCacheSize=poolInfo.getPreparedStatementCacheSize();
		
		if(conCurSize.get() <PoolMaxSize){
			if(conCurSize.incrementAndGet() <= PoolMaxSize) {
				try {
					Connection con = connectionFactory.createConnection();
					 
					
					
					PooledConnection pooledCon = new PooledConnection(con,PreparedStatementCacheSize,this);
					pooledCon.setConnectionState(PooledConnectionState.USING);
					conArray.add(pooledCon);
					return pooledCon;
				} catch (SQLException e) {
					conCurSize.decrementAndGet();
					throw e;
				}
			} else {
				conCurSize.decrementAndGet();
			}
		}
		return null;
	}
	protected PooledConnection takeOneTransferConnection(long timeout,TimeUnit unit,Borrower borrower) {
		try {
			waiterSize.incrementAndGet();
			return transferQueue.poll(timeout,unit);
		}catch(InterruptedException	e){
			return null;
		} finally {
			waiterSize.decrementAndGet();
		}
	}
	
	/**
	 * transfer released connection to a waiter
	 * 
	 * @param pooledConnection need transfered connection
	 * @return if transfer successful then return true,otherwise false
	 */
	protected boolean transferToWaiter(PooledConnection pooledConnection) { 
		return transferQueue.offer(pooledConnection);
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
		 transferPolicy.tryTransferToWaiter(pooledConnection);
	}

	/**
	 * inner timer will call the method to clear some idle timeout connections
	 * or dead connections,or long time not active connections in using state
	 */
	public void closeIdleTimeoutConnection() {
		if (isNormal() && !existWaiter()) {
			LinkedList<PooledConnection> badConList = new LinkedList<PooledConnection>();
			for (PooledConnection pooledConnection:conArray.getArray()) {
				final int state = pooledConnection.getConnectionState();
				if (state == PooledConnectionState.IDLE) {
					final boolean isDead = !isActivePooledConnection(pooledConnection);
					final boolean isTimeout = ((systemClock.currentTimeMillis() - pooledConnection.getLastActiveTime()-poolInfo.getConnectionIdleTimeout()>=0));
					if ((isDead || isTimeout) && (pooledConnection.compareAndSet(state, PooledConnectionState.CLOSED))) {
						conCurSize.decrementAndGet();
						pooledConnection.closePhysicalConnection();
						badConList.add(pooledConnection);
					}

				} else if (state == PooledConnectionState.USING) {
					final boolean isDead = !isActivePooledConnection(pooledConnection);
					final boolean isTimeout = ((systemClock.currentTimeMillis() - pooledConnection.getLastActiveTime()-MAX_IDLE_TIME_IN_USING >=0));
					if ((isDead || isTimeout) && (pooledConnection.compareAndSet(state, PooledConnectionState.CLOSED))) {
						conCurSize.decrementAndGet();
						pooledConnection.closePhysicalConnection();
						badConList.add(pooledConnection);
					}
				} else if (state == PooledConnectionState.CLOSED) {
					pooledConnection.closePhysicalConnection();
					badConList.add(pooledConnection);
				}
			}
		 
			if (!badConList.isEmpty()) {
				conArray.removeAll(badConList);
				badConList.clear();
				badConList = null;
			}
		}
	}

	/**
	 * resource release on pool closing
	 */
	public void destroy() {
		if (isNormal()) {
			state = STATE_CLOSED;
			connectionIdleCheckTimer.cancel();
			
			while (existWaiter()) 
			 LockSupport.parkNanos(1000);
			
			//clear all connections
			LinkedList<PooledConnection> badConList = new LinkedList<PooledConnection>();
			while (conArray.size() > 0) {
				PooledConnection[]  connListArray = conArray.getArray();
				for (PooledConnection pooledConnection : connListArray) {
					if (pooledConnection.compareAndSet(PooledConnectionState.IDLE, PooledConnectionState.CLOSED)) {
						pooledConnection.closePhysicalConnection();
						badConList.add(pooledConnection);
					} else if (pooledConnection.getConnectionState() == PooledConnectionState.CLOSED) {
						badConList.add(pooledConnection);
					} else if (pooledConnection.getConnectionState() == PooledConnectionState.USING) {
						final boolean isDead = !isActivePooledConnection(pooledConnection);
						final boolean isTimeout = ((systemClock.currentTimeMillis()-pooledConnection.getLastActiveTime()-MAX_IDLE_TIME_IN_USING >=0));
						if ((isDead || isTimeout) && (pooledConnection.compareAndSet(PooledConnectionState.USING, PooledConnectionState.CLOSED))) {
							pooledConnection.closePhysicalConnection();
							badConList.add(pooledConnection);
						}
					}
				}//for
				
				if(badConList.size()>0){
					conArray.removeAll(badConList);
					badConList.clear();
				}
				if (conArray.size() > 0)
					LockSupport.parkNanos(1000);
			}//while
			conCurSize.set(0);
			
			try {
				Runtime.getRuntime().removeShutdownHook(connectionPoolHook);
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
			poolReference = connectionPool;
		}

		public void run() {
			poolReference.closeIdleTimeoutConnection();
		}
	}

	/**
	 * Hook when JVM exit
	 */
	 class ConnectionPoolHook extends Thread {
		private ConnectionPool pool;

		public ConnectionPoolHook(ConnectionPool connectionPool) {
			pool = connectionPool;
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
				return jdbcConnectionDriver.connect(jdbcURL,jdbcProperties);
			} else {
				return DriverManager.getConnection(jdbcURL,jdbcProperties);
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
			while (pool.existWaiter()) {
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
			while (pool.existWaiter() && pooledConnection.getConnectionState()== PooledConnectionState.IDLE) {
				if (pool.transferToWaiter(pooledConnection)) {
					return;
				} else if (++failTimes % 50 == 0) {
					LockSupport.parkNanos(1000);
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
