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
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
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
	private boolean validateSQLIsNull;
	protected final BeeDataSourceConfig poolConfig;

	private final Semaphore poolSemaphore;
	private final TransferPolicy tansferPolicy;
	private final int transferCheckStateCode;
	private final CreateConnThread createThread=new CreateConnThread();
	private final PooledConnectionList connList=new PooledConnectionList();
	private final ConcurrentLinkedQueue<Borrower> waitQueue = new ConcurrentLinkedQueue<Borrower>();
	private final ThreadLocal<WeakReference<Borrower>> threadLocal = new ThreadLocal<WeakReference<Borrower>>();

	private String poolName="";
	private static String poolNamePrefix="Pool-";
	private static AtomicInteger poolNameIndex=new AtomicInteger(1);
	private volatile boolean surpportQryTimeout=true;
	protected static final SQLException PoolCloseException = new SQLException("Pool has been closed");
	protected static final SQLException RequestTimeoutException = new SQLException("Request timeout");
	protected static final SQLException RequestInterruptException = new SQLException("Request interrupt");
	
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
			
			validateSQLIsNull = isNull(poolConfig.getValidationQuery());
			idleCheckTimer = new Timer(true);
			idleCheckTimer.schedule(new PooledConnectionIdleTask(), 60000L, 180000L);
			exitHook = new ConnectionPoolHook();
			Runtime.getRuntime().addShutdownHook(exitHook);
			
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
			System.out.println("BeeCP("+poolName+")has been startup{init size:"+connList.size()+",max size:"+config.getMaximumPoolSize()+",concurrent size:"+poolConfig.getConcurrentSize()+ ",mode:"+mode +",max wait:"+poolConfig.getMaxWait()+"ms}");
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
	private final boolean isClosed() {
		return state == POOL_CLOSED;
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
		snapshotMap.put("PoolMaxSize", poolConfig.getMaximumPoolSize());
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
				return conn.isValid(poolConfig.getValidationQueryTimeout());
			} catch (SQLException e) {
				return false;
			}
		} else {
			Statement st = null;
			try {
				st = conn.createStatement();
				setsetQueryTimeout(st);
				st.execute(poolConfig.getValidationQuery());
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
				st.setQueryTimeout(poolConfig.getValidationQueryTimeout());
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
	private boolean checkConnection(PooledConnection pConn) {
		if(currentTimeMillis()-pConn.getLastActiveTime()<=poolConfig.getValidationInterval()) 
			return true;
		
		if(isActiveConn(pConn))return true;
		pConn.setState(CONNECTION_CLOSED);
		pConn.closePhysicalConnection();
		connList.remove(pConn);
		return false;
	}
	private boolean checkOnBorrow(PooledConnection pConn) {
		return poolConfig.isTestOnBorrow()?checkConnection(pConn):true;
	}
	private boolean checkOnReturn(PooledConnection pConn) {
		return poolConfig.isTestOnReturn()?checkConnection(pConn):true;
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
				con=connFactory.create();
				initNewConneciton(con);
				connList.add(new PooledConnection(con,poolConfig.getPreparedStatementCacheSize(),this));
			}
		} catch (SQLException e) {
			throw e;
		}
	}
	
	/**
	 * borrow a connection from pool
	 * @return If exists idle connection in pool,then return one;if not, waiting until other borrower release
	 * @throws SQLException if pool is closed or waiting timeout,then throw exception
	 */
	public final Connection getConnection() throws SQLException {
		return getConnection(poolConfig.getMaxWait());
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
		if(isClosed())throw PoolCloseException;
		
		Borrower borrower=null;
		PooledConnection pConn=null;
		
		WeakReference<Borrower> bRef=threadLocal.get();
		if(bRef!=null)borrower=bRef.get();
		if(borrower==null){
			borrower = new Borrower(this);
			bRef=new WeakReference<Borrower>(borrower);
			threadLocal.set(bRef);	
		}else if((pConn= borrower.getLastUsedConn())!=null){
			if(pConn.compareAndSetState(CONNECTION_IDLE,CONNECTION_USING)){
				if(checkOnBorrow(pConn)) 
				   return createProxyConnection(pConn, borrower);
				else
				   borrower.setLastUsedConn(null);
			}
		}
		
		return getConnection(wait,borrower);
	}
	
	//Concurrent control
	protected Connection getConnection(long wait,Borrower borrower)throws SQLException {
		boolean acquired=false;
		try{
			wait=MILLISECONDS.toNanos(wait);
			long deadlineNanos=nanoTime()+wait;
			
			if(acquired=poolSemaphore.tryAcquire(wait,NANOSECONDS)){
				return takeOneConnection(deadlineNanos,borrower);
			}else{
				throw RequestTimeoutException;
			}	
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw RequestInterruptException;
		}finally {
		  if(acquired)poolSemaphore.release();
		}
	}
	
	//take one connection
    final Connection takeOneConnection(long deadlineNanos,Borrower borrower)throws SQLException {
    	for(PooledConnection sPConn:connList.getArray()) {
			if(sPConn.compareAndSetState(CONNECTION_IDLE,CONNECTION_USING) && checkOnBorrow(sPConn)) {
				return createProxyConnection(sPConn, borrower);
			}
		}
    	
    	//wait one transfered connection 
		boolean timeout=false;
		Object stateObject=null;
		try{
			long waitNanos=0;
			borrower.resetThread();
			borrower.resetStateObject();//BORROWER_NORMAL	
			waitQueue.offer(borrower);
			tryToCreateNewConnByAsyn();
			 
			for(;;){
				stateObject=borrower.getStateObject();
				if(stateObject instanceof PooledConnection){
					PooledConnection pConn=(PooledConnection)stateObject;
					
					//fix issue:#3 Chris-2019-08-01 begin
					//if(tansferPolicy.tryCatch(pConn){
					if(tansferPolicy.tryCatch(pConn)&& this.checkOnBorrow(pConn)){
					//fix issue:#3 Chris-2019-08-01 end
				    	return createProxyConnection(pConn,borrower);
				    }else{
				    	borrower.resetStateObject();
				    }
				}else if(stateObject instanceof SQLException){
					throw (SQLException)stateObject;
				}
			
				if(timeout){
					if(borrower.compareAndSetStateObject(BORROWER_NORMAL,BORROWER_TIMEOUT))
						throw RequestTimeoutException;
				
					continue; 
				}
				
				waitNanos=deadlineNanos-nanoTime();
				if(waitNanos<=0){timeout=true;continue;}
				if(borrower.compareAndSetStateObject(BORROWER_NORMAL,BORROWER_WAITING))
					LockSupport.parkNanos(borrower,waitNanos);
			}//for 
		}finally{
			waitQueue.remove(borrower);
		}
    }
    
	//create proxy to wrap connection as result
	private ProxyConnection createProxyConnection(PooledConnection pConn,Borrower borrower)throws SQLException {
		borrower.setLastUsedConn(pConn);
		ProxyConnection proxyConn = ProxyConnectionFactory.createProxyConnection(pConn);
		pConn.bindProxyConnection(proxyConn);
		pConn.updateLastActivityTime();
		return proxyConn; 
	}
 	//notify to create connections to pool 
	private void tryToCreateNewConnByAsyn() {
		if(createThread.state==THREAD_WAITING && poolConfig.getMaximumPoolSize()>connList.size()){
			createThread.state=THREAD_WORKING;
			LockSupport.unpark(createThread);
		}
	}
	
	/**
	 * return connection to pool
	 * @param pConn target connection need release
	 */
	void release(PooledConnection pConn,boolean isNew) {
		if(!isNew && !checkOnReturn(pConn))return;
		tansferPolicy.beforeTransferReleaseConnection(pConn);
		
		Thread waitThred;
		Object waiterState=null;
		for(Borrower waiter:waitQueue){
			if(pConn.getState()!=transferCheckStateCode)return;
			
			waitThred=waiter.getThread();
			waiterState=waiter.getStateObject();
			if(waitThred.isAlive() && (waiterState==BORROWER_NORMAL||waiterState==BORROWER_WAITING) && waiter.compareAndSetStateObject(waiterState,pConn)){
				if(waiterState==BORROWER_WAITING)LockSupport.unpark(waitThred);
				return;
			} 
		}
		
		tansferPolicy.onFailTransfer(pConn);
	}
	private void transferSQLException(SQLException exception){
		Object waiterState=null;
		for(Borrower waiter:waitQueue){
			waiterState=waiter.getStateObject();
			if((waiterState==BORROWER_NORMAL||waiterState==BORROWER_WAITING) && waiter.compareAndSetStateObject(waiterState,exception)){
				if(waiterState==BORROWER_WAITING)LockSupport.unpark(waiter.getThread());
				return;
			}
		}
	}
	//set some inited parameter
	private final void initNewConneciton(Connection connection)throws SQLException{
		connection.setAutoCommit(poolConfig.isDefaultAutoCommit());
		connection.setReadOnly(poolConfig.isReadOnly());
		if(ConnectionUtil.isNull(poolConfig.getCatalog()))
		  connection.setCatalog(poolConfig.getCatalog());
		connection.setTransactionIsolation(poolConfig.getDefaultTransactionIsolation());
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
					final boolean isTimeout = ((currentTimeMillis()-pConn.getLastActiveTime()>=poolConfig.getIdleTimeout()));
					if ((isDead || isTimeout) && (pConn.compareAndSetState(state,CONNECTION_CLOSED))) {
						pConn.closePhysicalConnection();
						badConList.add(pConn);
					}

				} else if (state == CONNECTION_USING) {
					final boolean isDead = !isActiveConn(pConn);
					final boolean isTimeout = ((currentTimeMillis()-pConn.getLastActiveTime()>=poolConfig.getMaxHoldTimeInUnused()));
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

	/**
	 * resource release on pool closing
	 */
	public void destroy() {
		if (isNormal()) {
			state = POOL_CLOSED;
			idleCheckTimer.cancel();
			
			long parkNanos=SECONDS.toNanos(1);
			while(existBorrower())LockSupport.parkNanos(parkNanos);
			
			createThread.shutdown();
	
			//clear all connections
			while (connList.size() > 0) {
				for (PooledConnection pConn : connList.getArray()) {
					if (pConn.compareAndSetState(CONNECTION_IDLE,CONNECTION_CLOSED)) {
						pConn.closePhysicalConnection();
						connList.remove(pConn);
					} else if (pConn.getState() ==CONNECTION_CLOSED) {
						connList.remove(pConn);
					} else if (pConn.getState() == CONNECTION_USING) {
						final boolean isDead = !isActiveConn(pConn);
						final boolean isTimeout = ((currentTimeMillis()-pConn.getLastActiveTime()>=poolConfig.getMaxHoldTimeInUnused()));
						if ((isDead || isTimeout) && (pConn.compareAndSetState(CONNECTION_USING,CONNECTION_CLOSED))) {
							pConn.closePhysicalConnection();
							connList.remove(pConn);
						}
					}
				}//for
				
				if (connList.size() > 0)
					LockSupport.parkNanos(parkNanos);
			}//while
			
			try {
				Runtime.getRuntime().removeShutdownHook(exitHook);
			} catch (Throwable e) {}
			System.out.println("BeeCP("+poolName+")has been shutdown");
		}
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
			final int PoolMaxSize = poolConfig.getMaximumPoolSize();
			final int CacheSize = poolConfig.getPreparedStatementCacheSize();
			ConnectionFactory connFactory= poolConfig.getConnectionFactory();
			
			for(;;){
				state=THREAD_WORKING;
				if(PoolMaxSize>connList.size() && !waitQueue.isEmpty()){
					try {
						con=connFactory.create();
						initNewConneciton(con);
						pConn = new PooledConnection(con,CacheSize,pool);
						pConn.setState(CONNECTION_USING);
						connList.add(pConn);
						release(pConn,true);
					} catch (SQLException e) {
						transferSQLException(e);
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
  	class CompeteTransferPolicy extends TransferPolicy {
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
  	class FairTransferPolicy extends TransferPolicy {
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