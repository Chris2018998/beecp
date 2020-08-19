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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static cn.beecp.pool.PoolExceptionList.*;
import static cn.beecp.pool.PoolObjectsState.*;
import static cn.beecp.util.BeecpUtil.isNullText;
import static cn.beecp.util.BeecpUtil.oclose;
import static java.lang.System.*;
import static java.util.concurrent.TimeUnit.*;
import static java.util.concurrent.locks.LockSupport.*;

/**
 * JDBC Connection Pool Implementation
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class FastConnectionPool extends Thread implements ConnectionPool, ConnectionPoolJMXBean{
	private int PoolMaxSize;
	private long DefaultMaxWaitNanos;//nanoseconds
	private int ConUnCatchStateCode;
	private String ConnectionTestSQL;//select
	private int ConnectionTestTimeout;//seconds
	private long ConnectionTestInterval;//milliseconds
	private ConnectionPoolHook exitHook;
	private BeeDataSourceConfig poolConfig;

	private Semaphore semaphore;
	private TransferPolicy transferPolicy;
	private ConnectionTestPolicy testPolicy;
	private ConnectionFactory connFactory;
	private final Object connArrayLock =new Object();
	private final Object connNotifyLock =new Object();
	private volatile PooledConnection[] connArray = new PooledConnection[0];
	private final ConcurrentLinkedQueue<Borrower> waitQueue = new ConcurrentLinkedQueue<Borrower>();
	private final ThreadLocal<WeakReference<Borrower>> threadLocal = new ThreadLocal<WeakReference<Borrower>>();
	private ScheduledFuture<?> idleCheckSchFuture = null;
	private ScheduledThreadPoolExecutor idleSchExecutor = new ScheduledThreadPoolExecutor(1,new PoolThreadThreadFactory("IdleConnectionScan"));

	private int networkTimeout;
	private boolean supportValidTest=true;
	private boolean supportSchema=true;
	private boolean supportNetworkTimeout=true;
	private boolean supportQueryTimeout=true;
	private boolean supportIsValidTested=false;
	private ThreadPoolExecutor networkTimeoutExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
			Runtime.getRuntime().availableProcessors(),15,SECONDS, new LinkedBlockingQueue<Runnable>(),new PoolThreadThreadFactory("networkTimeout"));

	static final class PoolThreadThreadFactory implements ThreadFactory {
	    private String thName;
	    public  PoolThreadThreadFactory(String thName){
	        this.thName=thName;
        }
		public Thread newThread(Runnable r){
			Thread th= new Thread(r,thName);
			th.setDaemon(true);
			return th;
		}
	}

	private String poolName="";
	private String poolMode="";
	private AtomicInteger poolState = new AtomicInteger(POOL_UNINIT);
	private AtomicInteger createConnThreadState =new AtomicInteger(THREAD_WORKING);
	private AtomicInteger needAddConnSize = new AtomicInteger(0);
	private static Logger log = LoggerFactory.getLogger(FastConnectionPool.class);
	private static AtomicInteger PoolNameIndex = new AtomicInteger(1);
	private static final long spinForTimeoutThreshold = 1000L;
   	private static final int maxTimedSpins = (Runtime.getRuntime().availableProcessors() < 2) ? 0 : 32;
	private static final AtomicIntegerFieldUpdater<PooledConnection> ConnStateUpdater = AtomicIntegerFieldUpdater.newUpdater(PooledConnection.class, "state");
	private static final AtomicReferenceFieldUpdater<Borrower, Object> BorrowerStateUpdater = AtomicReferenceFieldUpdater.newUpdater(Borrower.class, Object.class, "state");
 
	private static final String DESC_REMOVE_INIT="init";
	private static final String DESC_REMOVE_BAD="bad";
	private static final String DESC_REMOVE_IDLE="idle";
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
		if (poolState.get()== POOL_UNINIT) {
			checkProxyClasses();
			if(config == null)throw new SQLException("DataSource configuration can't be null");
			poolConfig = config;

			poolName = !isNullText(config.getPoolName()) ? config.getPoolName():"FastPool-" + PoolNameIndex.getAndIncrement();
			log.info("BeeCP({})starting....",poolName);

			PoolMaxSize=poolConfig.getMaxActive();
			connFactory=poolConfig.getConnectionFactory();
			ConnectionTestSQL=poolConfig.getConnectionTestSQL();
			ConnectionTestTimeout=poolConfig.getConnectionTestTimeout();
			this.testPolicy= new SQLQueryTestPolicy(poolConfig.isDefaultAutoCommit());
			if(isNullText(ConnectionTestSQL))
				ConnectionTestSQL="select 1 from dual";

			DefaultMaxWaitNanos=MILLISECONDS.toNanos(poolConfig.getMaxWait());
			ConnectionTestInterval=poolConfig.getConnectionTestInterval();
			createInitConnections(poolConfig.getInitialSize());

			if (poolConfig.isFairMode()) {
				poolMode = "fair";
				transferPolicy = new FairTransferPolicy();
				ConUnCatchStateCode = transferPolicy.getCheckStateCode();
			} else {
				poolMode = "compete";
				transferPolicy =new CompeteTransferPolicy();
				ConUnCatchStateCode = transferPolicy.getCheckStateCode();
			}

			exitHook = new ConnectionPoolHook();
			Runtime.getRuntime().addShutdownHook(exitHook);
			semaphore = new Semaphore(poolConfig.getBorrowConcurrentSize(), poolConfig.isFairMode());
			networkTimeoutExecutor.allowCoreThreadTimeOut(true);
			idleCheckSchFuture = idleSchExecutor.scheduleAtFixedRate(new Runnable() {
				public void run() {// check idle connection
					closeIdleTimeoutConnection();
				}
			},config.getIdleCheckTimeInitDelay(),config.getIdleCheckTimeInterval(), TimeUnit.MILLISECONDS);

			registerJMX();
			log.info("BeeCP({})has startup{mode:{},init size:{},max size:{},concurrent size:{},max wait:{}ms,driver:{}}",
					poolName,
					poolMode,
					connArray.length,
					config.getMaxActive(),
					poolConfig.getBorrowConcurrentSize(),
					poolConfig.getMaxWait(),
					poolConfig.getDriverClassName());

			poolState.set(POOL_NORMAL);
			this.setDaemon(true);
			this.setName("PooledConnectionAdd");
			this.start();
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

	boolean isSupportValidTest() {
		return supportValidTest;
	}
	boolean isSupportSchema() {
		return supportSchema;
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
		return poolConfig.getBorrowConcurrentSize()>semaphore.availablePermits()||semaphore.hasQueuedThreads();
	}
	//create Pooled connection
	private PooledConnection createPooledConn(int connState) throws SQLException {
		synchronized (connArrayLock) {
			if (connArray.length < PoolMaxSize) {
				Connection con= connFactory.create();
				setDefaultOnRawConn(con);
				PooledConnection pConn = new PooledConnection(con,connState,this,poolConfig);// add
				PooledConnection[] arrayNew = new PooledConnection[connArray.length + 1];
				arraycopy(connArray, 0, arrayNew, 0, connArray.length);
				arrayNew[connArray.length] = pConn;// tail
				connArray = arrayNew;
				return pConn;
			}else{
				return null;
			}
		}
	}

	//remove Pooled connection
	private void removePooledConn(PooledConnection pConn,String removeType) {
		pConn.state=CONNECTION_CLOSED;
		pConn.closeRawConn();
		synchronized (connArrayLock) {
			int oldLen=connArray.length;
			PooledConnection[] arrayNew = new PooledConnection[oldLen - 1];
			for (int i = 0; i < oldLen; i++) {
				if (connArray[i] == pConn) {
					arraycopy(connArray, i + 1, arrayNew, i, oldLen- i - 1);
					break;
				} else {
					arrayNew[i] = connArray[i];
				}
			}
			connArray = arrayNew;
		}
	}
	//set default attribute on raw connection
	private void setDefaultOnRawConn(Connection rawConn){
		try{
			rawConn.setAutoCommit(poolConfig.isDefaultAutoCommit());
		}catch( Throwable e) {
			log.warn("BeeCP({})failed to set default on executing 'setAutoCommit'",poolName);
		}

		try{
			rawConn.setTransactionIsolation(poolConfig.getDefaultTransactionIsolationCode());
		}catch( SQLException e) {
			log.warn("BeeCP({}))failed to set default on executing to 'setTransactionIsolation'",poolName);
		}

		try{
			rawConn.setReadOnly(poolConfig.isDefaultReadOnly());
		}catch( Throwable e){
			log.warn("BeeCP({}))failed to set default on executing to 'setReadOnly'",poolName);
		}

		if(!isNullText(poolConfig.getDefaultCatalog())){
			try{
				rawConn.setCatalog(poolConfig.getDefaultCatalog());
			}catch( Throwable e) {
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
				if(networkTimeout<0) {
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
				if(!rawConn.isValid(ConnectionTestTimeout))
					throw new SQLException();
				this.testPolicy = new ConnValidTestPolicy();
			} catch (Throwable e) {
				supportValidTest=false;
				log.warn("BeeCP({})driver not support 'isValid'",poolName);
				Statement st=null;
				try {
					st=rawConn.createStatement();
					st.setQueryTimeout(ConnectionTestTimeout);
				}catch(Throwable ee){
					supportQueryTimeout=false;
					log.warn("BeeCP({})driver not support 'queryTimeout'",poolName);
				}finally{
					if(st!=null)oclose(st);
				}
			} finally {
				supportIsValidTested = true;
			}
		}
		//for JDK1.7 end
	}

	/**
	 * check connection state
	 *
	 * @return if the checked connection is active then return true,otherwise
	 *         false if false then close it
	 */
	private boolean testOnBorrow(PooledConnection pConn) {
		if(currentTimeMillis()-pConn.lastAccessTime-ConnectionTestInterval<0 || testPolicy.isActive(pConn)) return true;

		removePooledConn(pConn,DESC_REMOVE_BAD);
		tryToCreateNewConnByAsyn();
		return false;
	}
	/**
	 * create initialization connections
	 *
	 * @throws SQLException
	 *             error occurred in creating connections
	 */
	private void createInitConnections(int initSize) throws SQLException {
		try {
			for (int i=0;i<initSize; i++)
				createPooledConn(CONNECTION_IDLE);
		} catch (SQLException e) {
			for (PooledConnection pConn : connArray)
				removePooledConn(pConn,DESC_REMOVE_INIT);
			throw e;
		}
	}

	/**
	 * borrow one connection from pool
	 *
	 * @return If exists idle connection in pool,then return one;if not, waiting
	 *         until other borrower release
	 * @throws SQLException
	 *             if pool is closed or waiting timeout,then throw exception
	 */
	public Connection getConnection() throws SQLException {
		if (poolState.get() != POOL_NORMAL)throw PoolCloseException;

		//0:try to get from threadLocal cache
		WeakReference<Borrower> bRef = threadLocal.get();
		Borrower borrower=(bRef !=null)?bRef.get():null;
		if (borrower != null) {
			PooledConnection pConn=borrower.lastUsedConn;
			if (pConn != null && ConnStateUpdater.compareAndSet(pConn, CONNECTION_IDLE, CONNECTION_USING)) {
				if(testOnBorrow(pConn))return createProxyConnection(pConn, borrower);

				borrower.lastUsedConn = null;
			}
		} else {
			borrower = new Borrower();
			threadLocal.set(new WeakReference<Borrower>(borrower));
		}


		long deadline = nanoTime() + DefaultMaxWaitNanos;
		try {
			if (!this.semaphore.tryAcquire(this.DefaultMaxWaitNanos, TimeUnit.NANOSECONDS))
				throw RequestTimeoutException;
	 	} catch (InterruptedException e) {
			throw RequestInterruptException;
		}

		try{//semaphore acquired
			//1:try to search one from array
			PooledConnection[] connections=connArray;
			for (int i=0,l=connections.length;i<l;i++) {
				PooledConnection pConn = connections[i];
				if (ConnStateUpdater.compareAndSet(pConn, CONNECTION_IDLE, CONNECTION_USING) && testOnBorrow(pConn))
					return createProxyConnection(pConn, borrower);
			}

			//2:try to create one directly
			PooledConnection pConn;
			if (connArray.length < PoolMaxSize && (pConn = createPooledConn(CONNECTION_USING)) != null)
				return createProxyConnection(pConn, borrower);

			//3:try to get one transferred connection
			long timeout;
			boolean isFailed = false;
			SQLException failedCause = null;
			Thread bThread = borrower.thread;
			borrower.state = BORROWER_NORMAL;

			waitQueue.offer(borrower);
			int spinSize = (waitQueue.peek() == borrower) ? maxTimedSpins : 0;
			while (true) {
				Object state = borrower.state;
				if (state instanceof PooledConnection) {
					pConn = (PooledConnection) state;
					if (transferPolicy.tryCatch(pConn) && this.testOnBorrow(pConn)) {
						waitQueue.remove(borrower);
						return createProxyConnection(pConn, borrower);
					}

					borrower.state = BORROWER_NORMAL;
					yield();
				} else if (state instanceof SQLException) {
					waitQueue.remove(borrower);
					throw (SQLException) state;
				} else if (isFailed) {
					BorrowerStateUpdater.compareAndSet(borrower, state, failedCause);
				} else if ((timeout = deadline - nanoTime()) > 0L) {
					if (spinSize > 0) {
						--spinSize;
					} else if (timeout > spinForTimeoutThreshold && BorrowerStateUpdater.compareAndSet(borrower, state, BORROWER_WAITING)) {
						parkNanos(this, timeout);
						if (bThread.isInterrupted()) {
							isFailed = true;
							failedCause = RequestInterruptException;
						}
					}
				} else {//timeout
					isFailed = true;
					failedCause = RequestTimeoutException;
				}
			}//while
		} finally {
			 semaphore.release();
		}
	}

	// create proxy to wrap connection as result
	private static final Connection createProxyConnection(PooledConnection pConn, Borrower borrower)
			throws SQLException {
		// borrower.setBorrowedConnection(pConn);
		// return pConn.proxyConnCurInstance=new ProxyConnection(pConn);
		throw new SQLException("Proxy classes not be generated,please execute 'ProxyClassGenerator' after compile");
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
	 */
	public void recycle(PooledConnection pConn) {
		transferPolicy.beforeTransfer(pConn);
		Iterator<Borrower>itor= waitQueue.iterator();
	    while(itor.hasNext()){
			Borrower borrower=itor.next();
			for(Object state = borrower.state; state ==BORROWER_NORMAL || state == BORROWER_WAITING; state = borrower.state) {
				if (pConn.state != ConUnCatchStateCode) return;
				if (BorrowerStateUpdater.compareAndSet(borrower, state, pConn)) {//transfer successful
					if (state == BORROWER_WAITING) unpark(borrower.thread);
					return;
				}
			}
		}
		transferPolicy.onFailedTransfer(pConn);
	}
	/**
	 * @param exception:
	 *            transfer Exception to waiter
	 */
	private void transferException(SQLException exception) {
		Iterator<Borrower>itor= waitQueue.iterator();
		while(itor.hasNext()){
			Borrower borrower=itor.next();
			for(Object state = borrower.state; state == BORROWER_NORMAL || state == BORROWER_WAITING; state = borrower.state) {
				if (BorrowerStateUpdater.compareAndSet(borrower, state, exception)) {//transfer successful
					if (state == BORROWER_WAITING) unpark(borrower.thread);
					return;
				}
			}
		}
	}

	/**
	 * inner timer will call the method to clear some idle timeout connections
	 * or dead connections,or long time not active connections in using state
	 */
	private void closeIdleTimeoutConnection() {
		if (poolState.get() == POOL_NORMAL) {
			PooledConnection[]array=connArray;
			for (int i=0,len=array.length;i<len;i++) {
				PooledConnection pConn=array[i];
				int state = pConn.state;
				if (state == CONNECTION_IDLE && !existBorrower()) {
					boolean isTimeoutInIdle=(currentTimeMillis() - pConn.lastAccessTime - poolConfig.getIdleTimeout()>=0);
					if (isTimeoutInIdle && ConnStateUpdater.compareAndSet(pConn, state, CONNECTION_CLOSED)) {//need close idle
						removePooledConn(pConn, DESC_REMOVE_IDLE);
						tryToCreateNewConnByAsyn();
					}
				} else if (state == CONNECTION_USING) {
					ProxyConnectionBase proxyConn=pConn.proxyConn;
					boolean isHolTimeoutInNotUsing = currentTimeMillis() - pConn.lastAccessTime - poolConfig.getHoldIdleTimeout()>= 0;
					if(isHolTimeoutInNotUsing &&proxyConn!=null && proxyConn.setAsClosed()){//recycle connection
						try{
							pConn.resetRawConnOnReturn();
							this.recycle(pConn);
						}catch(Throwable e){
							this.abandonOnReturn(pConn);
						}
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
		long parkNanoSeconds = SECONDS.toNanos(poolConfig.getWaitTimeToClearPool());
		while (true) {
			if (poolState.compareAndSet(POOL_NORMAL,POOL_CLOSED)) {
				log.info("BeeCP({})begin to shutdown",poolName);
				removeAllConnections(poolConfig.isForceCloseConnection(),DESC_REMOVE_DESTROY);
				while (!idleCheckSchFuture.isCancelled() && !idleCheckSchFuture.isDone()) {
					idleCheckSchFuture.cancel(true);
				}

				idleSchExecutor.shutdownNow();
				networkTimeoutExecutor.shutdownNow();
				shutdownCreateConnThread();
				unregisterJMX();

				try {
					Runtime.getRuntime().removeShutdownHook(exitHook);
				} catch (Throwable e) {
					log.warn("BeeCP({})failed to remove pool hook",poolName);
				}

				log.info("BeeCP({})has shutdown",poolName);
				break;
			} else if (poolState.get() == POOL_CLOSED) {
				break;
			} else {
				parkNanos(parkNanoSeconds);// wait 3 seconds
			}
		}
	}

	public boolean isShutdown() {
		return poolState.get()==POOL_CLOSED;
	}

	// remove all connections
	private void removeAllConnections(boolean force,String source) {
		while (existBorrower()) {
			transferException(PoolCloseException);
		}

		long parkNanoSeconds = SECONDS.toNanos(poolConfig.getWaitTimeToClearPool());
		while (connArray.length > 0) {
			PooledConnection[]array=connArray;
			for (int i=0,len=array.length;i<len;i++) {
				PooledConnection pConn=array[i];
				if (ConnStateUpdater.compareAndSet(pConn, CONNECTION_IDLE, CONNECTION_CLOSED)) {
					removePooledConn(pConn,source);
				} else if (pConn.state == CONNECTION_CLOSED) {
					removePooledConn(pConn,source);
				} else if (pConn.state == CONNECTION_USING) {
					ProxyConnectionBase proxyConn=pConn.proxyConn;
					if (force) {
						if(proxyConn!=null &&proxyConn.setAsClosed()){
							if (ConnStateUpdater.compareAndSet(pConn, CONNECTION_USING, CONNECTION_CLOSED))
								removePooledConn(pConn,source);
						}
					} else {
						boolean isTimeout = (currentTimeMillis()-pConn.lastAccessTime-poolConfig.getHoldIdleTimeout()>= 0);
						if (isTimeout &&proxyConn!=null &&proxyConn.setAsClosed()){
							if(ConnStateUpdater.compareAndSet(pConn, CONNECTION_USING, CONNECTION_CLOSED))
								removePooledConn(pConn,source);
						}
					}
				}
			} // for

			if (connArray.length > 0)parkNanos(parkNanoSeconds);
		} // while
		idleSchExecutor.getQueue().clear();
	}

	/**
	 * Hook when JVM exit
	 */
	private class ConnectionPoolHook extends Thread {
		public void run() {
			FastConnectionPool.this.shutdown();
		}
	}
	// notify to create connections to pool
	private void tryToCreateNewConnByAsyn() {
		if(connArray.length+needAddConnSize.get()<PoolMaxSize) {
			synchronized(connNotifyLock){
				if(connArray.length+needAddConnSize.get()<PoolMaxSize)  {
					needAddConnSize.incrementAndGet();
					if(createConnThreadState.compareAndSet(THREAD_WAITING, THREAD_WORKING))
						unpark(this);
				}
			}
		}
	}
	// exit connection creation thread
	private void shutdownCreateConnThread() {
		int curSts;
		while (true) {
			curSts=createConnThreadState.get();
			if ((curSts==THREAD_WORKING||curSts==THREAD_WAITING )&&createConnThreadState.compareAndSet(curSts,THREAD_DEAD)) {
				if(curSts==THREAD_WAITING)unpark(this);
				break;
			}
		}
	}

	// create connection to pool
	public void run() {
		PooledConnection pConn;
		while(true) {
			while(needAddConnSize.get() > 0) {
				needAddConnSize.decrementAndGet();
				if (!waitQueue.isEmpty()) {
					try {
						if ((pConn = createPooledConn(CONNECTION_USING)) != null)
							new TransferThread(pConn).start();
					} catch (SQLException e) {
						new TransferThread(e).start();
					}
				}
			}

			if (needAddConnSize.get()==0 && createConnThreadState.compareAndSet(THREAD_WORKING, THREAD_WAITING))
				park(this);
			if (createConnThreadState.get() == THREAD_DEAD) break;
		}
	}
	//new connection TransferThread
	class TransferThread extends Thread {
		private boolean isConn;
		private SQLException e;
		private PooledConnection pConn;
		TransferThread(SQLException e){this.e=e;isConn=false;}
		TransferThread(PooledConnection pConn){this.pConn=pConn;isConn=true;}
		public void run(){
			if(isConn) {
				recycle(pConn);
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
		if (poolState.compareAndSet(POOL_NORMAL, POOL_RESTING)) {
			log.info("BeeCP({})begin to reset.",poolName);
			removeAllConnections(force,DESC_REMOVE_RESET);
			log.info("All pooledConns were cleared");
			poolState.set(POOL_NORMAL);// restore state;
			log.info("BeeCP({})finished reseting",poolName);
		}
	}

	public Map getPoolInfo(){
		 Map<String,Object> mapInfo=new LinkedHashMap<String,Object>(7);
		 int totSize=getConnTotalSize();
		 int idleSize=getConnIdleSize();
		 mapInfo.put("poolName",poolName);
		 mapInfo.put("poolMode",poolMode);
		 mapInfo.put("activeSize",totSize);
		 mapInfo.put("idleSize",idleSize);
		 mapInfo.put("usingSize",totSize-idleSize);
		 mapInfo.put("semaphoreWaiterSize",getSemaphoreWaitingSize());
		 mapInfo.put("transferWaiterSize",getSemaphoreWaitingSize());
		log.info("Pool info:"+mapInfo);
		 return mapInfo;
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
		return poolConfig.getBorrowConcurrentSize()-semaphore.availablePermits();
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
		private boolean AutoCommit;
		public SQLQueryTestPolicy(boolean autoCommit){
			this.AutoCommit=autoCommit;
		}
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
				pConn.lastAccessTime=currentTimeMillis();
				if(supportQueryTimeout){
					try {
						st.setQueryTimeout(ConnectionTestTimeout);
					}catch(Throwable e){
						log.error("BeeCP({})failed to setQueryTimeout",poolName,e);
					}
				}

				st.execute(ConnectionTestSQL);

				con.rollback();//why? maybe store procedure in test sql
				return true;
			} catch (Throwable e) {
				log.error("BeeCP({})failed to test connection",poolName,e);
				return false;
			} finally {
				if(st!=null)oclose(st);
				if(AutoCommit&& autoCommitChged){
					try {
						con.setAutoCommit(true);
					} catch (Throwable e){
						log.error("BeeCP({})failed to execute 'rollback or setAutoCommit(true)' after connection test",poolName,e);
					}
				}
			}
		}
	}
	//check Policy(call connection.isValid)
	class ConnValidTestPolicy implements ConnectionTestPolicy {
		public boolean isActive(PooledConnection pConn) {
			Connection con = pConn.rawConn;
			try {
				if(con.isValid(ConnectionTestTimeout)){
					pConn.lastAccessTime=currentTimeMillis();
					return true;
				}
			} catch (Throwable e) {
				log.error("BeeCP({})failed to test connection",poolName,e);
			}
			return false;
		}
	}
	// Transfer Policy
	static interface TransferPolicy {
		int getCheckStateCode();
		void beforeTransfer(PooledConnection pConn);
		boolean tryCatch(PooledConnection pConn);
		void onFailedTransfer(PooledConnection pConn);
	}
	static final class CompeteTransferPolicy implements TransferPolicy {
		public int getCheckStateCode() {return CONNECTION_IDLE;}
		public boolean tryCatch(PooledConnection pConn) {
			return ConnStateUpdater.compareAndSet(pConn, CONNECTION_IDLE, CONNECTION_USING); }
		public void onFailedTransfer(PooledConnection pConn) { }
		public void beforeTransfer(PooledConnection pConn) {
			pConn.state=CONNECTION_IDLE;
		}
	}
	static final class FairTransferPolicy implements TransferPolicy {
		public int getCheckStateCode() {return CONNECTION_USING; }
		public boolean tryCatch(PooledConnection pConn) {
			return pConn.state == CONNECTION_USING;
		}
		public void onFailedTransfer(PooledConnection pConn){pConn.state=CONNECTION_IDLE; }
		public void beforeTransfer(PooledConnection pConn) { }
	}
}
