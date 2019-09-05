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
package org.jmin.bee.pool;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jmin.bee.pool.PoolObjectsState.BORROWER_NORMAL;
import static org.jmin.bee.pool.PoolObjectsState.BORROWER_TIMEOUT;
import static org.jmin.bee.pool.PoolObjectsState.BORROWER_WAITING;
import static org.jmin.bee.pool.PoolObjectsState.CONNECTION_CLOSED;
import static org.jmin.bee.pool.PoolObjectsState.CONNECTION_IDLE;
import static org.jmin.bee.pool.PoolObjectsState.CONNECTION_USING;
import static org.jmin.bee.pool.PoolObjectsState.POOL_CLOSED;
import static org.jmin.bee.pool.PoolObjectsState.POOL_NORMAL;
import static org.jmin.bee.pool.PoolObjectsState.POOL_UNINIT;
import static org.jmin.bee.pool.PoolObjectsState.POOL_RESTING;
import static org.jmin.bee.pool.PoolObjectsState.THREAD_DEAD;
import static org.jmin.bee.pool.PoolObjectsState.THREAD_NORMAL;
import static org.jmin.bee.pool.PoolObjectsState.THREAD_WAITING;
import static org.jmin.bee.pool.PoolObjectsState.THREAD_WORKING;
import static org.jmin.bee.pool.util.ConnectionUtil.isNull;
import static org.jmin.bee.pool.util.ConnectionUtil.oclose;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import org.jmin.bee.BeeDataSourceConfig;
import org.jmin.bee.ConnectionFactory;
import org.jmin.bee.pool.util.ConnectionUtil;
/**
 * JDBC Connection Pool base Implementation
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class ConnectionPool{
	private volatile int state=POOL_UNINIT;
	private Timer idleCheckTimer;
	private ConnectionPoolHook exitHook;
	protected final BeeDataSourceConfig poolConfig;
	
	private final String validationQuery;
	private final boolean validateSQLIsNull;
	private final int validationQueryTimeout;
	private final boolean testOnBorrow;
	private final boolean testOnReturn;
	private final long defaultMaxWaitMills;
	private final long validationIntervalMills;
	private final boolean statementCacheInd;
	private final int transferCheckStateCode;
	private final Semaphore poolSemaphore;
	private final TransferPolicy tansferPolicy;
	
	private final CreateConnThread createThread=new CreateConnThread();
	private final PooledConnectionList connList=new PooledConnectionList();
	private final ConcurrentLinkedQueue<Borrower> waitQueue = new ConcurrentLinkedQueue<Borrower>();
	private final ThreadLocal<WeakReference<Borrower>> threadLocal = new ThreadLocal<WeakReference<Borrower>>();

	private String poolName="";
	private static String poolNamePrefix="Pool-";
	private static AtomicInteger poolNameIndex=new AtomicInteger(1);
	private volatile boolean surpportQryTimeout=true;
	protected static final SQLException RequestTimeoutException = new SQLException("Request timeout");
	protected static final SQLException RequestInterruptException = new SQLException("Request interrupt");
	protected static final SQLException PoolCloseException = new SQLException("Pool has been closed or in resting");
	private final static AtomicIntegerFieldUpdater<ConnectionPool> stateUpdater = AtomicIntegerFieldUpdater.newUpdater(ConnectionPool.class,"state");
	
	/**
	 * initialize pool with configuration
	 * 
	 * @param config data source configuration
	 * @throws SQLException check configuration fail or to create initiated connection 
	 */
	public ConnectionPool(BeeDataSourceConfig config) throws SQLException {
		if (state == POOL_UNINIT) {
			checkProxyClasses();
			if(config == null)throw new SQLException("Datasource configeruation can't be null");
			
			poolConfig=config;
			poolConfig.check();
			poolConfig.setInited(true);
			
			validationQuery=poolConfig.getValidationQuery();
			validateSQLIsNull=isNull(validationQuery);
			validationQueryTimeout=poolConfig.getValidationQueryTimeout();
			idleCheckTimer = new Timer(true);
			idleCheckTimer.schedule(new PooledConnectionIdleTask(), 60000L, 180000L);
			exitHook = new ConnectionPoolHook();
			Runtime.getRuntime().addShutdownHook(exitHook);
			statementCacheInd=poolConfig.getPreparedStatementCacheSize()>0;
			defaultMaxWaitMills=poolConfig.getMaxWait();
			validationIntervalMills=poolConfig.getValidationInterval();
			testOnBorrow=poolConfig.isTestOnBorrow();
			testOnReturn=poolConfig.isTestOnReturn();		
			
			String mode = "";
			if (poolConfig.isFairQueue()) {
				mode = "fair";
				tansferPolicy = new FairTransferPolicy();
				transferCheckStateCode=tansferPolicy.getCheckStateCode();
			} else {
				mode = "compete";
				tansferPolicy = new CompeteTransferPolicy();
				transferCheckStateCode=tansferPolicy.getCheckStateCode();
			}
			
			createInitConns();
			poolSemaphore=new Semaphore(poolConfig.getConcurrentSize(),poolConfig.isFairQueue());
			
			poolName=!ConnectionUtil.isNull(poolConfig.getPoolName())?poolConfig.getPoolName():poolNamePrefix+poolNameIndex.getAndIncrement();
			System.out.println("BeeCP("+poolName+")has been startup{init size:"+connList.size()+",max size:"+config.getMaxActive()+",concurrent size:"+poolConfig.getConcurrentSize()+ ",mode:"+mode +",max wait:"+poolConfig.getMaxWait()+"ms}");
			state = POOL_NORMAL;
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
			Class.forName("org.jmin.bee.pool.ProxyConnectionImpl", true, classLoader);
			Class.forName("org.jmin.bee.pool.ProxyStatement", true, classLoader);
			Class.forName("org.jmin.bee.pool.ProxyPsStatement", true, classLoader);
			Class.forName("org.jmin.bee.pool.ProxyCsStatement", true, classLoader);
			Class.forName("org.jmin.bee.pool.ProxyResultSet", true, classLoader);
		} catch (ClassNotFoundException e) {
			throw new SQLException("some pool jdbc proxy classes are missed");
		}
	}
	private final boolean isNormal() {
		return state == POOL_NORMAL;
	}
	boolean isStatementCacheInd() {
		return statementCacheInd;
	}
	protected boolean existBorrower() {
		return(poolSemaphore.availablePermits()<poolConfig.getConcurrentSize()||poolSemaphore.hasQueuedThreads());	
	}
	private int getIdleConnSize(){
		int conIdleSize=0;
		for (PooledConnection pConn:connList.getArray()) {
			if (pConn.getState() == CONNECTION_IDLE)
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
	 */
	private final boolean isActiveConn(PooledConnection pConn) {
		final Connection conn = pConn.getPhisicConnection();
		if (validateSQLIsNull) {
			try {
				boolean isValid=conn.isValid(validationQueryTimeout);
				pConn.updateAccessTime();
				return isValid;
			} catch (SQLException e) {
				return false;
			}
		} else {
			Statement st = null;
			try {
				st = conn.createStatement();
				setsetQueryTimeout(st);
				st.execute(validationQuery);
				pConn.updateAccessTime();
				return true;
			} catch (SQLException e) {
				return false;
			} finally {
				oclose(st);
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

	/**
	 * @param pConn
	 *            connection test whether active
	 *                   
	 * @return if is valid,then return true,otherwise false;
	 */
	private boolean testConnection(PooledConnection pConn) {
		if(currentTimeMillis()-pConn.getLastAccessTime()<=validationIntervalMills) 
			return true;
		
		if(isActiveConn(pConn))return true;
		pConn.setState(CONNECTION_CLOSED);
		pConn.closePhysicalConnection();
		connList.remove(pConn);
		return false;
	}
	private boolean testOnBorrow(PooledConnection pConn) {
		return testOnBorrow?testConnection(pConn):true;	
	}
	private boolean testOnReturn(PooledConnection pConn) {
		return testOnReturn?testConnection(pConn):true;
	}
	
	/**
	 * create initialization connections
	 * 
	 * @throws SQLException
	 *             error occurred in creating connections
	 */
	private void createInitConns() throws SQLException {
		Connection con=null;
		int size = poolConfig.getInitialSize();
		ConnectionFactory connFactory= poolConfig.getConnectionFactory();
		
		try {
			for (int i = 0; i < size; i++){ 
				if((con=connFactory.create())!=null){
					connList.add(new PooledConnection(con,this));
					setDefaultOnConnection(con);
				}
			}
		} catch (SQLException e) {
			for(PooledConnection sPConn:connList.getArray()) {
				sPConn.closePhysicalConnection();
				connList.remove(sPConn);
			}
			throw e;
		}
	}
	
	/**
	 * borrow a connection from pool
	 * @return If exists idle connection in pool,then return one;if not, waiting until other borrower release
	 * @throws SQLException if pool is closed or waiting timeout,then throw exception
	 */
	public final Connection getConnection() throws SQLException {
		return getConnection(defaultMaxWaitMills);
	}

	/**
	 * borrow one connection from pool
	 * 
	 * @param wait
	 *            max wait time for borrower
	 * @return If exists idle connection in pool,then return one;if not, waiting
	 *         until other borrower release
	 * @throws SQLException
	 *             if pool is closed or waiting timeout,then throw exception
	 */
	public final Connection getConnection(long wait) throws SQLException {
		if(stateUpdater.get(this)!= POOL_NORMAL)throw PoolCloseException;
		
		Borrower borrower = null;
		PooledConnection pConn = null;
		
		try {
			WeakReference<Borrower> bRef = threadLocal.get();
			if (bRef != null)
				borrower = bRef.get();
			if (borrower == null) {
				borrower = new Borrower(this);
				bRef = new WeakReference<Borrower>(borrower);
				threadLocal.set(bRef);
			} else {
				borrower.resetInBorrowing();
				if ((pConn = borrower.getLastUsedConn()) != null) {
					if (pConn.compareAndSetState(CONNECTION_IDLE, CONNECTION_USING)) {
						if (testOnBorrow(pConn))
							return createProxyConnection(pConn, borrower);
						else
							borrower.setLastUsedConn(null);
					}
				}
			}
			return getConnection(wait, borrower);
		} catch (InterruptedException e) {
			if (borrower != null && borrower.isHasHoldNewOne()) {
				borrower.getLastUsedConn().setState(CONNECTION_IDLE);
			}

			Thread.currentThread().interrupt();
			throw RequestInterruptException;
		}
	}
	
	//Concurrent control
	protected Connection getConnection(long wait,Borrower borrower)throws SQLException,InterruptedException {
		wait=MILLISECONDS.toNanos(wait);
		long deadlineNanos=nanoTime()+wait;
		
		if(poolSemaphore.tryAcquire(wait,NANOSECONDS)){
			try{
				return takeOneConnection(deadlineNanos,borrower);
			}finally {
				 poolSemaphore.release();
			}
		}else{
			throw RequestTimeoutException;
		}	
	}
	
	//take one connection
    final Connection takeOneConnection(long deadlineNanos,Borrower borrower)throws SQLException,InterruptedException{
    	for(PooledConnection sPConn:connList.getArray()) {
			if(sPConn.compareAndSetState(CONNECTION_IDLE,CONNECTION_USING) && testOnBorrow(sPConn)) {
				return createProxyConnection(sPConn, borrower);
			}
		}
    	
    	try{//wait one transfered connection 
    		long waitNanos=0;
			boolean timeout=false;
			Object stateObject=null;
			PooledConnection pConn=null;
			
			Thread bthread=borrower.resetThread();
			borrower.resetStateObject();// BORROWER_NORMAL	
			waitQueue.offer(borrower);
			tryToCreateNewConnByAsyn();
			 
			for(;;){
				if(bthread.isInterrupted())
					throw new InterruptedException();
				
				stateObject=borrower.getStateObject();
				if(stateObject instanceof PooledConnection){
					pConn=(PooledConnection)stateObject;
					//fix issue:#3 Chris-2019-08-01 begin
					//if(tansferPolicy.tryCatch(pConn){
					if(tansferPolicy.tryCatch(pConn)&& testOnBorrow(pConn))   
					//fix issue:#3 Chris-2019-08-01 end
				       return createProxyConnection(pConn,borrower);
				    else{
				       borrower.resetStateObject();//BORROWER_NORMAL
				       continue;
				    }
				}else if(stateObject instanceof SQLException){
					throw (SQLException)stateObject;
				}else if(timeout){
					if(borrower.compareAndSetStateObject(BORROWER_NORMAL,BORROWER_TIMEOUT))
						throw RequestTimeoutException;
				}else{
					waitNanos=deadlineNanos-nanoTime();
					if(timeout=(waitNanos<=0))continue;
					if(borrower.compareAndSetStateObject(BORROWER_NORMAL,BORROWER_WAITING))
						LockSupport.parkNanos(borrower,waitNanos);
				}
			}//for 
		}finally{
			waitQueue.remove(borrower);
		}
    }
    //create proxy to wrap connection as result
  	private ProxyConnection createProxyConnection(PooledConnection pConn,Borrower borrower)throws SQLException {
  		borrower.setLastUsedConn(pConn);
  		ProxyConnection proxyConn=ProxyConnectionFactory.createProxyConnection(pConn);
  		pConn.bindProxyConnection(proxyConn);
  		return proxyConn;
  	}
	//set default on Connection
	private final void setDefaultOnConnection(Connection connection)throws SQLException{
		connection.setAutoCommit(poolConfig.isDefaultAutoCommit());
		connection.setTransactionIsolation(poolConfig.getDefaultTransactionIsolation());
		connection.setReadOnly(poolConfig.isDefaultReadOnly());
		if(!ConnectionUtil.isNull(poolConfig.getDefaultCatalog()))
		  connection.setCatalog(poolConfig.getDefaultCatalog());
	}
 	//notify to create connections to pool 
	private void tryToCreateNewConnByAsyn() {
		if(createThread.state==THREAD_WAITING && poolConfig.getMaxActive()>connList.size()){
			createThread.state=THREAD_WORKING;
			LockSupport.unpark(createThread);
		}
	}
	
	/**
	 * return connection to pool
	 * @param pConn target connection need release
	 */
	void release(PooledConnection pConn,boolean isNew) {
		if(!isNew && !testOnReturn(pConn))return;
		
		tansferPolicy.beforeTransferReleaseConnection(pConn);
		Thread waiterThread=null;Object waiterState=null;
		for(Borrower waiter:waitQueue){
			if(pConn.getState()!=transferCheckStateCode)return;
			
			waiterThread=waiter.getThread();
			waiterState=waiter.getStateObject();
			if(!waiterThread.isInterrupted() && (waiterState==BORROWER_NORMAL||waiterState==BORROWER_WAITING) && waiter.compareAndSetStateObject(waiterState,pConn)){
				if(waiterState==BORROWER_WAITING)LockSupport.unpark(waiterThread);
				return;
			} 
		}

		tansferPolicy.onFailTransfer(pConn);
	}
	private void transferSQLException(SQLException exception){
		Thread waiterThread=null;
		Object waiterState=null;
		for(Borrower waiter:waitQueue){
		
			waiterThread=waiter.getThread();
			waiterState=waiter.getStateObject();
			if(!waiterThread.isInterrupted() && (waiterState==BORROWER_NORMAL||waiterState==BORROWER_WAITING) && waiter.compareAndSetStateObject(waiterState,exception)){
				if(waiterState==BORROWER_WAITING)LockSupport.unpark(waiterThread);
				return;
			} 
		}
	}
	
	/**
	 * inner timer will call the method to clear some idle timeout connections
	 * or dead connections,or long time not active connections in using state
	 */
	private void closeIdleTimeoutConnection() {
		if (isNormal() && !existBorrower()){
			LinkedList<PooledConnection> badConList = new LinkedList<PooledConnection>();
			for (PooledConnection pConn:connList.getArray()) {
				final int state = pConn.getState();
				if (state == CONNECTION_IDLE) {
					final boolean isDead = !isActiveConn(pConn);
					final boolean isTimeout = ((currentTimeMillis()-pConn.getLastAccessTime()>=poolConfig.getIdleTimeout()));
					if ((isDead || isTimeout) && (pConn.compareAndSetState(state,CONNECTION_CLOSED))) {
						pConn.closePhysicalConnection();
						badConList.add(pConn);
					}

				} else if (state == CONNECTION_USING) {
					final boolean isDead = !isActiveConn(pConn);
					final boolean isTimeout = ((currentTimeMillis()-pConn.getLastAccessTime()>=poolConfig.getMaxHoldTimeInUnused()));
					if ((isDead || isTimeout) && (pConn.compareAndSetState(state,CONNECTION_CLOSED))) {

						pConn.closePhysicalConnection();
						badConList.add(pConn);
					}
				} else if (state ==CONNECTION_CLOSED) {
					pConn.closePhysicalConnection();
					badConList.add(pConn);
				}
			}
		
			if (!badConList.isEmpty()) {
				connList.removeAll(badConList);
				badConList.clear();
				badConList = null;
			}
		}
	}
	
	//close all connections
	public void reset() {
		reset(false);//wait borrower release connection,then close them 
	}
	//close all connections
	public void reset(boolean force) {
		if(stateUpdater.compareAndSet(this,POOL_NORMAL,POOL_RESTING)){
			removeAllConnections(force);
			stateUpdater.set(this,POOL_NORMAL);//restore state;
		}
	}
	//shutdown pool
	public void destroy() {
		long parkNanos=SECONDS.toNanos(3);
		for(;;) {
			if(stateUpdater.compareAndSet(this,POOL_NORMAL,POOL_CLOSED)) {
				removeAllConnections(poolConfig.isForceShutdown());
				idleCheckTimer.cancel();
				createThread.shutdown();
				try {
					Runtime.getRuntime().removeShutdownHook(exitHook);
				} catch (Throwable e) {}
				System.out.println("BeeCP(" + poolName + ")has been shutdown");
				break;
			} else if(stateUpdater.get(this)==POOL_CLOSED){
				break;
			}else{
				LockSupport.parkNanos(parkNanos);//wait 3 seconds
			}
		}
	}
	//remove all connections
	private void removeAllConnections(boolean force){
		long parkNanos=SECONDS.toNanos(3);
		while (connList.size()>0) {
			for(PooledConnection pConn : connList.getArray()) {
				if (pConn.compareAndSetState(CONNECTION_IDLE,CONNECTION_CLOSED)) {
					pConn.closePhysicalConnection();
					connList.remove(pConn);
				} else if (pConn.getState()==CONNECTION_CLOSED) {
					connList.remove(pConn);
				} else if (pConn.getState()==CONNECTION_USING) {
					if(force){
						if (pConn.compareAndSetState(CONNECTION_USING,CONNECTION_CLOSED)) {
							pConn.closePhysicalConnection();
							connList.remove(pConn);
						}
					}else{
						final boolean isDead = !isActiveConn(pConn);
						final boolean isTimeout = ((currentTimeMillis()-pConn.getLastAccessTime()>=poolConfig.getMaxHoldTimeInUnused()));
						if ((isDead || isTimeout) && (pConn.compareAndSetState(CONNECTION_USING,CONNECTION_CLOSED))) {
							pConn.closePhysicalConnection();
							connList.remove(pConn);
						}
					}
				}
			}//for
			
			if (connList.size() > 0)
				LockSupport.parkNanos(parkNanos);
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
   
   private class CreateConnThread extends Thread {
	   private volatile int state=THREAD_NORMAL;   
	   CreateConnThread(){
		   this.setDaemon(true);
	   }
	   
	   public void shutdown(){
		   state=THREAD_DEAD;
		   LockSupport.unpark(this);
	   }
	   public void run() {
		    Connection con=null;
			state=THREAD_WORKING;
			PooledConnection pConn = null;
			final ConnectionPool pool = ConnectionPool.this;
			final int PoolMaxSize = poolConfig.getMaxActive();
			ConnectionFactory connFactory= poolConfig.getConnectionFactory();
			
			for(;;){
				state=THREAD_WORKING;
				if(PoolMaxSize>connList.size() && !waitQueue.isEmpty()){
					try {
						if((con=connFactory.create())!=null){
							setDefaultOnConnection(con);
							pConn = new PooledConnection(con,pool);
							pConn.setState(CONNECTION_USING);
							connList.add(pConn);
							release(pConn,true);
						}
					} catch (SQLException e) {
						if(con!=null)ConnectionUtil.oclose(con);
						transferSQLException(e);
					}finally{
						con=null;
					}
				}else{
					state=THREAD_WAITING;
					LockSupport.park(this);
				}
				if(state==THREAD_DEAD)break;
			}
		}
   }
   
   //Transfer Policy
   abstract class TransferPolicy {
		protected abstract int getCheckStateCode();
		protected abstract boolean tryCatch(PooledConnection pConn);
		protected abstract void onFailTransfer(PooledConnection pConn);
		protected abstract void beforeTransferReleaseConnection(PooledConnection pConn);
  	}
  	final class CompeteTransferPolicy extends TransferPolicy {
  		protected int getCheckStateCode(){
  			return CONNECTION_IDLE;
  		}
		protected boolean tryCatch(PooledConnection pConn){
			return pConn.compareAndSetState(CONNECTION_IDLE,CONNECTION_USING);
		}
		protected void onFailTransfer(PooledConnection pConn){
		}
		protected void beforeTransferReleaseConnection(PooledConnection pConn){
			pConn.setState(CONNECTION_IDLE);
		}
  	}
  	final class FairTransferPolicy extends TransferPolicy {
  		protected int getCheckStateCode(){
  			return CONNECTION_USING;
  		}
		protected boolean tryCatch(PooledConnection pConn){
			return pConn.getState()==CONNECTION_USING;
		}
		protected void onFailTransfer(PooledConnection pConn){
			pConn.setState(CONNECTION_IDLE);
		}
		protected void beforeTransferReleaseConnection(PooledConnection pConn){ 
		}
  	}
}