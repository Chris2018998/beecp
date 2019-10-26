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

import static cn.bee.dbcp.pool.PoolObjectsState.BORROWER_INTERRUPTED;
import static cn.bee.dbcp.pool.PoolObjectsState.BORROWER_NORMAL;
import static cn.bee.dbcp.pool.PoolObjectsState.BORROWER_TIMEOUT;
import static cn.bee.dbcp.pool.PoolObjectsState.BORROWER_WAITING;
import static cn.bee.dbcp.pool.PoolObjectsState.CONNECTION_CHECKING;
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

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import cn.bee.dbcp.BeeDataSourceConfig;
import cn.bee.dbcp.ConnectionFactory;
import cn.bee.dbcp.pool.util.ConnectionUtil;

/**
 * JDBC Connection Pool base Implementation
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class ConnectionPool{
	private Timer idleCheckTimer;
	private ConnectionPoolHook exitHook;
	protected final BeeDataSourceConfig poolConfig;
	
	private final String validationQuery;
	private final boolean validateSQLIsNull;
	private final int validationQueryTimeout;
	
	private final int PoolMaxSize;
	private final boolean TestOnBorrow;
	private final boolean TestOnReturn;
	private final long DefaultMaxWaitMills;
	private final long ValidationIntervalMills;
	private final boolean StatementCacheInd;
	private final int ConUnCatchStateCode;
	
	protected final Semaphore poolSemaphore;
	private final TransferPolicy tansferPolicy;
	private volatile boolean surpportQryTimeout=true;
	private final CreateConnThread createThread=new CreateConnThread();
	private final AtomicInteger createThreadState=createThread.state;
	private final PooledConnectionList connList=new PooledConnectionList();
	protected final ConcurrentLinkedQueue<Borrower> waitQueue = new ConcurrentLinkedQueue<Borrower>();
	private final ThreadLocal<WeakReference<Borrower>> threadLocal = new ThreadLocal<WeakReference<Borrower>>();
	
	private String poolName="";
	private static String poolNamePrefix="Pool-";
	private static AtomicInteger poolNameIndex=new AtomicInteger(1);
	private final AtomicInteger poolState=new AtomicInteger(POOL_UNINIT);
	protected final static InterruptedException InterruptException = new InterruptedException();
	protected static final SQLException RequestTimeoutException = new SQLException("Request timeout");
	protected static final SQLException RequestInterruptException = new SQLException("Request interrupt");
	protected static final SQLException PoolCloseException = new SQLException("Pool has been closed or in resting");
	private final static int maxTimedSpins = (Runtime.getRuntime().availableProcessors()<2)?0:32;
	private final static AtomicIntegerFieldUpdater<PooledConnection>ConnStateUpdater=AtomicIntegerFieldUpdater.newUpdater(PooledConnection.class,"state");
	private final static AtomicReferenceFieldUpdater<Borrower,Object>TansferStateUpdater=AtomicReferenceFieldUpdater.newUpdater(Borrower.class,Object.class,"stateObject");

	/**
	 * initialize pool with configuration
	 * 
	 * @param config data source configuration
	 * @throws SQLException check configuration fail or to create initiated connection 
	 */
	public ConnectionPool(BeeDataSourceConfig config) throws SQLException {
		if (poolState.get() == POOL_UNINIT) {
			checkProxyClasses();
			if(config == null)throw new SQLException("Datasource configeruation can't be null");
			poolConfig=config;
			poolConfig.check();
			poolConfig.setInited(true);
			
			poolName=!ConnectionUtil.isNull(config.getPoolName())?config.getPoolName():poolNamePrefix+poolNameIndex.getAndIncrement();
			System.out.println("BeeCP("+poolName+")starting....");
			
			PoolMaxSize=poolConfig.getMaxActive();
			validationQuery=poolConfig.getValidationQuery();
			validateSQLIsNull=isNull(validationQuery);
			validationQueryTimeout=poolConfig.getValidationQueryTimeout();
			idleCheckTimer = new Timer(true);
			idleCheckTimer.schedule(new PooledConnectionIdleTask(), 60000L, 180000L);
			exitHook = new ConnectionPoolHook();
			Runtime.getRuntime().addShutdownHook(exitHook);
			StatementCacheInd=poolConfig.getPreparedStatementCacheSize()>0;
			DefaultMaxWaitMills=poolConfig.getMaxWait();
			ValidationIntervalMills=poolConfig.getValidationInterval();
			TestOnBorrow=poolConfig.isTestOnBorrow();
			TestOnReturn=poolConfig.isTestOnReturn();		
			
			String mode = "";
			if (poolConfig.isFairQueue()) {
				mode = "fair";
				tansferPolicy = new FairTransferPolicy();
				ConUnCatchStateCode=tansferPolicy.getCheckStateCode();
			} else {
				mode = "compete";
				tansferPolicy = new CompeteTransferPolicy();
				ConUnCatchStateCode=tansferPolicy.getCheckStateCode();
			}
			
			createInitConns();
			poolSemaphore=new Semaphore(poolConfig.getConcurrentSize(),poolConfig.isFairQueue());
			System.out.println("BeeCP("+poolName+")has been startup{init size:"+connList.size()+",max size:"+config.getMaxActive()+",concurrent size:"+poolConfig.getConcurrentSize()+ ",mode:"+mode +",max wait:"+poolConfig.getMaxWait()+"ms}");
			poolState.set(POOL_NORMAL); 
			createThread.start();
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
			Class.forName("cn.bee.dbcp.pool.ProxyConnection",true,classLoader);
			Class.forName("cn.bee.dbcp.pool.ProxyStatement", true,classLoader);
			Class.forName("cn.bee.dbcp.pool.ProxyPsStatement",true,classLoader);
			Class.forName("cn.bee.dbcp.pool.ProxyCsStatement",true,classLoader);
			Class.forName("cn.bee.dbcp.pool.ProxyDatabaseMetaData",true,classLoader);
			Class.forName("cn.bee.dbcp.pool.ProxyResultSet",true,classLoader);
		} catch (ClassNotFoundException e) {
			throw new SQLException("some pool jdbc proxy classes are missed");
		}
	}
	
	private final boolean isNormal() {
		return poolState.get() == POOL_NORMAL;
	}
	boolean isStatementCacheInd() {
		return StatementCacheInd;
	}
	protected boolean existBorrower() {
		return(poolConfig.getConcurrentSize()-poolSemaphore.availablePermits()+poolSemaphore.getQueueLength())>0;	
	}
	private int getIdleConnSize(){
		int conIdleSize=0;
		for (PooledConnection pConn:connList.getArray()) {
			if(ConnStateUpdater.get(pConn) == CONNECTION_IDLE)
				conIdleSize++;
		}
		return conIdleSize;
	}
	public Map<String,Integer> getPoolSnapshot(){
		Map<String,Integer> snapshotMap = new LinkedHashMap<String,Integer>();
		snapshotMap.put("PoolMaxSize", poolConfig.getMaxActive());
		snapshotMap.put("concurrentSize", poolConfig.getConcurrentSize());
		snapshotMap.put("ConCurSize",connList.size());
		snapshotMap.put("ConIdleSize",getIdleConnSize());
		return snapshotMap;
	}
	 
	/**
	 * check connection state,when
	 * @return if the checked connection is active then return true,otherwise false 
	 * if false then close it   
	 */
	private final boolean isActiveConn(PooledConnection pConn) {
		boolean isActive = true;
		try {
			if (validateSQLIsNull) {
				try {
					isActive = pConn.getPhisicConnection().isValid(validationQueryTimeout);
					pConn.updateAccessTime();
				} catch (SQLException e) {
					isActive = false;
				}
			} else {
				Statement st=null;
				try {
					st=pConn.getPhisicConnection().createStatement();
					pConn.updateAccessTime();
					setsetQueryTimeout(st);
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
				removePooledConnection(pConn);
			}
		}
	}
	private final void setsetQueryTimeout(Statement st) {
		if(surpportQryTimeout){
			try {
				st.setQueryTimeout(validationQueryTimeout);
			} catch (SQLException e) {
				surpportQryTimeout=false;
			}
		}
	}
	private boolean testOnBorrow(PooledConnection pConn) {
		return TestOnBorrow&&(currentTimeMillis()-pConn.getLastAccessTime()-ValidationIntervalMills>=0)?isActiveConn(pConn):true;	
	}
	private boolean testOnReturn(PooledConnection pConn) {
		return TestOnReturn&&(currentTimeMillis()-pConn.getLastAccessTime()-ValidationIntervalMills>=0)?isActiveConn(pConn):true;
	}
	private void removePooledConnection(PooledConnection pConn){
		pConn.closePhysicalConnection();
		connList.remove(pConn);
	}
	
	/**
	 * create initialization connections
	 * 
	 * @throws SQLException
	 *             error occurred in creating connections
	 */
	protected void createInitConns() throws SQLException {
		Connection con=null;
		int size = poolConfig.getInitialSize();
		ConnectionFactory connFactory= poolConfig.getConnectionFactory();
		
		try {
			for (int i = 0; i < size; i++){ 
				if((con=connFactory.create())!=null){
					connList.add(new PooledConnection(con,this));
				}
			}
		} catch (SQLException e) {
			for(PooledConnection pConn:connList.getArray()) 
				removePooledConnection(pConn);
			throw e;
		}
	}
	
	/**
	 * borrow a connection from pool
	 * @return If exists idle connection in pool,then return one;if not, waiting until other borrower release
	 * @throws SQLException if pool is closed or waiting timeout,then throw exception
	 */
	public final Connection getConnection() throws SQLException {
		return getConnection(DefaultMaxWaitMills);
	}

	/**
	 * borrow one connection from pool
	 * 
	 * @param wait must be greater than zero
	 *             
	 * @return If exists idle connection in pool,then return one;if not, waiting
	 *         until other borrower release
	 * @throws SQLException
	 *             if pool is closed or waiting timeout,then throw exception
	 */
	public Connection getConnection(long wait) throws SQLException {
		if(poolState.get()!= POOL_NORMAL)throw PoolCloseException;
		if(wait<=0)throw new SQLException("wait time must be greater than zero");
		
		PooledConnection pConn = null;
		WeakReference<Borrower> bRef=threadLocal.get();
		Borrower borrower=(bRef!=null)?bRef.get():null;
		
		try {
			if (borrower == null) {
				borrower = new Borrower(this);
				threadLocal.set(new WeakReference<Borrower>(borrower));
			} else {
				borrower.resetInBorrowing();
				if ((pConn = borrower.getLastUsedConn()) != null) {
					if(ConnStateUpdater.compareAndSet(pConn,CONNECTION_IDLE,CONNECTION_USING)) {	
						if(testOnBorrow(pConn))
							return createProxyConnection(pConn, borrower);
						else
							borrower.setLastUsedConn(null);
					}
				}
			}
			
			return getConnection(wait,borrower);
		} catch (Throwable e) {
			if (borrower != null && borrower.isHasHoldNewOne()) {//has borrowed one
				this.release(borrower.getLastUsedConn(),false);
			}
			
			if(e instanceof SQLException){
			   throw (SQLException)e;
			}else if(e instanceof InterruptedException){
				throw RequestInterruptException;
			}else{
			    throw new SQLException("Failed to take connection",e); 
			}  
		}  
	}
	
	//Concurrent control
	protected Connection getConnection(long wait,Borrower borrower)throws SQLException,InterruptedException {
		wait=MILLISECONDS.toNanos(wait);
		long deadlineNanos=nanoTime()+wait;

		if(poolSemaphore.tryAcquire(wait,NANOSECONDS)){
			try{
				Connection con=takeOneConnection(deadlineNanos,borrower);
				if(con!=null)return con;
				
				if(Thread.currentThread().isInterrupted())
				throw RequestInterruptException;
				throw RequestTimeoutException;
			}finally {
			  poolSemaphore.release();
			}
		}else{
			throw RequestTimeoutException;
		}
	}
	
	//take one PooledConnection
    final Connection takeOneConnection(long deadlineNanos,Borrower borrower)throws SQLException,InterruptedException{
    	for(PooledConnection pConn:connList.getArray()) {
    		  if(ConnStateUpdater.compareAndSet(pConn,CONNECTION_IDLE,CONNECTION_USING)&& testOnBorrow(pConn)) {
    			  return createProxyConnection(pConn,borrower);
			}
		}
    	
		long waitNanos=0;
		boolean isTimeout=false;
		Object stateObject=null;
		PooledConnection pConn=null;
		int spinSize = maxTimedSpins;
    	Thread thread=borrower.resetThread();
		TansferStateUpdater.set(borrower,BORROWER_NORMAL);
		
		try {// wait one transfered connection
			waitQueue.offer(borrower);
			tryToCreateNewConnByAsyn();
			
			for(;;){
				stateObject=TansferStateUpdater.get(borrower);
				if(stateObject instanceof PooledConnection) {
					pConn = (PooledConnection) stateObject;
					// fix issue:#3 Chris-2019-08-01 begin
					// if(tansferPolicy.tryCatch(pConn){
					if(tansferPolicy.tryCatch(pConn) && testOnBorrow(pConn)){
					// fix issue:#3 Chris-2019-08-01 end
						return createProxyConnection(pConn,borrower);
					}else{
						TansferStateUpdater.set(borrower,BORROWER_NORMAL);
						continue;
					}
				}else if(stateObject instanceof SQLException) {
					throw (SQLException) stateObject;
				}
				
				if(thread.isInterrupted()) {
					if(TansferStateUpdater.compareAndSet(borrower,stateObject,BORROWER_INTERRUPTED))
					 return null;
					continue;
				}
				
				if(isTimeout) {
					if(TansferStateUpdater.compareAndSet(borrower,stateObject,BORROWER_TIMEOUT))
					return null;
					continue;
				}
				
				if((waitNanos=deadlineNanos-nanoTime())<=0){
					isTimeout=true;
					if(TansferStateUpdater.compareAndSet(borrower,stateObject,BORROWER_TIMEOUT))
					return null;
					continue;
				}

				if(spinSize-->0)continue;//spin
				if(TansferStateUpdater.compareAndSet(borrower,stateObject,BORROWER_WAITING)) {
					LockSupport.parkNanos(borrower,waitNanos);
					if(TansferStateUpdater.get(borrower)==BORROWER_WAITING) {
						TansferStateUpdater.compareAndSet(borrower,BORROWER_WAITING,BORROWER_NORMAL);
					}
				}
			}// for
		} finally {
			waitQueue.remove(borrower);
		}
    }
    //create proxy to wrap connection as result
  	private ProxyConnectionBase createProxyConnection(PooledConnection pConn,Borrower borrower)throws SQLException {
  		borrower.setLastUsedConn(pConn);
  		ProxyConnectionBase proxyConn=ProxyConnectionFactory.createProxyConnection(pConn);
  		pConn.bindProxyConnection(proxyConn);
  		return proxyConn;
  	}
  	
 	//notify to create connections to pool 
	private void tryToCreateNewConnByAsyn() {
		if(createThreadState.get()==THREAD_WAITING &&connList.size()<PoolMaxSize&&createThreadState.compareAndSet(THREAD_WAITING,THREAD_WORKING)){
			LockSupport.unpark(createThread);
		}
	}

	/**
	 * return connection to pool
	 * @param pConn target connection need release
	 * @param needTest, true check active
	 */
	protected final void release(PooledConnection pConn,boolean needTest) {
		if(needTest && !testOnReturn(pConn))return;
		tansferPolicy.beforeTransfer(pConn);

		Object state = null;
		for(Borrower borrower:waitQueue) {
			if(ConnStateUpdater.get(pConn) != ConUnCatchStateCode)
				return;

			state=TansferStateUpdater.get(borrower);
			while(state==BORROWER_NORMAL || state==BORROWER_WAITING){
				if(TansferStateUpdater.compareAndSet(borrower, state, pConn)){
					if(state==BORROWER_WAITING)LockSupport.unpark(borrower.thread);
					return;
				}
				state = TansferStateUpdater.get(borrower);
			}
		}
		tansferPolicy.onFailTransfer(pConn);
	}
	
	/**
	 * @param exception: transfered Exception to waiter
	 */
	private final void transferException(SQLException exception){
		Object state=null;
		for(Borrower borrower:waitQueue){
			state=TansferStateUpdater.get(borrower);
			while(state==BORROWER_NORMAL ||state ==BORROWER_WAITING){
				if(TansferStateUpdater.compareAndSet(borrower, state, exception)){
					if(state==BORROWER_WAITING)LockSupport.unpark(borrower.thread);
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
		if (isNormal()){
			for (PooledConnection pConn:connList.getArray()) {
				final int state = ConnStateUpdater.get(pConn);
				if (state == CONNECTION_IDLE && !existBorrower()) {
					final boolean isTimeoutInIdle = ((currentTimeMillis()-pConn.getLastAccessTime()-poolConfig.getIdleTimeout()>=0));
					if(isTimeoutInIdle && ConnStateUpdater.compareAndSet(pConn,state,CONNECTION_CHECKING)){	
						 if(isActiveConn(pConn)){//active connection	
							ConnStateUpdater.set(pConn,CONNECTION_USING);
							release(pConn,false); 
						 } 
					}
				} else if (state == CONNECTION_USING) {
					final boolean isTimeoutInNotUsing = ((currentTimeMillis()-pConn.getLastAccessTime()-poolConfig.getMaxHoldTimeInUnused()>=0));
					if(isTimeoutInNotUsing){
						if(isActiveConn(pConn)){//return to pool
						  release(pConn,false);
						}else if(!waitQueue.isEmpty())//bad connection;
						  tryToCreateNewConnByAsyn();//<--why?    
					}
				} else if (state==CONNECTION_CLOSED) {
					removePooledConnection(pConn);
				} else if (state==CONNECTION_CHECKING) {
					//do nothing
				}
			}
			
			if(!waitQueue.isEmpty() && connList.getArray().length==0){
				tryToCreateNewConnByAsyn();
			}
		}
	}
	
	//close all connections
	public void reset() {
		reset(false);//wait borrower release connection,then close them 
	}
	//close all connections
	public void reset(boolean force) {
		if(poolState.compareAndSet(POOL_NORMAL,POOL_RESTING)){
			removeAllConnections(force);
			poolState.set(POOL_NORMAL);//restore state;
		}
	}
	//shutdown pool
	public void destroy() {
		System.out.println("BeeCP(" + poolName + ")begin to shutdown");
		long parkNanos=SECONDS.toNanos(poolConfig.getWaitTimeToClearPool());
		
		for(;;) {
			if(poolState.compareAndSet(POOL_NORMAL,POOL_CLOSED)) {
				removeAllConnections(poolConfig.isForceCloseConnection());
				idleCheckTimer.cancel();
				createThread.shutdown();
				
				try {
					Runtime.getRuntime().removeShutdownHook(exitHook);
				} catch (Throwable e) {}
				
				System.out.println("BeeCP(" + poolName + ")has been shutdown");
				break;
			} else if(poolState.get()==POOL_CLOSED){
				break;
			}else{
				LockSupport.parkNanos(parkNanos);//wait 3 seconds
			}
		}
	}
	
	//remove all connections
	private void removeAllConnections(boolean force){
		while(existBorrower()){
			transferException(PoolCloseException);
		}
		
		long parkNanos=SECONDS.toNanos(poolConfig.getWaitTimeToClearPool());
		while (connList.size()>0) {
			for(PooledConnection pConn : connList.getArray()) {
				if(ConnStateUpdater.compareAndSet(pConn,CONNECTION_IDLE,CONNECTION_CLOSED)) {	
					removePooledConnection(pConn);
				 }else if (ConnStateUpdater.compareAndSet(pConn,CONNECTION_CHECKING,CONNECTION_CLOSED)) {	
					removePooledConnection(pConn);
				 }else if (ConnStateUpdater.get(pConn)==CONNECTION_CLOSED) {	
					removePooledConnection(pConn);
				 }else if (ConnStateUpdater.get(pConn)==CONNECTION_USING) {
					if(force){
						if(ConnStateUpdater.compareAndSet(pConn,CONNECTION_USING,CONNECTION_CLOSED)) {
							removePooledConnection(pConn);
						}
					}else{
 						final boolean isTimeout=((currentTimeMillis()-pConn.getLastAccessTime()-poolConfig.getMaxHoldTimeInUnused()>=0));					
						if(isTimeout && ConnStateUpdater.compareAndSet(pConn,CONNECTION_USING,CONNECTION_CLOSED)){	
							removePooledConnection(pConn);
						}
					}
				}
			}//for
		
			if(connList.size()>0)LockSupport.parkNanos(parkNanos);
		}//while
	}
	
	/**
	 * a inner task to scan idle timeout connections or dead
	 */
	private class PooledConnectionIdleTask extends TimerTask {
		public void run() {
			closeIdleTimeoutConnection();
		}
	}

	/**
	 * Hook when JVM exit
	 */
   private class ConnectionPoolHook extends Thread {
		public void run() {
			ConnectionPool.this.destroy();
		}
   }
   
    class CreateConnThread extends Thread {
	   final AtomicInteger state=new AtomicInteger(THREAD_WORKING);
	   CreateConnThread(){
		   this.setDaemon(true);
	   }

		public void shutdown() {
			int stateCode;
			for(;;){
				stateCode=state.get();
				if(state.compareAndSet(stateCode,THREAD_DEAD)){
					if(stateCode==THREAD_WAITING)LockSupport.unpark(this);
					break;
				}
			}
		}
	   public void run() {
		    Connection con=null;
			PooledConnection pConn=null;
			final ConnectionPool pool=ConnectionPool.this;
			ConnectionFactory connFactory= poolConfig.getConnectionFactory();
			
			while(state.get()==THREAD_WORKING){
				if(PoolMaxSize>connList.size() && !waitQueue.isEmpty()){
					try {
						if((con=connFactory.create())!=null){
							pConn=new PooledConnection(con,pool);
							pConn.state=CONNECTION_USING;
							connList.add(pConn);
							release(pConn,false);
						}
					} catch (SQLException e) {
						if(con!=null)ConnectionUtil.oclose(con);
						transferException(e);
					}finally{
						con=null;
					}
				}else if(state.compareAndSet(THREAD_WORKING,THREAD_WAITING)){
					LockSupport.park(this);
				}
			}
		}
   }
   //Transfer Policy
   static abstract class TransferPolicy {
		protected abstract int getCheckStateCode();
		protected abstract boolean tryCatch(PooledConnection pConn);
		protected abstract void onFailTransfer(PooledConnection pConn);
		protected abstract void beforeTransfer(PooledConnection pConn);
  	}
   static final class CompeteTransferPolicy extends TransferPolicy {
  		protected int getCheckStateCode(){
  			return CONNECTION_IDLE;
  		}
		protected boolean tryCatch(PooledConnection pConn){
			return ConnStateUpdater.compareAndSet(pConn,CONNECTION_IDLE,CONNECTION_USING);
		}
		protected void onFailTransfer(PooledConnection pConn){
		}
		protected void beforeTransfer(PooledConnection pConn){
			ConnStateUpdater.set(pConn,CONNECTION_IDLE);
		}
  	}
   static final class FairTransferPolicy extends TransferPolicy {
  		protected int getCheckStateCode(){
  			return CONNECTION_USING;
  		}
		protected boolean tryCatch(PooledConnection pConn){
			return ConnStateUpdater.get(pConn)==CONNECTION_USING;
		}
		protected void onFailTransfer(PooledConnection pConn){
			ConnStateUpdater.set(pConn,CONNECTION_IDLE);
		}
		protected void beforeTransfer(PooledConnection pConn){ 
		}
  	}
}