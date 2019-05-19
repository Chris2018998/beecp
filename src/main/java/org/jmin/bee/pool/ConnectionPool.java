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
	private Timer idleCheckTimer;
	private ConnectionPoolHook exitHook;
	private boolean validateSQLIsNull;
	protected final BeeDataSourceConfig info;
	
	protected final boolean isCompete;
	private final Semaphore takeSemaphore;
	private final TransferPolicy transferPolicy;
	private final ConnectionFactory connFactory;
	private final AtomicInteger conCurSize = new AtomicInteger(0);
	protected final AtomicInteger waiterSize = new AtomicInteger(0);
	private final PooledConnectionList conList=new PooledConnectionList();
	private final SynchronousQueue<PooledConnection> transferQueue = new SynchronousQueue<PooledConnection>(true);	
	private final ThreadLocal<WeakReference<Borrower>> threadLocal = new ThreadLocal<WeakReference<Borrower>>();
	private volatile boolean surpportQryTimeout=true;
	private final long MAX_IDLE_TIME_IN_USING = 600000L;
	private final TimeUnit WaitTimeUnit=TimeUnit.MILLISECONDS;
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
			this.info = poolInfo;
			poolInfo.setInited(true);
			validateSQLIsNull = ConnectionUtil.isNull(poolInfo.getValidationQuerySQL());

			idleCheckTimer = new Timer(true);
			idleCheckTimer.schedule(new PooledConnectionIdleTask(this), 60000, 180000);
			exitHook = new ConnectionPoolHook(this);
			Runtime.getRuntime().addShutdownHook(exitHook);
			state = STATE_NORMAL;
 
			String mode = "";
			if (poolInfo.isFairMode()) {
				mode = "fair";
				transferPolicy = new FairTransferPolicy(this);
			} else {
				mode = "compete";
				transferPolicy = new CompeteTransferPolicy(this);
			}
			
			isCompete=!poolInfo.isFairMode();
			takeSemaphore=new Semaphore(poolInfo.getPoolMaxSize(),true);
			connFactory = new ConnectionFactory(poolInfo.getDriverURL(),poolInfo.getJdbcProperties(),poolInfo.getJdbcConnectionDriver());
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
			for (PooledConnection pooledConnection:conList.getArray()) {
				if (pooledConnection.getConnectionState() == PooledConnectionState.IDLE)
					conIdleSize++;
			}
		}
		
		Map<String,Integer> snapshotMap = new LinkedHashMap<String,Integer>();
		snapshotMap.put("PoolMaxSize", info.getPoolMaxSize());
		snapshotMap.put("ConCurSize",conCurSize);
		snapshotMap.put("ConIdleSize",conIdleSize);
		snapshotMap.put("WaiterSize",waiterSize);
		return snapshotMap;
	}
	
	/**
	 * check connection state,when
	 * @return if the checked connection is active then return true,otherwise false     
	 */
	private final boolean isActiveConn(PooledConnection pConn) {
		if(SystemClock.currentTimeMillis()-pConn.getLastActiveTime()-info.getMaxInactiveTimeToCheck()<=0) 
			return true;
		
		final Connection conn = pConn.getPhisicConnection();
		if (validateSQLIsNull) {
			try {
				return conn.isValid(info.getValidationQueryTimeout());
			} catch (SQLException e) {
				return false;
			}
		} else {
			Statement st = null;
			try {
				st = conn.createStatement();
				setsetQueryTimeout(st);
				st.execute(info.getValidationQuerySQL());
				return true;
			} catch (SQLException e) {
				return false;
			} finally {
				ConnectionUtil.close(st);
			}
		}
	}

	private final void setsetQueryTimeout(Statement st) {
		if(surpportQryTimeout){
			try {
				st.setQueryTimeout(info.getValidationQueryTimeout());
			} catch (SQLException e) {
				surpportQryTimeout=false;
			}
		}
	}

	/**
	 * @param pConn
	 *            need check when borrower take it
	 * @param  badConList    
	 * 			collect bad connections    
	 * @return if is valid,then return true,otherwise false;
	 */
	private final boolean checkOnBorrowed(PooledConnection pConn,List<PooledConnection> badConList) {
		if (isActiveConn(pConn)) 
			return true;
		
		conCurSize.decrementAndGet();
		pConn.setConnectionState(PooledConnectionState.CLOSED);
		pConn.closePhysicalConnection();
		badConList.add(pConn);
		return false;
	}
	
	/**
	 * create some idle connections to pool when pool initialization
	 * 
	 * @throws SQLException
	 *             error occurred in creating connections
	 */
	private void createInitConnections() throws SQLException {
		int size = info.getPoolInitSize();
		ArrayList<PooledConnection> tempList = new ArrayList<PooledConnection>(size);
		try {
			for (int i = 0; i < size; i++) {
				Connection con = connFactory.createConnection();
				tempList.add(new PooledConnection(con, info.getPreparedStatementCacheSize(), this));
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
		conList.addAll(tempList);
		conCurSize.set(tempList.size());
	}

	/**
	 * borrow a connection from pool
	 * @return If exists idle connection in pool,then return one;if not, waiting until other borrower release
	 * @throws SQLException if pool is closed or waiting timeout,then throw exception
	 */
	public Connection getConnection() throws SQLException {
		return getConnection(info.getBorrowerMaxWaitTime());
	}

	/**
	 * borrow a connection from pool
	 * 
	 * @param maxWait
	 *            max wait time for borrower
	 * @return If exists idle connection in pool,then return one;if not, waiting
	 *         until other borrower release
	 * @throws SQLException
	 *             if pool is closed or waiting timeout,then throw exception
	 */
	public Connection getConnection(final long maxWait) throws SQLException {
		checkPool();
		boolean acquired=false;
		PooledConnection pConn=null;
		PooledConnection tmpPConn=null;
		
		WeakReference<Borrower> bRef = threadLocal.get();
		Borrower borrower=(bRef!=null)?bRef.get():null;
		if (borrower == null) {
			borrower = new Borrower();
			threadLocal.set(new WeakReference<Borrower>(borrower));
		}else{
			tmpPConn = borrower.getLastUsedConnection();
		}
		List<PooledConnection> badConList = borrower.getBadConnectionList();//collect bad connection
		
		try {
			if (tmpPConn!=null && tmpPConn.compareAndSet(PooledConnectionState.IDLE,PooledConnectionState.USING)) {//step1: try to reuse one
				if (checkOnBorrowed(tmpPConn, badConList)) 
					pConn = tmpPConn;
				 else 
					borrower.setLastUsedConnection(null);
			}
			
			if (pConn == null) {
				long timeout = maxWait;
				final long deadlinePoint=SystemClock.currentTimeMillis()+timeout;
				try{acquired=takeSemaphore.tryAcquire(timeout,WaitTimeUnit);}catch(InterruptedException e){}
				
				 for(;;){
					if ((tmpPConn=searchOne())!=null && checkOnBorrowed(tmpPConn, badConList)){//step2:try to search one 
						pConn = tmpPConn;
						break;
					}
					
					if ((pConn = createOne()) != null)break;//step3:try to create one
					if ((timeout=deadlinePoint-SystemClock.currentTimeMillis())<=0)break;
					if ((tmpPConn=waitForOne(timeout,WaitTimeUnit,borrower))!= null){//step4:wait for transfered one
						if (transferPolicy.tryCatch(tmpPConn)&&checkOnBorrowed(tmpPConn,badConList)) {
							pConn = tmpPConn;
							break;
						}
					}
					if(isClosed())break;
				}//for
			}//if
		} finally {
			if(acquired)takeSemaphore.release();
			if(!badConList.isEmpty()) {
				conList.removeAll(badConList);
				badConList.clear();
			}
		}
	 
		if (pConn != null) {
			borrower.setLastUsedConnection(pConn);
			ProxyConnection proxyConn = ProxyConnectionFactory.createProxyConnection(pConn);
			pConn.bindProxyConnection(proxyConn);
			pConn.updateLastActivityTime();
			return proxyConn;
		}else if (isClosed())
			throw PoolCloseStateException;
		else
			throw ConnectionRequestTimeoutException;
	}
	private final PooledConnection searchOne() {
		for (PooledConnection pConn:conList.getArray()) {
			if (pConn.compareAndSet(PooledConnectionState.IDLE, PooledConnectionState.USING))
				return pConn;
		}
		return null;
	}
	private final PooledConnection createOne() throws SQLException {
	    final int PoolMaxSize=info.getPoolMaxSize();	
		final int StCacheSize=info.getPreparedStatementCacheSize();
		
		if(conCurSize.get()>=PoolMaxSize)return null;
		if(conCurSize.incrementAndGet() <= PoolMaxSize) {
			try {
				Connection con = connFactory.createConnection();
				PooledConnection pConn = new PooledConnection(con,StCacheSize,this);
				pConn.setConnectionState(PooledConnectionState.USING);
				conList.add(pConn);
				return pConn;
			} catch (SQLException e) {
				conCurSize.decrementAndGet();
				throw e;
			}
		} else {
			conCurSize.decrementAndGet();
			return null;
		}
	}
	protected PooledConnection waitForOne(long timeout,TimeUnit unit,Borrower borrower) {
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
	 * @param pConn need transfered connection
	 * @return if transfer successful then return true,otherwise false
	 */
	protected boolean transfer(PooledConnection pConn) { 
		return transferQueue.offer(pConn);
	}
	
	/**
	 * the method called to return a used connection to pool. if exists waiting
	 * borrowers at releasing moment,then try to transfer the connection to one
	 * of them
	 * 
	 * @param pConn
	 *            target connection need release
	 * @throws SQLException
	 *             if error occurred,then throws exception
	 */
	public void release(final PooledConnection pConn) throws SQLException {
		 transferPolicy.tryTransfer(pConn);
	}

	/**
	 * inner timer will call the method to clear some idle timeout connections
	 * or dead connections,or long time not active connections in using state
	 */
	public void closeIdleTimeoutConnection() {
		if (isNormal() && !existWaiter()) {
			LinkedList<PooledConnection> badConList = new LinkedList<PooledConnection>();
			for (PooledConnection pooledConnection:conList.getArray()) {
				final int state = pooledConnection.getConnectionState();
				if (state == PooledConnectionState.IDLE) {
					final boolean isDead = !isActiveConn(pooledConnection);
					final boolean isTimeout = ((SystemClock.currentTimeMillis() - pooledConnection.getLastActiveTime()-info.getConnectionIdleTimeout()>=0));
					if ((isDead || isTimeout) && (pooledConnection.compareAndSet(state, PooledConnectionState.CLOSED))) {
						conCurSize.decrementAndGet();
						pooledConnection.closePhysicalConnection();
						badConList.add(pooledConnection);
					}

				} else if (state == PooledConnectionState.USING) {
					final boolean isDead = !isActiveConn(pooledConnection);
					final boolean isTimeout = ((SystemClock.currentTimeMillis() - pooledConnection.getLastActiveTime()-MAX_IDLE_TIME_IN_USING >=0));
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
				conList.removeAll(badConList);
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
			idleCheckTimer.cancel();
			
			while (existWaiter()) 
			 LockSupport.parkNanos(1000);
			
			//clear all connections
			LinkedList<PooledConnection> badConList = new LinkedList<PooledConnection>();
			while (conList.size() > 0) {
				PooledConnection[]  connListArray = conList.getArray();
				for (PooledConnection pooledConnection : connListArray) {
					if (pooledConnection.compareAndSet(PooledConnectionState.IDLE, PooledConnectionState.CLOSED)) {
						pooledConnection.closePhysicalConnection();
						badConList.add(pooledConnection);
					} else if (pooledConnection.getConnectionState() == PooledConnectionState.CLOSED) {
						badConList.add(pooledConnection);
					} else if (pooledConnection.getConnectionState() == PooledConnectionState.USING) {
						final boolean isDead = !isActiveConn(pooledConnection);
						final boolean isTimeout = ((SystemClock.currentTimeMillis()-pooledConnection.getLastActiveTime()-MAX_IDLE_TIME_IN_USING >=0));
						if ((isDead || isTimeout) && (pooledConnection.compareAndSet(PooledConnectionState.USING, PooledConnectionState.CLOSED))) {
							pooledConnection.closePhysicalConnection();
							badConList.add(pooledConnection);
						}
					}
				}//for
				
				if(badConList.size()>0){
					conList.removeAll(badConList);
					badConList.clear();
				}
				if (conList.size() > 0)
					LockSupport.parkNanos(1000);
			}//while
			conCurSize.set(0);
			
			try {
				Runtime.getRuntime().removeShutdownHook(exitHook);
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
	
	 static class ConnectionFactory {
		private String url;
		private Properties prop;
		private Driver driver;
		public ConnectionFactory(String jdbcURL,Properties jdbcProperties,Driver jdbcConnectionDriver) throws SQLException {
			url=jdbcURL;
			prop=jdbcProperties;
			driver=jdbcConnectionDriver;
		}
		public Connection createConnection() throws SQLException {
			if(driver!=null)return driver.connect(url,prop);
			return DriverManager.getConnection(url,prop);
		}
	}
	
	/**
	 * Connection transfer
	 */
	static interface TransferPolicy {
		public void tryTransfer(PooledConnection pConn);
		public boolean tryCatch(PooledConnection pConn);
	}
	static class FairTransferPolicy implements TransferPolicy {
		private ConnectionPool pool;
		public FairTransferPolicy(ConnectionPool pool) {
			this.pool = pool;
		}
		public void tryTransfer(PooledConnection pConn) {
			int failTimes = 0;
			while (pool.existWaiter()) {
				if (pool.transfer(pConn)) {
					return;
				} else if (++failTimes%50==0) {
 					LockSupport.parkNanos(10);
				} else {
					Thread.yield();
				}
			}
			pConn.setConnectionState(PooledConnectionState.IDLE);
		}
		public boolean tryCatch(PooledConnection pConn) {
			return true;
		}
	}

	static class CompeteTransferPolicy implements TransferPolicy {
		private ConnectionPool pool;
		public CompeteTransferPolicy(ConnectionPool pool) {
			this.pool = pool;
		}
		public void tryTransfer(PooledConnection pConn) {
			int failTimes=0;
			pConn.setConnectionState(PooledConnectionState.IDLE);
			while (pool.existWaiter() && pConn.getConnectionState()== PooledConnectionState.IDLE) {
				if (pool.transfer(pConn)) {
					return;
				} else if (++failTimes % 50 == 0) {
					LockSupport.parkNanos(1000);
				} else {
					Thread.yield();
				}
			}
		}
		public boolean tryCatch(PooledConnection pConn) {
			return pConn.compareAndSet(PooledConnectionState.IDLE, PooledConnectionState.USING);
		}
	}
}
