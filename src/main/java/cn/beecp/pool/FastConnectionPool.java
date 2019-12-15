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
package cn.beecp.pool;

import static cn.beecp.pool.PoolExceptionList.PoolCloseException;
import static cn.beecp.pool.PoolExceptionList.RequestInterruptException;
import static cn.beecp.pool.PoolExceptionList.RequestTimeoutException;
import static cn.beecp.pool.PoolExceptionList.WaitTimeException;
import static cn.beecp.pool.PoolObjectsState.BORROWER_INTERRUPTED;
import static cn.beecp.pool.PoolObjectsState.BORROWER_NORMAL;
import static cn.beecp.pool.PoolObjectsState.BORROWER_TIMEOUT;
import static cn.beecp.pool.PoolObjectsState.BORROWER_WAITING;
import static cn.beecp.pool.PoolObjectsState.CONNECTION_CLOSED;
import static cn.beecp.pool.PoolObjectsState.CONNECTION_IDLE;
import static cn.beecp.pool.PoolObjectsState.CONNECTION_USING;
import static cn.beecp.pool.PoolObjectsState.POOL_CLOSED;
import static cn.beecp.pool.PoolObjectsState.POOL_NORMAL;
import static cn.beecp.pool.PoolObjectsState.POOL_RESTING;
import static cn.beecp.pool.PoolObjectsState.POOL_UNINIT;
import static cn.beecp.pool.PoolObjectsState.THREAD_DEAD;
import static cn.beecp.pool.PoolObjectsState.THREAD_WAITING;
import static cn.beecp.pool.PoolObjectsState.THREAD_WORKING;
import static cn.beecp.util.BeecpUtil.isNullText;
import static cn.beecp.util.BeecpUtil.oclose;
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

import cn.beecp.BeeDataSourceConfig;
import cn.beecp.ConnectionFactory;
/**
 * JDBC Connection Pool Implementation
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class FastConnectionPool extends Thread implements ConnectionPool, ConnectionPoolJMXBean {
	private int PoolMaxSize;
	private boolean AutoCommit;
	private boolean TestOnBorrow;
	private boolean TestOnReturn;
	private long DefaultMaxWaitMills;
	private long ValidationIntervalMills;
	private int ConUnCatchStateCode;
	private String ConnectionTestSQL;
	private int ConnectionTestTimeout;//seconds
	private boolean validateSQLIsNull;

	private ConnectionPoolHook exitHook;
	private BeeDataSourceConfig poolConfig;

	private Semaphore poolSemaphore;
	private TransferPolicy tansferPolicy;
	private ConnectionFactory connFactory;
	private final Object connArraySyn=new Object();
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

	private String poolName;
	private AtomicInteger poolState = new AtomicInteger(POOL_UNINIT);
	private Logger log = LoggerFactory.getLogger(this.getClass());

	private static AtomicInteger PoolNameIndex = new AtomicInteger(1);
	private static final int MaxTimedSpins = (Runtime.getRuntime().availableProcessors() < 2) ? 0 : 32;
	private static final AtomicIntegerFieldUpdater<PooledConnection> ConnStateUpdater = AtomicIntegerFieldUpdater.newUpdater(PooledConnection.class, "state");
	private static final AtomicReferenceFieldUpdater<Borrower, Object> TansferStateUpdater = AtomicReferenceFieldUpdater.newUpdater(Borrower.class, Object.class, "stateObject");

	private static final String DESC_REMOVE_INIT="init";
	private static final String DESC_REMOVE_BAD="bad";
	private static final String DESC_REMOVE_IDLE="idle";
	private static final String DESC_REMOVE_HOLDTIMEOUT="hold timeout";
	private static final String DESC_REMOVE_CLOSED="closed";
	private static final String DESC_REMOVE_RESET="reset";
	private static final String DESC_REMOVE_DESTROY="close";
	
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

			poolName = !isNullText(config.getPoolName()) ? config.getPoolName():"FastPool-" + PoolNameIndex.getAndIncrement();
			log.info("BeeCP({})starting....",poolName);

			PoolMaxSize=poolConfig.getMaxActive();
			AutoCommit=poolConfig.isDefaultAutoCommit();
			ConnectionTestSQL = poolConfig.getConnectionTestSQL();
			validateSQLIsNull = isNullText(ConnectionTestSQL);
			ConnectionTestTimeout = poolConfig.getConnectionTestTimeout();

			DefaultMaxWaitMills = poolConfig.getMaxWait();
			ValidationIntervalMills = poolConfig.getConnectionTestInterval();
			TestOnBorrow = poolConfig.isTestOnBorrow();
			TestOnReturn = poolConfig.isTestOnReturn();
			connFactory=poolConfig.getConnectionFactory();
			
			String mode;
			if (poolConfig.isFairMode()) {
				mode = "fair";
				tansferPolicy = new FairTransferPolicy();
				ConUnCatchStateCode = tansferPolicy.getCheckStateCode();
			} else {
				mode = "compete";
				tansferPolicy = new CompeteTransferPolicy();
				ConUnCatchStateCode = tansferPolicy.getCheckStateCode();
			}
			
			exitHook = new ConnectionPoolHook();
			Runtime.getRuntime().addShutdownHook(exitHook);
			
			this.setDaemon(true);
			this.setName("ConnectionAdd");
			this.start();

			poolSemaphore = new Semaphore(poolConfig.getConcurrentSize(), poolConfig.isFairMode());
			log.info("BeeCP({})has been startup{init size:{},max size:{},concurrent size:{},mode:{},max wait:{}ms}",
					poolName,
					connArray.length,
					config.getMaxActive(), 
					poolConfig.getConcurrentSize(), 
					mode,
					poolConfig.getMaxWait());
			
			poolState.set(POOL_NORMAL);
			
			idleCheckSchFuture = idleSchExecutor.scheduleAtFixedRate(new Runnable() {
				public void run() {// check idle connection
					closeIdleTimeoutConnection();
				}
			},config.getIdleCheckTimeInitDelay(),config.getIdleCheckTimeInterval(), TimeUnit.MILLISECONDS);

			registerJMX();
			createInitConns();
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
			Class.forName("cn.beecp.pool.ProxyConnection", true, classLoader);
			Class.forName("cn.beecp.pool.ProxyStatement", true, classLoader);
			Class.forName("cn.beecp.pool.ProxyPsStatement", true, classLoader);
			Class.forName("cn.beecp.pool.ProxyCsStatement", true, classLoader);
			Class.forName("cn.beecp.pool.ProxyDatabaseMetaData", true, classLoader);
			Class.forName("cn.beecp.pool.ProxyResultSet", true, classLoader);
		} catch (ClassNotFoundException e) {
			throw new SQLException("Jdbc proxy class missed",e);
		}
	}

	private PooledConnection createPooledConn(int connState) throws SQLException {
		synchronized(connArraySyn) {
			int oldLen = connArray.length;
			if (oldLen < PoolMaxSize) {
				Connection con=null;
				PooledConnection pConn;
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
				log.debug("Created pooledConn:{}",pConn);
				return pConn;
			}else{
				return null;
			}
		} 
	}
	private void removePooledConn(PooledConnection pConn,String removeType) {
		synchronized(connArraySyn) {
			pConn.closeRawConn();
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
			log.debug("Removed {}pooledConn:{}",removeType,pConn);
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
		Connection con= pConn.rawConn;
		try {
			if (validateSQLIsNull) {
				try {
					isActive = con.isValid(ConnectionTestTimeout);
					pConn.updateAccessTime();
				} catch (SQLException e) {
					isActive = false;
				}
			} else {
				Statement st = null;
				boolean autoCommitChged=false;
				try {
					if(AutoCommit){
					  con.setAutoCommit(false);
					  autoCommitChged=true;
					}
					
					st = con.createStatement();
					pConn.updateAccessTime();
					st.setQueryTimeout(ConnectionTestTimeout);
					st.execute(ConnectionTestSQL);
				} catch (SQLException e) {
					isActive = false;
				} finally {
					try {
						con.rollback();
					} catch (SQLException e){
						log.error("Failed to execute 'rollback' after connection test");
					}
					try {
					  if(AutoCommit&&autoCommitChged)
						 con.setAutoCommit(true);
					} catch (SQLException e){
						log.error("Failed to execute 'setAutoCommit(true)' after connection test");
					}
					oclose(st);
				}
			}

			return isActive;
		} finally {
			if (!isActive) {
				ConnStateUpdater.set(pConn,CONNECTION_CLOSED);
				removePooledConn(pConn,DESC_REMOVE_BAD);
				tryToCreateNewConnByAsyn();
			}
		}
	}

	private boolean testOnBorrow(PooledConnection pConn) {
		return !TestOnBorrow || (currentTimeMillis() - pConn.lastAccessTime - ValidationIntervalMills<0) || isActiveConn(pConn);
	}
	private boolean testOnReturn(PooledConnection pConn) {
		return !TestOnReturn || (currentTimeMillis() - pConn.lastAccessTime - ValidationIntervalMills<0) || isActiveConn(pConn);
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
	private Connection takeOneConnection(long deadline, Borrower borrower) throws SQLException {
		final PooledConnection[] connArray=this.connArray;
		for (PooledConnection pConn : connArray) {
			if (ConnStateUpdater.compareAndSet(pConn, CONNECTION_IDLE, CONNECTION_USING) && testOnBorrow(pConn)) {
				return createProxyConnection(pConn, borrower);
			}
		}
		
		//create directly
 		PooledConnection pConn;
		if(connArray.length<PoolMaxSize && (pConn=this.createPooledConn(CONNECTION_USING))!=null){
			return createProxyConnection(pConn,borrower);
		}
		
		long waitTime;
		Object stateObject;
		boolean isTimeout = false;
		int spinSize = MaxTimedSpins;
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

		for(Borrower borrower:waitTransferQueue){
			if(ConnStateUpdater.get(pConn)!=ConUnCatchStateCode)return;
			if(transferToWaiter(borrower,pConn))return;
		}
		tansferPolicy.onFailTransfer(pConn);
	}
	/**
	 * @param exception:
	 *            transfered Exception to waiter
	 */
	private void transferException(SQLException exception) {
		for(Borrower borrower:waitTransferQueue){
			if(transferToWaiter(borrower,exception))return;
		}
	}
	private boolean transferToWaiter(Borrower waiter,Object val) {
		Object state;
		for(;;){
			state = TansferStateUpdater.get(waiter);
			if(state == BORROWER_NORMAL || state == BORROWER_WAITING){
				if(TansferStateUpdater.compareAndSet(waiter,state,val)) {
					if (state == BORROWER_WAITING)
						LockSupport.unpark(waiter.thread);
					return true;
				}
			}else{
				return false;
			}
		}//inner for
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
	public void shutdown() {
		long parkNanos = SECONDS.toNanos(poolConfig.getWaitTimeToClearPool());
		for (;;) {
			if (poolState.compareAndSet(POOL_NORMAL,POOL_CLOSED)) {
				log.info("BeeCP({})begin to shutdown",poolName);
				removeAllConnections(poolConfig.isForceCloseConnection(),DESC_REMOVE_DESTROY);
				while (true) {
					if (idleCheckSchFuture.cancel(true)) break;
				}
				//idleSchExecutor.shutdown();
				shutdownCreateConnThread();
				unregisterJMX();

				try {
					Runtime.getRuntime().removeShutdownHook(exitHook);
				} catch (Throwable e) {
					log.error("Failed to remove pool hook");
				}

				log.info("BeeCP({})has been shutdown",poolName);
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
						boolean isTimeout = ((currentTimeMillis()-pConn.lastAccessTime-poolConfig.getHoldIdleTimeout()>= 0));
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
			FastConnectionPool.this.shutdown();
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
		PooledConnection pConn;
		while (createConnThreadState.get()==THREAD_WORKING) {
			if (!waitTransferQueue.isEmpty() && connArray.length<PoolMaxSize) {
				try {
				 if((pConn=this.createPooledConn(CONNECTION_USING))!=null)
					release(pConn,false);
				} catch (SQLException e) {
					transferException(e);
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
			final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
			registerJMXBean(mBeanServer,String.format("cn.beecp.pool.FastConnectionPool:type=BeeCP(%s)",poolName),this);
			registerJMXBean(mBeanServer,String.format("cn.beecp.BeeDataSourceConfig:type=BeeCP(%s)-config",poolName),poolConfig);
		}
	}
	private void registerJMXBean(MBeanServer mBeanServer,String regName,Object bean) {
		try {
			final ObjectName jmxRegName = new ObjectName(regName);
			if(!mBeanServer.isRegistered(jmxRegName)) {
				mBeanServer.registerMBean(bean,jmxRegName);
			}
		} catch (Exception e) {
			log.warn("Failed to register jmx-bean:{}",regName,e);
		}
	}
	// unregister JMX
	private void unregisterJMX() {
		if (poolConfig.isEnableJMX()) {
			final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
			unregisterJMXBean(mBeanServer,String.format("cn.beecp.pool.FastConnectionPool:type=BeeCP(%s)",poolName));
			unregisterJMXBean(mBeanServer,String.format("cn.beecp.BeeDataSourceConfig:type=BeeCP(%s)-config",poolName));
		}
	}
	private void unregisterJMXBean(MBeanServer mBeanServer,String regName) {
		try {
			final ObjectName jmxRegName = new ObjectName(regName);
			if(mBeanServer.isRegistered(jmxRegName)) {
				mBeanServer.unregisterMBean(jmxRegName);
			}
		} catch (Exception e) {
			log.warn("Failed to unregister jmx-bean:{}",regName,e);
		}
	}

	//******************************** JMX **************************************/

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
