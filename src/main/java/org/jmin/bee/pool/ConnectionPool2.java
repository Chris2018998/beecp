/*
 * Copyright Chris Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http:www.apache.org/licenses/LICENSE-2.0
 */
package org.jmin.bee.pool;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.jmin.bee.BeeDataSourceConfig;

/**
 * JDBC Connection Pool Implementation
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public final class ConnectionPool2 extends ConnectionPool {
	private final static PooledConnection DummyTransferVal=new PooledConnection();
	private ConcurrentLinkedQueue<Borrower> transferQueue = new ConcurrentLinkedQueue<Borrower>();
	public ConnectionPool2(BeeDataSourceConfig poolInfo) throws SQLException {
		super(poolInfo);
	}
	
	public PooledConnection waitForOne(long timeout, TimeUnit unit, Borrower waiter)throws InterruptedException,SQLException{
		try {
			Object tfv=null;
			waiterSize.incrementAndGet();
			transferQueue.offer(waiter);
			tryTocreateNewConnByAsyn();// notify to create connections

			if((tfv=waiter.getTransferVal())!=null)return readTransferPooledConn(tfv);
	
			LockSupport.parkNanos(waiter,unit.toNanos(timeout));
			
			if((tfv=waiter.getTransferVal())!=null)return readTransferPooledConn(tfv);
			
			if(transferQueue.remove(waiter)){waiterSize.decrementAndGet();return null;}
			if(waiter.setTransferVal(DummyTransferVal))return null;
	
			if((tfv=waiter.getTransferVal())!=null)return readTransferPooledConn(tfv);
			return null;
		 } finally {
		    waiter.setTransferValAsNull();
		}
	}
	
	//transfer to waiter
	protected boolean transfer(Object transferVal){
		Borrower waiter=null;
		boolean transfered=false;
		if ((waiter = transferQueue.poll()) != null) {
			waiterSize.decrementAndGet();
			if (waiter.setTransferVal(transferVal)) {
				transfered=true;
				LockSupport.unpark(waiter.getThread());
			}
		}
		return transfered;
	}
}