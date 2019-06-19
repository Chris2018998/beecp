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

import static java.lang.System.nanoTime;
import static java.lang.Thread.yield;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
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
	private static final int STATE_UNINIT = 0;
	private static final int STATE_NORMAL = 1;
	private static final int STATE_CLOSED = 2;
	private volatile int state=STATE_UNINIT;
	private Timer idleCheckTimer;
	private ConnectionPoolHook exitHook;
	private boolean validateSQLIsNull;
	protected final BeeDataSourceConfig info;
	
	private final int maxAcquireSize;
	private final TransferPolicy tansferPolicy;
	private final PoolBorrowSemaphore takeSemaphore;
	protected final AtomicInteger waiterSize = new AtomicInteger(0);
	private final PooledConnectionList connList=new PooledConnectionList();
	private final SynchronousQueue<Object> transferQueue = new SynchronousQueue<Object>(true);
	private final ThreadLocal<WeakReference<Borrower>> threadLocal = new ThreadLocal<WeakReference<Borrower>>();
	private final CreateConnectionTask createconnTask=new CreateConnectionTask();
	private final ThreadPoolExecutor createConnExecutor=new ThreadPoolExecutor(1,1,5,SECONDS,new LinkedBlockingQueue<Runnable>());
	
	private volatile boolean surpportQryTimeout=true;
	private static final long TransferParkNanos=10000L;
	private static final int TransferFailSizeNeedPark=100;
	private static final long MAX_IDLE_TIME_IN_USING = 600000L;
	private static final SQLException PoolCloseStateException = new SQLException("Pool has been closed");
	private static final SQLException ConnectionRequestTimeoutException = new SQLException("Request timeout");
	private static final SQLException ConnectionRequestInterruptException = new SQLException("Request interrupt");

	/**
	 * initialize pool with configuration
	 * 
	 * @param poolInfo data source configuration
	 * @throws SQLException check configuration fail or to create initiated connection 
	 */
	public ConnectionPool(BeeDataSourceConfig poolInfo) throws SQLException {
		if (poolInfo == null)
			throw new SQLException("Pool info can't be null");
		if (state == STATE_UNINIT) {
			poolInfo.check();
			checkProxyClasses();
			info = poolInfo;
			info.setInited(true);
			validateSQLIsNull = ConnectionUtil.isNull(info.getValidationQuerySQL());

			idleCheckTimer = new Timer(true);
			idleCheckTimer.schedule(new PooledConnectionIdleTask(), 60000, 180000);
			exitHook = new ConnectionPoolHook();
			Runtime.getRuntime().addShutdownHook(exitHook);
			
			String mode = "";
			if (info.isFairMode()) {
				mode = "fair";
				tansferPolicy = new FairTransferPolicy();
			} else {
				mode = "compete";
				tansferPolicy = new CompeteTransferPolicy();
			}
		
			createInitConns();
			createConnExecutor.allowCoreThreadTimeOut(true);
			maxAcquireSize=info.getMaxAcquireSize();
			takeSemaphore=new PoolBorrowSemaphore(maxAcquireSize,info.isFairMode());
			System.out.println("BeeCP("+info.getPoolName()+")has been startup{init size:" + getCurConnSize() + ",max size:"+poolInfo.getPoolMaxSize() + ",mode:"+mode +",max acquire size:"+maxAcquireSize+",max wait:"+info.getBorrowerMaxWaitTime()+"ms}");
			state = STATE_NORMAL;
		} else {
			throw new SQLException("Pool has been initialized");
		}
	}

	/**
	 * check some proxy class whether exists
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
			throw new SQLException("Some pool jdbc proxy classes are missed");
		}
	}
	private boolean existWaiter() {
		return waiterSize.get()>0;
	}
	private final boolean isNormal() {
		return state == STATE_NORMAL;
	}
	private final boolean isClosed() {
		return state == STATE_CLOSED;
	}
	private final int getCurConnSize(){
		return connList.size();
	}
	private int getIdleConnSize(){
		int conIdleSize=0;
		for (PooledConnection pConn:connList.getArray()) {
			if (pConn.getConnectionState() == PooledConnectionState.IDLE)
				conIdleSize++;
		}
		return conIdleSize;
	}
	public Map<String,Integer> getPoolSnapshot(){
		int conCurSize=connList.size();
		int conIdleSize=getIdleConnSize();
		int waitingSize=waiterSize.get()+takeSemaphore.getQueueLength();
		
		Map<String,Integer> snapshotMap = new LinkedHashMap<String,Integer>();
		snapshotMap.put("PoolMaxSize", info.getPoolMaxSize());
		snapshotMap.put("ConCurSize",conCurSize);
		snapshotMap.put("ConIdleSize",conIdleSize);
		snapshotMap.put("WaiterSize",waitingSize);
		return snapshotMap;
	}
	private void checkPool() throws SQLException {
		if(isClosed())throw PoolCloseStateException;
	}
	
	/**
	 * check connection state,when
	 * @return if the checked connection is active then return true,otherwise false     
	 */
	private final boolean isActiveConn(PooledConnection pConn) {
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
	private boolean checkOnBorrowed(PooledConnection pConn) {
		if(SystemClock.curTimeMillis()-pConn.getLastActiveTime()<=info.getMaxInactiveTimeToCheck()) 
			return true;
		
		if(isActiveConn(pConn))return true;
		pConn.setConnectionState(PooledConnectionState.CLOSED);
		pConn.closePhysicalConnection();
		connList.remove(pConn);
		return false;
	}
	
	/**
	 * create initialization connections
	 * 
	 * @throws SQLException
	 *             error occurred in creating connections
	 */
	private void createInitConns() throws SQLException {
		String connectURL=info.getConnectURL();
		Driver connectDriver=info.getConnectDriver();
		Properties connectProperties= info.getConnectProperties();
		int size = info.getPoolInitSize();
		ArrayList<PooledConnection> tempList = new ArrayList<PooledConnection>(size);
		try {
			for (int i = 0; i < size; i++) 
			tempList.add(new PooledConnection(connectDriver.connect(connectURL,connectProperties),info.getPreparedStatementCacheSize(),this));
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
		connList.addAll(tempList);
	}

	/**
	 * borrow a connection from pool
	 * @return If exists idle connection in pool,then return one;if not, waiting until other borrower release
	 * @throws SQLException if pool is closed or waiting timeout,then throw exception
	 */
	public final Connection getConnection() throws SQLException {
		return getConnection(info.getBorrowerMaxWaitTime());
	}

	/**
	 * borrow a connection from pool
	 * 
	 * @param wait
	 *            max wait time for borrower
	 * @return If exists idle connection in pool,then return one;if not, waiting
	 *         until other borrower release
	 * @throws SQLException
	 *             if pool is closed or waiting timeout,then throw exception
	 */
	public final Connection getConnection(long wait) throws SQLException {
		checkPool();
		boolean acquired=false;
		PooledConnection pConn=null;
	
		WeakReference<Borrower> bRef=threadLocal.get();
		Borrower borrower=(bRef!=null)?bRef.get():null;
		if (borrower == null) {
			borrower = new Borrower();
			threadLocal.set(new WeakReference<Borrower>(borrower));
		}else{
			pConn = borrower.getLastUsedConn();
			if (pConn!=null && pConn.compareAndSet(PooledConnectionState.IDLE,PooledConnectionState.USING)) {//step1: try to reuse one
				if (checkOnBorrowed(pConn))
					return createProxyConnection(pConn,borrower);
				else 
					borrower.setLastUsedConn(null);
			}
		}
		
		try {
			wait=MILLISECONDS.toNanos(wait);
			final long deadlinePoint= nanoTime()+wait;
			if(acquired = takeSemaphore.tryAcquire(wait,NANOSECONDS)){
				for(;;){
					for(PooledConnection sPConn:connList.getArray()) {//step2:search one
						if(sPConn.compareAndSet(PooledConnectionState.IDLE, PooledConnectionState.USING)){
							if(checkOnBorrowed(sPConn)) 
							  return createProxyConnection(sPConn, borrower);
						}
					}//inside for
					
					if((wait=deadlinePoint-nanoTime())<=0L)break;
					if((pConn=waitForOne(wait,NANOSECONDS,borrower))!=null){//step4:wait one
						if(tansferPolicy.tryCatch(pConn) && checkOnBorrowed(pConn))
							return createProxyConnection(pConn,borrower);
					}
				}//outside for	
			}
		} catch (InterruptedException e) {
			throw ConnectionRequestInterruptException;
		} finally {
			if(acquired)takeSemaphore.release();
		}
		
		if (isClosed())
			throw PoolCloseStateException;
		else
			throw ConnectionRequestTimeoutException;
	}
	//create proxy to wrap connection as result
	private static ProxyConnection createProxyConnection(PooledConnection pConn,Borrower borrower)throws SQLException {
		borrower.setLastUsedConn(pConn);
		ProxyConnection proxyConn = ProxyConnectionFactory.createProxyConnection(pConn);
		pConn.bindProxyConnection(proxyConn);
		pConn.updateLastActivityTime();
		return proxyConn; 
	}
	//wait for one transfered connection
	protected PooledConnection waitForOne(long timeout,TimeUnit unit,Borrower borrower)throws InterruptedException, SQLException {
		Object tfv=null;
		try {
			waiterSize.incrementAndGet();
			tryTocreateNewConnByAsyn();// notify to create connections
			
			tfv= transferQueue.poll(timeout,unit);
		    if(tfv!=null)return readTransferPooledConn(tfv); 
		    return null;
		} finally {
			waiterSize.decrementAndGet();
		}
	}
	//notify to create connections to pool 
	protected void tryTocreateNewConnByAsyn() {
		if (connList.size() < info.getPoolMaxSize() && !createconnTask.isRunning){
			createconnTask.isRunning=true;
			createConnExecutor.execute(createconnTask);
		}
	}
	//get connection from transfered value
 	protected static PooledConnection readTransferPooledConn(Object tranferVal)throws SQLException{
 		if(tranferVal instanceof PooledConnection)return(PooledConnection)tranferVal;
		if(tranferVal instanceof SQLException) throw (SQLException)tranferVal;
		if(tranferVal instanceof Throwable) throw new SQLException((Throwable)tranferVal);
		return null;
	}
	//transfer to waiter(Pull or Push)
	protected boolean transfer(Object transferVal){
		return transferQueue.offer(transferVal);
	}
	
	/**
	 * return connection to pool
	 * 
	 * @param pConn
	 *            target connection need release
	 * @param isNewConn 
	 *  		  true:new connection,false:release by borrower      
	 */
	public void returnToPool(PooledConnection pConn) {
	  tansferPolicy.onBeforeTransfer(pConn);
 	  tansferPolicy.transferConnection(pConn,tansferPolicy.getChkState());
	}
	
	/**
	 * inner timer will call the method to clear some idle timeout connections
	 * or dead connections,or long time not active connections in using state
	 */
	public void closeIdleTimeoutConnection() {
		if (isNormal() && !existWaiter()) {
			LinkedList<PooledConnection> badConList = new LinkedList<PooledConnection>();
			for (PooledConnection pConn:connList.getArray()) {
				final int state = pConn.getConnectionState();
				if (state == PooledConnectionState.IDLE) {
					final boolean isDead = !isActiveConn(pConn);
					final boolean isTimeout = ((SystemClock.curTimeMillis()-pConn.getLastActiveTime()>=info.getConnectionIdleTimeout()));
					if ((isDead || isTimeout) && (pConn.compareAndSet(state, PooledConnectionState.CLOSED))) {
						pConn.closePhysicalConnection();
						badConList.add(pConn);
					}

				} else if (state == PooledConnectionState.USING) {
					final boolean isDead = !isActiveConn(pConn);
					final boolean isTimeout = ((SystemClock.curTimeMillis()-pConn.getLastActiveTime()>=MAX_IDLE_TIME_IN_USING));
					if ((isDead || isTimeout) && (pConn.compareAndSet(state, PooledConnectionState.CLOSED))) {

						pConn.closePhysicalConnection();
						badConList.add(pConn);
					}
				} else if (state == PooledConnectionState.CLOSED) {
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
			state = STATE_CLOSED;
			idleCheckTimer.cancel();			
			createConnExecutor.shutdown();
			
			while(takeSemaphore.hasQueuedThreads() || waiterSize.get()>0 || takeSemaphore.availablePermits() < maxAcquireSize){
				LockSupport.parkNanos(1000L);
			}
			
			//clear all connections
			LinkedList<PooledConnection> badConList = new LinkedList<PooledConnection>();
			while (connList.size() > 0) {
				for (PooledConnection pConn : connList.getArray()) {
					if (pConn.compareAndSet(PooledConnectionState.IDLE, PooledConnectionState.CLOSED)) {
						pConn.closePhysicalConnection();
						badConList.add(pConn);
					} else if (pConn.getConnectionState() == PooledConnectionState.CLOSED) {
						badConList.add(pConn);
					} else if (pConn.getConnectionState() == PooledConnectionState.USING) {
						final boolean isDead = !isActiveConn(pConn);
						final boolean isTimeout = ((SystemClock.curTimeMillis()-pConn.getLastActiveTime()-MAX_IDLE_TIME_IN_USING >=0));
						if ((isDead || isTimeout) && (pConn.compareAndSet(PooledConnectionState.USING, PooledConnectionState.CLOSED))) {
							pConn.closePhysicalConnection();
							badConList.add(pConn);
						}
					}
				}//for
				
				if(badConList.size()>0){
					connList.removeAll(badConList);
					badConList.clear();
				}
				if (connList.size() > 0)
					LockSupport.parkNanos(1000L);
			}//while
			
			try {
				Runtime.getRuntime().removeShutdownHook(exitHook);
			} catch (Throwable e) {}
			SystemClock.terminate();
			System.out.println("BeeCP("+info.getPoolName()+")has been shutdown");
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
    private class CreateConnectionTask implements Runnable {
		private volatile boolean isRunning = false;
		public void run() {
			isRunning=true;
			int addSize=0,borrowerSize=0;
			PooledConnection pConn=null;
			final ConnectionPool pool=ConnectionPool.this;
			final int PoolMaxSize=info.getPoolMaxSize();
			final int CacheSize=info.getPreparedStatementCacheSize();
	
			String url=info.getConnectURL();
			Driver driver=info.getConnectDriver();
			Properties prop=info.getConnectProperties();
			boolean addToHead=false;
			
			while(true){ 
				borrowerSize=takeSemaphore.getQueueLength()+maxAcquireSize-takeSemaphore.availablePermits();
				addSize=Math.min(PoolMaxSize-connList.size(),borrowerSize);
				if(addSize<=0)break;
				for (int i = 0; i < addSize; i++) {
					try {
						addToHead=true;
						pConn = new PooledConnection(driver.connect(url, prop), CacheSize, pool);
						if (waiterSize.get()>0){//mock action:release connection to pool
							addToHead=false;
							pConn.setConnectionState(PooledConnectionState.USING);
						    new NewConnTransferThread(pConn).start();
						}
						
						connList.add(pConn,addToHead);
					} catch (SQLException e) {
						if (waiterSize.get()>0)new ExceptionTransferThread(e).start();
					}
				}
			} // while
			isRunning = false;
		}
    }

    private class NewConnTransferThread extends Thread {
		private PooledConnection pConn;
		public NewConnTransferThread(PooledConnection pConn) {
			this.pConn = pConn;
		}
		public void run() {
			returnToPool(pConn);
		}
	}
	private class ExceptionTransferThread extends Thread {
		private SQLException exception;
		public ExceptionTransferThread(SQLException exception) {
			this.exception = exception;
		}
		public void run() {
			tansferPolicy.transferException(exception);
		}
    }
	
    //Transfer Policy
	abstract class TransferPolicy {
		boolean transferException(SQLException exception) {
			int failTimes=0;
			int ParkIndex=TransferFailSizeNeedPark;
			while (waiterSize.get()>0) {
				if(transfer(exception))return true;
				if(++failTimes==ParkIndex){
					LockSupport.parkNanos(exception,TransferParkNanos);
					ParkIndex +=TransferFailSizeNeedPark;
				}else {
					Thread.yield();
				}
			}
			return false;
		}
		
		boolean transferConnection(PooledConnection pConn,int chkState) {
			int failTimes=0;
			int ParkIndex = TransferFailSizeNeedPark;
			
			while(waiterSize.get() > 0 && pConn.getConnectionState()==chkState) {
				if(transfer(pConn))return true;
				if (++failTimes==ParkIndex) {
					LockSupport.parkNanos(pConn,TransferParkNanos);
					ParkIndex += TransferFailSizeNeedPark;
				} else{
					yield();
				}
			}
			
			onTransferFail(pConn);
			return false;
		}
	
		public abstract int getChkState();
		public abstract void onBeforeTransfer(PooledConnection pConn);//call at returnToPool used to change state
		protected abstract void onTransferFail(PooledConnection pConn);//call at exit transfer iteration 
		public boolean tryCatch(PooledConnection pConn) {//call at main iteration to hold a transfer connection
			return pConn.compareAndSet(PooledConnectionState.IDLE,PooledConnectionState.USING);
		}	
	}
	class CompeteTransferPolicy extends TransferPolicy {
		public int getChkState(){return PooledConnectionState.IDLE;}
		public void onBeforeTransfer(PooledConnection pConn){
			pConn.setConnectionState(PooledConnectionState.IDLE);
		}
		public void onTransferFail(PooledConnection pConn){}
	}
	class FairTransferPolicy extends TransferPolicy {
		public int getChkState(){return PooledConnectionState.USING;}
		public void onBeforeTransfer(PooledConnection pConn){}//do nothing here
		public void onTransferFail(PooledConnection pConn){
			pConn.setConnectionState(PooledConnectionState.IDLE);
		}
		public boolean tryCatch(PooledConnection pConn) {
			return pConn.getConnectionState()==PooledConnectionState.USING;
		}
	}
}