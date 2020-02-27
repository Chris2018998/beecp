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

import cn.beecp.BeeDataSourceConfig;
import cn.beecp.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static cn.beecp.pool.PoolExceptionList.*;
import static cn.beecp.pool.PoolObjectsState.*;
import static cn.beecp.util.BeecpUtil.isNullText;
import static cn.beecp.util.BeecpUtil.oclose;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.concurrent.locks.LockSupport.*;

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
	private long DefaultMaxWaitMills;//milliseconds
	private int ConUnCatchStateCode;
	private String ConnectionTestSQL;//select
	private int ConnectionTestTimeout;//seconds
	private long ConnectionTestInterval;//milliseconds

	private ConnectionPoolHook exitHook;
	private BeeDataSourceConfig poolConfig;

	private Semaphore semaphore;
	private TransferPolicy transferPolicy;
	private ConnectionTestPolicy testPolicy;
	private ConnectionFactory connCreateFactory;
	private final Object connArrayLock =new Object();
	private volatile PooledConnection[] connArray = new PooledConnection[0];
	private ConcurrentLinkedQueue<Borrower> waitQueue = new ConcurrentLinkedQueue<>();
	private ThreadLocal<WeakReference<Borrower>> threadLocal = new ThreadLocal<>();
	private ScheduledFuture<?> idleCheckSchFuture = null;
	private ScheduledThreadPoolExecutor idleSchExecutor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
		public Thread newThread(Runnable r) {
			Thread idleScanThread = new Thread(r);
			idleScanThread.setDaemon(true);
			idleScanThread.setName("IdleScanThread");
			return idleScanThread;
		}
	});

	private int networkTimeout;
	private boolean supportSchema=true;
	private boolean supportIsValid=true;
	private boolean supportNetworkTimeout=true;
	private boolean supportQueryTimeout=true;
	private boolean supportIsValidTested=false;
	private ThreadPoolExecutor networkTimeoutExecutor = new ThreadPoolExecutor(0,10,15,SECONDS, new SynchronousQueue<Runnable>(),new ThreadFactory() {
		public Thread newThread(Runnable r) {
			Thread timeoutThread = new Thread(r);
			timeoutThread.setDaemon(true);
			timeoutThread.setName("NetworkTimeoutExecutor");
			return timeoutThread;
		}
	});

	private String poolName;
	private volatile int poolState=POOL_UNINIT;
	private volatile int createConnThreadState=THREAD_WORKING;
	private AtomicInteger createNotifyCount = new AtomicInteger(0);
	private static Logger log = LoggerFactory.getLogger(FastConnectionPool.class);
	private static AtomicInteger PoolNameIndex = new AtomicInteger(1);
	private static final int MaxTimedSpins = (Runtime.getRuntime().availableProcessors() < 2) ? 0 : 32;
	private static final AtomicIntegerFieldUpdater<PooledConnection> ConnStateUpdater = AtomicIntegerFieldUpdater.newUpdater(PooledConnection.class, "state");
	private static final AtomicReferenceFieldUpdater<Borrower, Object> BorrowerStateUpdater = AtomicReferenceFieldUpdater.newUpdater(Borrower.class, Object.class, "stateObject");
	private static final AtomicIntegerFieldUpdater<FastConnectionPool> PoolStateUpdater = AtomicIntegerFieldUpdater.newUpdater(FastConnectionPool.class, "poolState");
	private static final AtomicIntegerFieldUpdater<FastConnectionPool> CreateConnThreadStateUpdater = AtomicIntegerFieldUpdater.newUpdater(FastConnectionPool.class, "createConnThreadState");

	private static final long MillsToNanoTimes=1_000_000L;
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
		if (poolState== POOL_UNINIT) {
			checkProxyClasses();
			if(config == null)throw new SQLException("Datasource configuration can't be null");
			poolConfig = config;

			poolName = !isNullText(config.getPoolName()) ? config.getPoolName():"FastPool-" + PoolNameIndex.getAndIncrement();
			log.info("BeeCP({})starting....",poolName);

			PoolMaxSize=poolConfig.getMaxActive();
			AutoCommit=poolConfig.isDefaultAutoCommit();
			ConnectionTestSQL = poolConfig.getConnectionTestSQL();
			ConnectionTestTimeout = poolConfig.getConnectionTestTimeout();
			this.testPolicy= new SQLQueryTestPolicy();
			if(isNullText(ConnectionTestSQL))
				ConnectionTestSQL="select 1 from dual";

			DefaultMaxWaitMills = poolConfig.getMaxWait();
			ConnectionTestInterval = poolConfig.getConnectionTestInterval();
			TestOnBorrow = poolConfig.isTestOnBorrow();
			TestOnReturn = poolConfig.isTestOnReturn();
			connCreateFactory =poolConfig.getConnectionFactory();

			String mode;
			if (poolConfig.isFairMode()) {
				mode = "fair";
				transferPolicy = new FairTransferPolicy();
				ConUnCatchStateCode = transferPolicy.getCheckStateCode();
			} else {
				mode = "compete";
				transferPolicy =new CompeteTransferPolicy();
				ConUnCatchStateCode = transferPolicy.getCheckStateCode();
			}

			exitHook = new ConnectionPoolHook();
			Runtime.getRuntime().addShutdownHook(exitHook);

			this.setDaemon(true);
			this.setName("ConnectionAdd");
			this.start();

			semaphore = new Semaphore(poolConfig.getConcurrentSize(), poolConfig.isFairMode());
			
			networkTimeoutExecutor.setMaximumPoolSize(config.getMaxActive());
			idleCheckSchFuture = idleSchExecutor.scheduleAtFixedRate(new Runnable() {
				public void run() {// check idle connection
					closeIdleTimeoutConnection();
				}
			},config.getIdleCheckTimeInitDelay(),config.getIdleCheckTimeInterval(), TimeUnit.MILLISECONDS);

			registerJMX();
			createInitConnections();
			poolState=POOL_NORMAL;
			log.info("BeeCP({})has startup{mode:{},init size:{},max size:{},concurrent size:{},max wait:{}ms,driver:{}}",
					poolName,
					mode,
					connArray.length,
					config.getMaxActive(),
					poolConfig.getConcurrentSize(),
					poolConfig.getMaxWait(),
					poolConfig.getDriverClassName());
		} else {
			throw new SQLException("Pool has initialized");
		}
	}

	/**
	 * check some proxy classes whether exists
	 */
	private void checkProxyClasses() throws SQLException {
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			Class.forName("cn.beecp.pool.ProxyConnection",false, classLoader);
			Class.forName("cn.beecp.pool.ProxyStatement", false, classLoader);
			Class.forName("cn.beecp.pool.ProxyPsStatement", false, classLoader);
			Class.forName("cn.beecp.pool.ProxyCsStatement", false, classLoader);
			Class.forName("cn.beecp.pool.ProxyDatabaseMetaData", false, classLoader);
			Class.forName("cn.beecp.pool.ProxyResultSet", false, classLoader);
		} catch (ClassNotFoundException e) {
			throw new SQLException("Jdbc proxy class missed",e);
		}
	}
	boolean isSupportSchema() {
		return supportSchema;
	}
	boolean isSupportIsValid() {
		return supportIsValid;
	}
	boolean isSupportNetworkTimeout() {
		return supportNetworkTimeout;
	}
	int getNetworkTimeout() {
		return networkTimeout;
	}
	ThreadPoolExecutor getNetworkTimeoutExecutor() {
		return networkTimeoutExecutor;
	}
	private boolean existBorrower() {
		return poolConfig.getConcurrentSize()>semaphore.availablePermits()||semaphore.hasQueuedThreads();
	}
	//create Pooled connection
	private PooledConnection createPooledConn(int connState) throws SQLException {
		synchronized (connArrayLock) {
			int oldLen = connArray.length;
			if (oldLen < PoolMaxSize) {
				Connection con=connCreateFactory.create();
				setDefaultOnRawConn(con);
				PooledConnection pConn = new PooledConnection(con,this,poolConfig,connState);// add
				PooledConnection[] arrayNew = new PooledConnection[oldLen + 1];
				System.arraycopy(connArray, 0, arrayNew, 0, oldLen);
				arrayNew[oldLen] = pConn;// tail
				connArray = arrayNew;
				log.debug("BeeCP({})created pooledConn:{}",poolName,pConn);
				return pConn;
			}else{
				return null;
			}
		}
	}
	//remove Pooled connection
	private void removePooledConn(PooledConnection pConn,String removeType) {
		synchronized (connArrayLock) {
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
			log.debug("BeeCP({})removed {}pooledConn:{}",poolName,removeType,pConn);
		}
	}
	//set default attribute on raw connection
	private void setDefaultOnRawConn(Connection rawConn){
		try{
			rawConn.setAutoCommit(poolConfig.isDefaultAutoCommit());
		}catch( SQLException e) {
			log.warn("BeeCP({})failed to set default on executing 'setAutoCommit'",poolName);
		}

		try{
			rawConn.setTransactionIsolation(poolConfig.getDefaultTransactionIsolationCode());
		}catch( SQLException e) {
			log.warn("BeeCP({}))failed to set default on executing to 'setTransactionIsolation'",poolName);
		}

		try{
			rawConn.setReadOnly(poolConfig.isDefaultReadOnly());
		}catch( SQLException e){
			log.warn("BeeCP({}))failed to set default on executing to 'setReadOnly'",poolName);
		}

		if(!isNullText(poolConfig.getDefaultCatalog())){
			try{
				rawConn.setCatalog(poolConfig.getDefaultCatalog());
			}catch( SQLException e) {
				log.warn("BeeCP({}))failed to set default on executing to 'setCatalog'",poolName);
			}
		}

		//for JDK1.7 begin
		if(supportSchema&&!isNullText(poolConfig.getDefaultSchema())){//test schema
			try{
				rawConn.setSchema(poolConfig.getDefaultSchema());
			}catch(Throwable e) {
				supportSchema=false;
				log.warn("BeeCP({})driver not support 'schema'",poolName);
			}
		}

		if(supportNetworkTimeout){//test networkTimeout
			try {//set networkTimeout
				this.networkTimeout=rawConn.getNetworkTimeout();
				if(networkTimeout<=0) {
					supportNetworkTimeout=false;
					log.warn("BeeCP({})driver not support 'networkTimeout'",poolName);
				}else{
					rawConn.setNetworkTimeout(this.getNetworkTimeoutExecutor(),networkTimeout);
				}
			}catch(Throwable e) {
				supportNetworkTimeout=false;
				log.warn("BeeCP({})driver not support 'networkTimeout'",poolName);
			}
		}

		if (!this.supportIsValidTested) {//test isValid
			try {//test Connection.isValid
				rawConn.isValid(1);
				this.testPolicy = new ConnValidTestPolicy();
			} catch (Throwable e) {
				this.supportIsValid = false;
				log.warn("BeeCP({})driver not support 'isValid'",poolName);
				Statement st=null;
				try {
					st=rawConn.createStatement();
					st.setQueryTimeout(ConnectionTestTimeout);
				}catch(Throwable ee){
					supportQueryTimeout=false;
					log.warn("BeeCP({})driver not support 'queryTimeout'",poolName);
				}finally{
					oclose(st);
				}
			} finally {
				supportIsValidTested = true;
			}
		}
		//for JDK1.7 end
	}

	/**
	 * check connection state,when
	 *
	 * @return if the checked connection is active then return true,otherwise
	 *         false if false then close it
	 */
	private boolean isActiveConn(PooledConnection pConn) {
		boolean isActive=false;
		try {
			return(isActive=testPolicy.isActive(pConn));
		} finally {
			if (!isActive) {
				pConn.state=CONNECTION_CLOSED;
				removePooledConn(pConn,DESC_REMOVE_BAD);
				tryToCreateNewConnByAsyn();
			}
		}
	}

	private boolean testOnBorrow(PooledConnection pConn) {
		return !TestOnBorrow || (currentTimeMillis() - pConn.lastAccessTime - ConnectionTestInterval <=0) || isActiveConn(pConn);
	}
	private boolean testOnReturn(PooledConnection pConn) {
		return !TestOnReturn || (currentTimeMillis() - pConn.lastAccessTime - ConnectionTestInterval <=0) || isActiveConn(pConn);
	}

	/**
	 * create initialization connections
	 *
	 * @throws SQLException
	 *             error occurred in creating connections
	 */
	private void createInitConnections() throws SQLException {
		try {
			for (int i=0,size=poolConfig.getInitialSize();i < size; i++)
				createPooledConn(CONNECTION_IDLE);
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
		if (wait <= 0)throw WaitTimeException;
		if (poolState != POOL_NORMAL)
			throw PoolCloseException;

		WeakReference<Borrower> bRef = threadLocal.get();
		Borrower borrower=(bRef !=null)?bRef.get():null;
		if (borrower != null) {
			PooledConnection pConn = borrower.initBeforeBorrow();
			if (pConn != null && ConnStateUpdater.compareAndSet(pConn, CONNECTION_IDLE, CONNECTION_USING)) {
				if(testOnBorrow(pConn))
				 return createProxyConnection(pConn, borrower);
				
				borrower.lastUsedConn = null;
			}
		} else {
			borrower = new Borrower();
			threadLocal.set(new WeakReference<>(borrower));
		}

		try {
			wait*=MillsToNanoTimes;
			long deadline = nanoTime()+wait;
			if (semaphore.tryAcquire(wait,NANOSECONDS)) {
				try {
					return takeOneConnection(deadline, borrower);
				}finally { semaphore.release();}
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
		for (PooledConnection pConn:connArray) {
			if (ConnStateUpdater.compareAndSet(pConn, CONNECTION_IDLE, CONNECTION_USING) && testOnBorrow(pConn)) {
				return createProxyConnection(pConn, borrower);
			}
		}

		//create directly
		PooledConnection pConn=null;
		if(connArray.length<PoolMaxSize && (pConn=createPooledConn(CONNECTION_USING))!=null){
			return createProxyConnection(pConn,borrower);
		}

		long waitTime;
		Object stateObject;
		boolean isTimeout=false;
		boolean isInterrupted=false;
		int spinSize = MaxTimedSpins;
		Thread borrowThread=borrower.thread;
		borrower.stateObject=BORROWER_NORMAL;
	 
		try {// wait one transferred connection
			waitQueue.offer(borrower);

			for (;;) {
				stateObject = borrower.stateObject;
				if (stateObject instanceof PooledConnection) {
					pConn = (PooledConnection) stateObject;
					if(transferPolicy.tryCatch(pConn) && testOnBorrow(pConn))
						return createProxyConnection(pConn, borrower);

					borrower.stateObject=BORROWER_NORMAL;//reset to normal
					continue;
				} else if (stateObject instanceof SQLException) {
					throw (SQLException) stateObject;
				}

				if (isInterrupted||(isInterrupted=borrowThread.isInterrupted())) {
					if (BorrowerStateUpdater.compareAndSet(borrower, stateObject, BORROWER_INTERRUPTED))
						break;
					continue;
				}

				if (isTimeout||(isTimeout=(waitTime=deadline-nanoTime())<=0)) {
					if (BorrowerStateUpdater.compareAndSet(borrower, stateObject, BORROWER_TIMEOUT))
						break;
					continue;
				}

				if (spinSize--> 0)continue;//spin
				if (BorrowerStateUpdater.compareAndSet(borrower, stateObject, BORROWER_WAITING)) {
					parkNanos(borrower, waitTime);
					BorrowerStateUpdater.compareAndSet(borrower, BORROWER_WAITING, BORROWER_NORMAL);
				}
			} // for
		} finally {
			waitQueue.remove(borrower);
		}

		if(isInterrupted)throw RequestInterruptException;
		throw RequestTimeoutException;
	}

	// create proxy to wrap connection as result
	private static ProxyConnectionBase createProxyConnection(PooledConnection pConn, Borrower borrower)
			throws SQLException {
		// borrower.setBorrowedConnection(pConn);
		// return pConn.proxyConnCurInstance=new ProxyConnection(pConn);
		throw new SQLException("Proxy classes not be generated,please execute 'ProxyClassGenerator' after compile");
	}

	// notify to create connections to pool
	private void tryToCreateNewConnByAsyn() {
		if(connArray.length+createNotifyCount.get()<PoolMaxSize)  {
			createNotifyCount.incrementAndGet();
			if(CreateConnThreadStateUpdater.compareAndSet(this, THREAD_WAITING, THREAD_WORKING))
				unpark(this);
		}
	}

	/**
	 * remove connection
	 *
	 * @param pConn
	 *            target connection need release
	 */
	 void abandonOnReturn(PooledConnection pConn) {
		removePooledConn(pConn,DESC_REMOVE_BAD);
		tryToCreateNewConnByAsyn();
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

		transferPolicy.beforeTransfer(pConn);
		for(Borrower borrower:waitQueue){
			if(pConn.state!=ConUnCatchStateCode)return;
			if(transferToWaiter(borrower,pConn))return;
		}
		transferPolicy.onFailTransfer(pConn);
	}
	/**
	 * @param exception:
	 *            transfer Exception to waiter
	 */
	private void transferException(SQLException exception) {
		for(Borrower borrower:waitQueue){
			if(transferToWaiter(borrower,exception))return;
		}
	}
	private static boolean transferToWaiter(Borrower waiter,Object val) {
		Object waiterState=waiter.stateObject;
		while(waiterState == BORROWER_NORMAL || waiterState == BORROWER_WAITING){
			if(BorrowerStateUpdater.compareAndSet(waiter,waiterState,val)) {
				if (waiterState == BORROWER_WAITING)
					unpark(waiter.thread);
				return true;
			}
			waiterState=waiter.stateObject;
		}
		return false;
	}

	/**
	 * inner timer will call the method to clear some idle timeout connections
	 * or dead connections,or long time not active connections in using state
	 */
	private void closeIdleTimeoutConnection() {
		if (poolState == POOL_NORMAL) {
			for (PooledConnection pConn : connArray) {
				int state = pConn.state;
				if (state == CONNECTION_IDLE && !existBorrower()) {
					boolean isTimeoutInIdle = ((currentTimeMillis() - pConn.lastAccessTime - poolConfig.getIdleTimeout() >= 0));
					if (isTimeoutInIdle && ConnStateUpdater.compareAndSet(pConn, state, CONNECTION_CLOSED)) {//need close idle
						removePooledConn(pConn, DESC_REMOVE_IDLE);
						tryToCreateNewConnByAsyn();
					}
				} else if (state == CONNECTION_USING) {
					boolean isHolTimeoutInNotUsing = ((currentTimeMillis() - pConn.lastAccessTime - poolConfig.getHoldIdleTimeout() >= 0));
					if (isHolTimeoutInNotUsing && ConnStateUpdater.compareAndSet(pConn, state, CONNECTION_CLOSED)) {
						removePooledConn(pConn, DESC_REMOVE_HOLDTIMEOUT);
						tryToCreateNewConnByAsyn();
					}
				} else if (state == CONNECTION_CLOSED) {
					removePooledConn(pConn, DESC_REMOVE_CLOSED);
					tryToCreateNewConnByAsyn();
				}
			}
		}
	}

	// shutdown pool
	public void shutdown() {
		long parkNanos = SECONDS.toNanos(poolConfig.getWaitTimeToClearPool());
		for (;;) {
			if (PoolStateUpdater.compareAndSet(this,POOL_NORMAL,POOL_CLOSED)) {
				log.info("BeeCP({})begin to shutdown",poolName);
				removeAllConnections(poolConfig.isForceCloseConnection(),DESC_REMOVE_DESTROY);
				while (true) {
					if (idleCheckSchFuture.cancel(true)) break;
				}

				idleSchExecutor.shutdown();
				networkTimeoutExecutor.shutdown();
				shutdownCreateConnThread();
				unregisterJMX();

				try {
					Runtime.getRuntime().removeShutdownHook(exitHook);
				} catch (Throwable e) {
					log.error("BeeCP({})failed to remove pool hook",poolName);
				}

				log.info("BeeCP({})has shutdown",poolName);
				break;
			} else if (poolState == POOL_CLOSED) {
				break;
			} else {
				parkNanos(parkNanos);// wait 3 seconds
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
				} else if (pConn.state == CONNECTION_CLOSED) {
					removePooledConn(pConn,source);
				} else if (pConn.state == CONNECTION_USING) {
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
				parkNanos(parkNanos);
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
		for (;;) {
			if (CreateConnThreadStateUpdater.compareAndSet(this,createConnThreadState, THREAD_DEAD)) {
				if(createConnThreadState == THREAD_WAITING)
					unpark(this);
				break;
			}
		}
	}
	// create connection to pool
	public void run() {
		int createdCount =0;
		PooledConnection pConn;
		while (createConnThreadState==THREAD_WORKING) {
			while(++createdCount<=createNotifyCount.get() && !waitQueue.isEmpty()) {
				try {
					if((pConn = createPooledConn(CONNECTION_USING)) != null) {
						new TransferThread(pConn).start();
					}else{//pool full
					 	break;
					}
				} catch (SQLException e) {
					new TransferThread(e).start();
				}
			}

			createdCount =0;
			createNotifyCount.set(0);
			if (CreateConnThreadStateUpdater.compareAndSet(this,THREAD_WORKING, THREAD_WAITING)) {
				park(this);
			}
		}
	}
	//new connection TransferThread
	class TransferThread extends Thread {
		private boolean isConn=true;
		private SQLException e;
		private PooledConnection pConn;
		TransferThread(SQLException e){this.e=e;isConn=false;}
		TransferThread(PooledConnection pConn){this.pConn=pConn;}
		public void run(){
			if(isConn) {
				release(pConn, false);
			}else {
				transferException(e);
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
		if (PoolStateUpdater.compareAndSet(this,POOL_NORMAL, POOL_RESTING)) {
			log.info("BeeCP({})begin to reset.",poolName);
			removeAllConnections(force,DESC_REMOVE_RESET);
			log.info("All pooledConns were cleared");
			poolState=POOL_NORMAL;// restore state;
			log.info("BeeCP({})finished reseting",poolName);
		}
	}
	public int getConnTotalSize(){
		return connArray.length;
	}
	public int getConnIdleSize(){
		int idleConnections=0;
		for (PooledConnection pConn:this.connArray) {
			if(pConn.state == CONNECTION_IDLE)
				idleConnections++;
		}
		return idleConnections;
	}
	public int getConnUsingSize(){
		int active=connArray.length - getConnIdleSize();
		return(active>0)?active:0;
	}
	public int getSemaphoreAcquiredSize(){
		return poolConfig.getConcurrentSize()-semaphore.availablePermits();
	}
	public int getSemaphoreWaitingSize(){
		return semaphore.getQueueLength();
	}
	public int getTransferWaitingSize(){
		return waitQueue.size();
	}
	// register JMX
	private void registerJMX() {
		if (poolConfig.isEnableJMX()) {
			MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
			registerJMXBean(mBeanServer,String.format("cn.beecp.pool.FastConnectionPool:type=BeeCP(%s)",poolName),this);
			registerJMXBean(mBeanServer,String.format("cn.beecp.BeeDataSourceConfig:type=BeeCP(%s)-config",poolName),poolConfig);
		}
	}
	private void registerJMXBean(MBeanServer mBeanServer,String regName,Object bean) {
		try {
			ObjectName jmxRegName = new ObjectName(regName);
			if(!mBeanServer.isRegistered(jmxRegName)) {
				mBeanServer.registerMBean(bean,jmxRegName);
			}
		} catch (Exception e) {
			log.warn("BeeCP({})failed to register jmx-bean:{}",poolName,regName,e);
		}
	}
	// unregister JMX
	private void unregisterJMX() {
		if (poolConfig.isEnableJMX()) {
			MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
			unregisterJMXBean(mBeanServer,String.format("cn.beecp.pool.FastConnectionPool:type=BeeCP(%s)",poolName));
			unregisterJMXBean(mBeanServer,String.format("cn.beecp.BeeDataSourceConfig:type=BeeCP(%s)-config",poolName));
		}
	}
	private void unregisterJMXBean(MBeanServer mBeanServer,String regName) {
		try {
			ObjectName jmxRegName = new ObjectName(regName);
			if(mBeanServer.isRegistered(jmxRegName)) {
				mBeanServer.unregisterMBean(jmxRegName);
			}
		} catch (Exception e) {
			log.warn("BeeCP({})failed to unregister jmx-bean:{}",poolName,regName,e);
		}
	}
	//******************************** JMX **************************************/

	// Connection check Policy
	interface ConnectionTestPolicy {
		boolean isActive(PooledConnection pConn);
	}
	// SQL check Policy
	class SQLQueryTestPolicy implements ConnectionTestPolicy {
		public boolean isActive(PooledConnection pConn){
			boolean autoCommitChged=false;
			Statement st = null;
			Connection con = pConn.rawConn;
			try {
				//may be a store procedure or a function in this test sql,so need rollback finally
				//for example: select xxx() from dual
				if(AutoCommit){
					con.setAutoCommit(false);
					autoCommitChged=true;
				}

				st = con.createStatement();
				pConn.updateAccessTime();
				if(supportQueryTimeout){
					try {
						st.setQueryTimeout(ConnectionTestTimeout);
					}catch(Throwable e){
						log.error("BeeCP({})failed to setQueryTimeout",poolName,e);
					}
				}

				st.execute(ConnectionTestSQL);
				return true;
			} catch (SQLException e) {
				log.error("BeeCP({})failed to test connection",poolName,e);
				return false;
			} finally {
				try {
					con.rollback();
					if(AutoCommit&&autoCommitChged)
						con.setAutoCommit(true);
				} catch (SQLException e){
					log.error("BeeCP({})failed to execute 'rollback or setAutoCommit(true)' after connection test",poolName,e);
				}
				oclose(st);
			}
		}
	}
	//check Policy(call connection.isValid)
	class ConnValidTestPolicy implements ConnectionTestPolicy {
		public boolean isActive(PooledConnection pConn) {
			Connection con = pConn.rawConn;
			try {
			      if(con.isValid(ConnectionTestTimeout)){
				  pConn.updateAccessTime();
				  return true;
			       }
			} catch (SQLException e) {
				log.error("BeeCP({})failed to test connection",poolName,e);
			}
			return false;
		}
	}

	// Transfer Policy
	interface TransferPolicy {
		int getCheckStateCode();
		boolean tryCatch(PooledConnection pConn);
		void onFailTransfer(PooledConnection pConn);
		void beforeTransfer(PooledConnection pConn);
	}
	class CompeteTransferPolicy implements TransferPolicy {
		public int getCheckStateCode() {return CONNECTION_IDLE;}
		public boolean tryCatch(PooledConnection pConn) {
			return ConnStateUpdater.compareAndSet(pConn, CONNECTION_IDLE, CONNECTION_USING); }
		public void onFailTransfer(PooledConnection pConn) { }
		public void beforeTransfer(PooledConnection pConn) {
			pConn.state=CONNECTION_IDLE;
		}
	}
	class FairTransferPolicy implements TransferPolicy {
		public int getCheckStateCode() {return CONNECTION_USING; }
		public boolean tryCatch(PooledConnection pConn) {
			return pConn.state == CONNECTION_USING;
		}
		public void onFailTransfer(PooledConnection pConn){pConn.state=CONNECTION_IDLE; }
		public void beforeTransfer(PooledConnection pConn) { }
	}
}
