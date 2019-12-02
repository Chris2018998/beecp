/*
 * Copyright Chris2018998
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
package cn.bee.dbcp.pool;

import static cn.bee.dbcp.pool.PoolExceptionList.PoolCloseException;
import static cn.bee.dbcp.pool.PoolExceptionList.RequestInterruptException;
import static cn.bee.dbcp.pool.PoolExceptionList.RequestTimeoutException;
import static cn.bee.dbcp.pool.PoolExceptionList.WaitTimeException;
import static cn.bee.dbcp.pool.PoolObjectsState.BORROWER_INTERRUPTED;
import static cn.bee.dbcp.pool.PoolObjectsState.BORROWER_NORMAL;
import static cn.bee.dbcp.pool.PoolObjectsState.BORROWER_TIMEOUT;
import static cn.bee.dbcp.pool.PoolObjectsState.BORROWER_WAITING;
import static cn.bee.dbcp.pool.PoolObjectsState.CONNECTION_CLOSED;
import static cn.bee.dbcp.pool.PoolObjectsState.CONNECTION_IDLE;
import static cn.bee.dbcp.pool.PoolObjectsState.CONNECTION_USING;
import static cn.bee.dbcp.pool.PoolObjectsState.POOL_CLOSED;
import static cn.bee.dbcp.pool.PoolObjectsState.POOL_NORMAL;
import static cn.bee.dbcp.pool.PoolObjectsState.POOL_RESTING;
import static cn.bee.dbcp.pool.PoolObjectsState.POOL_UNINIT;
import static cn.bee.dbcp.pool.PoolObjectsState.THREAD_DEAD;
import static cn.bee.dbcp.pool.PoolObjectsState.THREAD_WAITING;
import static cn.bee.dbcp.pool.PoolObjectsState.THREAD_WORKING;
import static cn.bee.dbcp.pool.util.ConnectionUtil.isNull;
import static cn.bee.dbcp.pool.util.ConnectionUtil.oclose;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.bee.dbcp.BeeDataSourceConfig;
import cn.bee.dbcp.BeeDataSourceConfigJMXBean;
import cn.bee.dbcp.ConnectionFactory;
import cn.bee.dbcp.pool.util.ConnectionUtil;

/**
 * JDBC Connection Pool Implementation
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class FastConnectionPool extends Thread implements ConnectionPool, ConnectionPoolJMXBean {
	private ConnectionPoolHook exitHook;
	private BeeDataSourceConfig poolConfig;

	private String validationQuery;
	private boolean validateSQLIsNull;
	private int validationQueryTimeout;

	private int PoolMaxSize;
	private boolean TestOnBorrow;
	private boolean TestOnReturn;
	private long DefaultMaxWaitMills;
	private long ValidationIntervalMills;
	private int ConUnCatchStateCode;

	private Semaphore poolSemaphore;
	private TransferPolicy tansferPolicy;
	private ConnectionFactory connFactory;
	private Object connArraySyn=new Object();
	private volatile PooledConnection[] connArray = new PooledConnection[0];
	private AtomicInteger createConnThreadState = new AtomicInteger(THREAD_WORKING);
	private ConcurrentLinkedQueue<Borrower> waitTransferQueue = new ConcurrentLinkedQueue<Borrower>();
	private ThreadLocal<WeakReference<Borrower>> threadLocal = new ThreadLocal<WeakReference<Borrower>>();
	private ScheduledFuture<?> idleCheckSchFuture = null;
	private ScheduledThreadPoolExecutor idleSchExecutor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
		public Thread newThread(Runnable r) {
			Thread idleScanThread = new Thread(r);
			idleScanThread.setDaemon(true);
			idleScanThread.setName("IdleScanThread");
			return idleScanThread;
		}
	});

	private String poolName = "";
	private static String poolNamePrefix = "FastPool-";
	private static AtomicInteger poolNameIndex = new AtomicInteger(1);
	private AtomicInteger poolState = new AtomicInteger(POOL_UNINIT);
	private static final int maxTimedSpins = (Runtime.getRuntime().availableProcessors() < 2) ? 0 : 32;
	private static final AtomicIntegerFieldUpdater<PooledConnection> ConnStateUpdater = AtomicIntegerFieldUpdater.newUpdater(PooledConnection.class, "state");
	private static final AtomicReferenceFieldUpdater<Borrower, Object> TansferStateUpdater = AtomicReferenceFieldUpdater.newUpdater(Borrower.class, Object.class, "stateObject");
	private Logger log = LoggerFactory.getLogger(this.getClass());

	private static final String DESC_REMOVE_INIT="init";
	private static final String DESC_REMOVE_BAD="bad";
	private static final String DESC_REMOVE_IDLE="idle";
	private static final String DESC_REMOVE_HOLDTIMEOUT="hold timeout";
	private static final String DESC_REMOVE_CLOSED="closed";
	private static final String DESC_REMOVE_RESET="reset";
	private static final String DESC_REMOVE_DESTROY="destroy";
	
	/**
	 * initialize pool with configuration
	 * 
	 * @param config
	 *            data source configuration
	 * @throws SQLException
	 *             check configuration fail or to create initiated connection
	 */
	public void init(BeeDataSourceConfig config) throws SQLException {
		if (poolState.get() == POOL_UNINIT) {
			checkProxyClasses();
			if(config == null)throw new SQLException("Datasource configeruation can't be null");
			poolConfig = config;

			poolName = !ConnectionUtil.isNull(config.getPoolName()) ? config.getPoolName():poolNamePrefix + poolNameIndex.getAndIncrement();
			log.info("BeeCP(" + poolName + ")starting....");

			PoolMaxSize = poolConfig.getMaxActive();
			validationQuery = poolConfig.getConnectionTestSQL();
			validateSQLIsNull = isNull(validationQuery);
			validationQueryTimeout = poolConfig.getConnectionTestTimeout();

			exitHook = new ConnectionPoolHook();
			Runtime.getRuntime().addShutdownHook(exitHook);
			DefaultMaxWaitMills = poolConfig.getMaxWait();
			ValidationIntervalMills = poolConfig.getConnectionTestInterval();
			TestOnBorrow = poolConfig.isTestOnBorrow();
			TestOnReturn = poolConfig.isTestOnReturn();
			connFactory=poolConfig.getConnectionFactory();
			 
			String mode = "";
			if (poolConfig.isFairMode()) {
				mode = "fair";
				tansferPolicy = new FairTransferPolicy();
				ConUnCatchStateCode = tansferPolicy.getCheckStateCode();
			} else {
				mode = "compete";
				tansferPolicy = new CompeteTransferPolicy();
				ConUnCatchStateCode = tansferPolicy.getCheckStateCode();
			}
			
			createInitConns();
			this.setDaemon(true);
			this.setName("ConnectionAdd");
			this.start();

			poolSemaphore = new Semaphore(poolConfig.getConcurrentSize(), poolConfig.isFairMode());
			log.info("BeeCP(" + poolName + ")has been startup{init size:" + connArray.length + ",max size:"
					+ config.getMaxActive() + ",concurrent size:" + poolConfig.getConcurrentSize() + ",mode:" + mode
					+ ",max wait:" + poolConfig.getMaxWait() + "ms}");
			
			poolState.set(POOL_NORMAL);
			
			idleCheckSchFuture = idleSchExecutor.scheduleAtFixedRate(new Runnable() {
				public void run() {// check idle connection
					closeIdleTimeoutConnection();
				}
			},config.getIdleCheckTimeInitDelay(),config.getIdleCheckTimeInterval(), TimeUnit.MILLISECONDS);

			registerJMX();
		} else {
			throw new SQLException("Pool has been initialized");
		}
	}

	/**
	 * check some proxy classes whether exists
	 */
	private void checkProxyClasses() throws SQLException {
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			Class.forName("cn.bee.dbcp.pool.ProxyConnection", true, classLoader);
			Class.forName("cn.bee.dbcp.pool.ProxyStatement", true, classLoader);
			Class.forName("cn.bee.dbcp.pool.ProxyPsStatement", true, classLoader);
			Class.forName("cn.bee.dbcp.pool.ProxyCsStatement", true, classLoader);
			Class.forName("cn.bee.dbcp.pool.ProxyDatabaseMetaData", true, classLoader);
			Class.forName("cn.bee.dbcp.pool.ProxyResultSet", true, classLoader);
		} catch (ClassNotFoundException e) {
			throw new SQLException("Some pool jdbc proxy classes are missed");
		}
	}

	private PooledConnection createPooledConn(int connState) throws SQLException {
		synchronized(connArraySyn) {
			int oldLen = connArray.length;
			if (oldLen < PoolMaxSize) {
				Connection con = null;
				PooledConnection pConn = null;
				try {
					con = connFactory.create();
					pConn = new PooledConnection(con, this, poolConfig, connState);// add
				} catch (SQLException e) {
					oclose(con);
					throw e;
				}

				PooledConnection[] arrayNew = new PooledConnection[oldLen + 1];
				System.arraycopy(connArray, 0, arrayNew, 0, oldLen);
				arrayNew[oldLen] = pConn;// tail
				connArray = arrayNew;
				log.debug("Created pooledConn:" + pConn);
				return pConn;
			}else{
				return null;
			}
		} 
	}
	private void removePooledConn(PooledConnection pConn,String removeType) {
		synchronized(connArraySyn) {
			pConn.closePhysicalConnection();
			int oldLen = connArray.length;
			PooledConnection[] arrayNew = new PooledConnection[oldLen - 1];
			for (int i = 0; i < oldLen; i++) {
				if (connArray[i] == pConn) {
					System.arraycopy(connArray, i + 1, arrayNew, i, oldLen - i - 1);
					break;
				} else {
					arrayNew[i] = connArray[i];
				}
			}
			
			connArray = arrayNew;
			log.debug("Removed "+removeType+" pooledConn:" + pConn);
		} 
	}
	
	private boolean existBorrower() {
		return poolConfig.getConcurrentSize()>poolSemaphore.availablePermits()||poolSemaphore.hasQueuedThreads();
	}

	/**
	 * check connection state,when
	 * 
	 * @return if the checked connection is active then return true,otherwise
	 *         false if false then close it
	 */
	private boolean isActiveConn(PooledConnection pConn) {
		boolean isActive = true;
		try {
			if (validateSQLIsNull) {
				try {
					isActive = pConn.connection.isValid(validationQueryTimeout);
					pConn.updateAccessTime();
				} catch (SQLException e) {
					isActive = false;
				}
			} else {
				Statement st = null;
				try {
					st = pConn.connection.createStatement();
					pConn.updateAccessTime();
					st.setQueryTimeout(validationQueryTimeout);
					st.execute(validationQuery);
				} catch (SQLException e) {
					isActive = false;
				} finally {
					oclose(st);
				}
			}

			return isActive;
		} finally {
			if (!isActive) {
				ConnStateUpdater.set(pConn,CONNECTION_CLOSED);
				removePooledConn(pConn,DESC_REMOVE_BAD);
			}
		}
	}

	private boolean testOnBorrow(PooledConnection pConn) {
		return TestOnBorrow && (currentTimeMillis() - pConn.lastAccessTime - ValidationIntervalMills >= 0)
				? isActiveConn(pConn) : true;
	}

	private boolean testOnReturn(PooledConnection pConn) {
		return TestOnReturn && (currentTimeMillis() - pConn.lastAccessTime - ValidationIntervalMills >= 0)
				? isActiveConn(pConn) : true;
	}

	/**
	 * create initialization connections
	 * 
	 * @throws SQLException
	 *             error occurred in creating connections
	 */
	private void createInitConns() throws SQLException {
		int size = poolConfig.getInitialSize();
		try {
			for (int i = 0; i < size; i++) 
				 this.createPooledConn(CONNECTION_IDLE);
		} catch (SQLException e) {
			for (PooledConnection pConn : connArray)
				removePooledConn(pConn,DESC_REMOVE_INIT);
			throw e;
		}
	}

	/**
	 * borrow a connection from pool
	 * 
	 * @return If exists idle connection in pool,then return one;if not, waiting
	 *         until other borrower release
	 * @throws SQLException
	 *             if pool is closed or waiting timeout,then throw exception
	 */
	public Connection getConnection() throws SQLException {
		return getConnection(DefaultMaxWaitMills);
	}

	/**
	 * borrow one connection from pool
	 * 
	 * @param wait
	 *            must be greater than zero
	 * 
	 * @return If exists idle connection in pool,then return one;if not, waiting
	 *         until other borrower release
	 * @throws SQLException
	 *             if pool is closed or waiting timeout,then throw exception
	 */
	public Connection getConnection(long wait) throws SQLException {
		if (wait <= 0)
			throw WaitTimeException;
		if (poolState.get() != POOL_NORMAL)
			throw PoolCloseException;

		WeakReference<Borrower> bRef = threadLocal.get();
		Borrower borrower = (bRef != null) ? bRef.get() : null;

		try {
			if (borrower == null) {
				borrower = new Borrower();
				threadLocal.set(new WeakReference<Borrower>(borrower));
			} else {
				borrower.hasHoldNewOne = false;
				PooledConnection pConn = borrower.lastUsedConn;
				if (pConn != null && ConnStateUpdater.compareAndSet(pConn, CONNECTION_IDLE, CONNECTION_USING)) {
					if (testOnBorrow(pConn))
						return createProxyConnection(pConn, borrower);
					else
						borrower.lastUsedConn = null;
				}
			}

			wait = MILLISECONDS.toNanos(wait);
			long deadline = nanoTime() + wait;
			if (poolSemaphore.tryAcquire(wait, NANOSECONDS)) {
				try {
					Connection con = takeOneConnection(deadline, borrower);
					if (con != null)
						return con;

					if (borrower.thread.isInterrupted())
					    throw RequestInterruptException;
					throw RequestTimeoutException;
				} finally {
					poolSemaphore.release();
				}
			} else {
				throw RequestTimeoutException;
			}
		} catch (Throwable e) {
			if (borrower != null && borrower.hasHoldNewOne) {// has borrowed one
				this.release(borrower.lastUsedConn, false);
			}

			if (e instanceof SQLException) {
				throw (SQLException) e;
			} else if (e instanceof InterruptedException) {
				throw RequestInterruptException;
			} else {
				throw new SQLException("Failed to take connection", e);
			}
		}
	}

	// take one PooledConnection
	private Connection takeOneConnection(long deadline, Borrower borrower) throws SQLException, InterruptedException {
		final PooledConnection[] connArray=this.connArray;
		for (PooledConnection pConn : connArray) {
			if (ConnStateUpdater.compareAndSet(pConn, CONNECTION_IDLE, CONNECTION_USING) && testOnBorrow(pConn)) {
				return createProxyConnection(pConn, borrower);
			}
		}
		
		//create directly
 		PooledConnection pConn = null;
		if(connArray.length<PoolMaxSize && (pConn=this.createPooledConn(CONNECTION_USING))!=null){
			return createProxyConnection(pConn,borrower);
		}
		
		long waitTime = 0;
		boolean isTimeout = false;
		Object stateObject = null;
		int spinSize = maxTimedSpins;
		TansferStateUpdater.set(borrower, BORROWER_NORMAL);
		
		try {// wait one transfered connection
			waitTransferQueue.offer(borrower);
			//this.tryToCreateNewConnByAsyn();
			
			for (;;) {
				stateObject = TansferStateUpdater.get(borrower);
				if (stateObject instanceof PooledConnection) {
					pConn = (PooledConnection) stateObject;
					// fix issue:#3 Chris-2019-08-01 begin
					// if(tansferPolicy.tryCatch(pConn){
					if (tansferPolicy.tryCatch(pConn) && testOnBorrow(pConn)) {
						// fix issue:#3 Chris-2019-08-01 end
						return createProxyConnection(pConn, borrower);
					} else {
						TansferStateUpdater.set(borrower, BORROWER_NORMAL);
						continue;
					}
				} else if (stateObject instanceof SQLException) {
					throw (SQLException) stateObject;
				}

				if (borrower.thread.isInterrupted()) {
					if (TansferStateUpdater.compareAndSet(borrower, stateObject, BORROWER_INTERRUPTED))
						break;
					continue;
				}

				if (isTimeout) {
					if (TansferStateUpdater.compareAndSet(borrower, stateObject, BORROWER_TIMEOUT))
						break;
					continue;
				}

				if ((waitTime = deadline - nanoTime()) <= 0) {
					isTimeout = true;
					if (TansferStateUpdater.compareAndSet(borrower, stateObject, BORROWER_TIMEOUT))
						break;
					continue;
				}

				if (spinSize-- > 0)continue;// spin
				if (TansferStateUpdater.compareAndSet(borrower, stateObject, BORROWER_WAITING)) {
					LockSupport.parkNanos(borrower, waitTime);
					TansferStateUpdater.compareAndSet(borrower, BORROWER_WAITING, BORROWER_NORMAL);
				}
			} // for
		} finally {
			waitTransferQueue.remove(borrower);
		}
		return null;
	}

	// create proxy to wrap connection as result
	private static ProxyConnectionBase createProxyConnection(PooledConnection pConn, Borrower borrower)
			throws SQLException {
		// borrower.setBorrowedConnection(pConn);
		// return pConn.proxyConnCurInstance=new ProxyConnection(pConn);
		throw new SQLException("Proxy classes not be generated,please execute 'ProxyClassUtil' after project compile");
	}

	// notify to create connections to pool
	private void tryToCreateNewConnByAsyn() {
		if (createConnThreadState.get() == THREAD_WAITING && connArray.length < PoolMaxSize
				&& createConnThreadState.compareAndSet(THREAD_WAITING, THREAD_WORKING)) {
			LockSupport.unpark(this);
		}
	}

	/**
	 * return connection to pool
	 * 
	 * @param pConn
	 *            target connection need release
	 * @param needTest,
	 *            true check active
	 */
	public void release(PooledConnection pConn, boolean needTest) {
		if (needTest && !testOnReturn(pConn))return;
		tansferPolicy.beforeTransfer(pConn);

		Object state = null;
		Borrower borrower=null;
		Iterator<Borrower>itor=waitTransferQueue.iterator();
		while(itor.hasNext()) {
			if (ConnStateUpdater.get(pConn) != ConUnCatchStateCode)
				return;
			
			borrower=itor.next();
			state = TansferStateUpdater.get(borrower);
			while (state == BORROWER_NORMAL || state == BORROWER_WAITING) {
				if (TansferStateUpdater.compareAndSet(borrower, state, pConn)) {
					if (state == BORROWER_WAITING)
						LockSupport.unpark(borrower.thread);
					return;
				}
				state = TansferStateUpdater.get(borrower);
			}
		}

		tansferPolicy.onFailTransfer(pConn);
	}

	/**
	 * @param exception:
	 *            transfered Exception to waiter
	 */
	private void transferException(SQLException exception) {
		Object state = null;
		Borrower borrower=null;
		Iterator<Borrower>itor=waitTransferQueue.iterator();
		while(itor.hasNext()) {
			borrower=itor.next();
			state = TansferStateUpdater.get(borrower);
			while (state == BORROWER_NORMAL || state == BORROWER_WAITING) {
				if (TansferStateUpdater.compareAndSet(borrower, state, exception)) {
					if (state == BORROWER_WAITING)
						LockSupport.unpark(borrower.thread);
					return;
				}
				state = TansferStateUpdater.get(borrower);
			}
		}
	}

	/**
	 * inner timer will call the method to clear some idle timeout connections
	 * or dead connections,or long time not active connections in using state
	 */
	private void closeIdleTimeoutConnection() {
		if (poolState.get() == POOL_NORMAL) {
			for (PooledConnection pConn : connArray) {
				final int state = ConnStateUpdater.get(pConn);
				if (state == CONNECTION_IDLE && !existBorrower()) {
					final boolean isTimeoutInIdle =((currentTimeMillis()-pConn.lastAccessTime-poolConfig.getIdleTimeout()>=0));
					if(isTimeoutInIdle && ConnStateUpdater.compareAndSet(pConn,state,CONNECTION_CLOSED)) {//need close idle
						removePooledConn(pConn,DESC_REMOVE_IDLE);
					}
				} else if (state == CONNECTION_USING) {
					final boolean isHolTimeoutInNotUsing=((currentTimeMillis()-pConn.lastAccessTime-poolConfig.getHoldIdleTimeout()>=0));
					if(isHolTimeoutInNotUsing && ConnStateUpdater.compareAndSet(pConn,state,CONNECTION_CLOSED)) {
						removePooledConn(pConn,DESC_REMOVE_HOLDTIMEOUT);
					}
				} else if (state == CONNECTION_CLOSED) {
					removePooledConn(pConn,DESC_REMOVE_CLOSED);
				} 
			}
			
			tryToCreateNewConnByAsyn();
		}
	}

	// shutdown pool
	public void destroy() {
		long parkNanos = SECONDS.toNanos(poolConfig.getWaitTimeToClearPool());
		for (;;) {
			if (poolState.compareAndSet(POOL_NORMAL,POOL_CLOSED)) {
				log.info("BeeCP(" + poolName + ")begin to shutdown");
				removeAllConnections(poolConfig.isForceCloseConnection(),DESC_REMOVE_DESTROY);
				while (!idleCheckSchFuture.cancel(true));
					
				idleSchExecutor.shutdown();
				shutdownCreateConnThread();
				unregisterJMX();

				try {
					Runtime.getRuntime().removeShutdownHook(exitHook);
				} catch (Throwable e) {
				}

				log.info("BeeCP(" + poolName + ")has been shutdown");
				break;
			} else if (poolState.get() == POOL_CLOSED) {
				break;
			} else {
				LockSupport.parkNanos(parkNanos);// wait 3 seconds
			}
		}
	}

	// remove all connections
	private void removeAllConnections(boolean force,String source) {
		while (existBorrower()) {
			transferException(PoolCloseException);
		}

		long parkNanos = SECONDS.toNanos(poolConfig.getWaitTimeToClearPool());
		while (connArray.length > 0) {
			for (PooledConnection pConn : connArray) {
				if (ConnStateUpdater.compareAndSet(pConn, CONNECTION_IDLE, CONNECTION_CLOSED)) {
					removePooledConn(pConn,source);
				} else if (ConnStateUpdater.get(pConn) == CONNECTION_CLOSED) {
					removePooledConn(pConn,source);
				} else if (ConnStateUpdater.get(pConn) == CONNECTION_USING) {
					if (force) {
						if (ConnStateUpdater.compareAndSet(pConn, CONNECTION_USING, CONNECTION_CLOSED)) {
							removePooledConn(pConn,source);
						}
					} else {
						final boolean isTimeout = ((currentTimeMillis()-pConn.lastAccessTime-poolConfig.getHoldIdleTimeout()>= 0));
						if (isTimeout && ConnStateUpdater.compareAndSet(pConn, CONNECTION_USING, CONNECTION_CLOSED)) {
							removePooledConn(pConn,source);
						}
					}
				}
			} // for

			if (connArray.length > 0)
				LockSupport.parkNanos(parkNanos);
		} // while
	}

	/**
	 * Hook when JVM exit
	 */
	private class ConnectionPoolHook extends Thread {
		public void run() {
			FastConnectionPool.this.destroy();
		}
	}

	// exit connection creation thread
	private void shutdownCreateConnThread() {
		int stateCode;
		for (;;) {
			stateCode = createConnThreadState.get();
			if (createConnThreadState.compareAndSet(stateCode, THREAD_DEAD)) {
				if (stateCode == THREAD_WAITING)
					LockSupport.unpark(this);
				break;
			}
		}
	}

	// create connection to pool
	public void run() {
		PooledConnection pConn=null;
		while (createConnThreadState.get()==THREAD_WORKING) {
			if (!waitTransferQueue.isEmpty() && connArray.length<PoolMaxSize) {
				try {
				 if((pConn=this.createPooledConn(CONNECTION_USING))!=null)
					release(pConn,false);
				} catch (SQLException e) {
					transferException(e);
				}finally{
					pConn=null;
				}
			} else if (createConnThreadState.compareAndSet(THREAD_WORKING, THREAD_WAITING)) {
				LockSupport.park(this);
			}
		}
	}

	/******************************** JMX **************************************/
	// close all connections
	public void reset() {
		reset(false);// wait borrower release connection,then close them
	}
	// close all connections
	public void reset(boolean force) {
		if (poolState.compareAndSet(POOL_NORMAL, POOL_RESTING)) {
			log.info("Pool begin to reset.");
			removeAllConnections(force,DESC_REMOVE_RESET);
			log.info("All pooledConns were cleared");
			poolState.set(POOL_NORMAL);// restore state;
			log.info("Pool finished reseting");
		}
	}
	public int getConnTotalSize(){
		return connArray.length;
	}
	public int getConnIdleSize(){
		int idleConnections=0;
		final PooledConnection[] connArray = this.connArray;
		for (PooledConnection pConn:connArray) {
			if(ConnStateUpdater.get(pConn) == CONNECTION_IDLE)
				idleConnections++;
		}
		return idleConnections;
	}
	public int getConnUsingSize(){
		int active=connArray.length - getConnIdleSize();
		return(active>0)?active:0;
	}
	public int getSemaphoreAcquiredSize(){
		return poolConfig.getConcurrentSize()-poolSemaphore.availablePermits();
	}
	public int getSemaphoreWatingSize(){
		return poolSemaphore.getQueueLength();
	}
	public int getTransferWatingSize(){
		return waitTransferQueue.size();
	}
	// register JMX
	private void registerJMX() {
		if (poolConfig.isEnableJMX()) {
			try {
				final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
				final ObjectName poolRegName = new ObjectName("cn.bee.dbcp.pool.FastConnectionPool:type=BeeCP("+poolName+")");
				final ObjectName configRegName = new ObjectName("cn.bee.dbcp.BeeDataSourceConfig:type=BeeCP("+poolName+")-config");
				
				if (!mBeanServer.isRegistered(poolRegName)) {
					mBeanServer.registerMBean((ConnectionPoolJMXBean)this,poolRegName);
					log.info("Registered BeeCP(" + poolName + ")as jmx-bean");
				} else {
					log.error("Jmx-name BeeCP(" + poolName + ")has been exist in jmx server");
				}
				
				if (!mBeanServer.isRegistered(configRegName)) {
					mBeanServer.registerMBean((BeeDataSourceConfigJMXBean)poolConfig,configRegName);
					log.info("Registered BeeCP(" + poolName + ")config as jmx-bean");
				} else {
					log.error("Pool BeeCP(" + poolName + ")config has been exist in jmx server");
				}
			} catch (Exception e) {
				log.warn("Failed to register pool jmx-bean", e);
			}
		}
	}

	// unregister JMX
	private void unregisterJMX() {
		if (poolConfig.isEnableJMX()) {
			try {
				final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
				final ObjectName poolRegName = new ObjectName("cn.bee.dbcp.pool.FastConnectionPool:type=BeeCP("+poolName+")");
				final ObjectName configRegName = new ObjectName("cn.bee.dbcp.BeeDataSourceConfig:type=BeeCP("+poolName+")-config");
				
				if(mBeanServer.isRegistered(poolRegName)) {
					 mBeanServer.unregisterMBean(poolRegName);
				}
				
				if(mBeanServer.isRegistered(configRegName)) {
				  mBeanServer.unregisterMBean(configRegName);
				}
			} catch (Exception e) {
				log.warn("Failed to unregister pool jmx-bean", e);
			}
		}
	}
	/******************************** JMX **************************************/

	// Transfer Policy
	interface TransferPolicy {
		int getCheckStateCode();

		boolean tryCatch(PooledConnection pConn);

		void onFailTransfer(PooledConnection pConn);

		void beforeTransfer(PooledConnection pConn);
	}

	class CompeteTransferPolicy implements TransferPolicy {
		public int getCheckStateCode() {
			return CONNECTION_IDLE;
		}

		public boolean tryCatch(PooledConnection pConn) {
			return ConnStateUpdater.compareAndSet(pConn, CONNECTION_IDLE, CONNECTION_USING);
		}

		public void onFailTransfer(PooledConnection pConn) {
		}

		public void beforeTransfer(PooledConnection pConn) {
			ConnStateUpdater.set(pConn, CONNECTION_IDLE);
		}
	}

	class FairTransferPolicy implements TransferPolicy {
		public int getCheckStateCode() {
			return CONNECTION_USING;
		}

		public boolean tryCatch(PooledConnection pConn) {
			return ConnStateUpdater.get(pConn) == CONNECTION_USING;
		}

		public void onFailTransfer(PooledConnection pConn) {
			ConnStateUpdater.set(pConn, CONNECTION_IDLE);
		}

		public void beforeTransfer(PooledConnection pConn) {
		}
	}
}
