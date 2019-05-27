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
	
	public PooledConnection waitForOne(long timeout, TimeUnit unit, Borrower waiter) {
		try {
			waiterSize.incrementAndGet();
			transferQueue.offer(waiter);
			if(waiter.getTransferConn()!=null)return waiter.getTransferConn();
			
			LockSupport.parkNanos(waiter,unit.toNanos(timeout));
			if(waiter.getTransferConn()!=null)return waiter.getTransferConn();
			
			return transferQueue.remove(waiter)?null:(waiter.setTransferConn(DummyTransferVal)?null:waiter.getTransferConn());
		 } finally {
			waiter.setTransferConnAsNull();
			waiterSize.decrementAndGet();
		}
	}

	public void release(final PooledConnection pConn) throws SQLException {
		if (isCompete)pConn.setConnectionState(PooledConnectionState.IDLE);
		
		Borrower waiter=null;
		for (;;) {
			if (isCompete && pConn.getConnectionState() != PooledConnectionState.IDLE)
				return;
			
			if((waiter=transferQueue.poll())==null){
				break;
			}else if(waiter.setTransferConn(pConn)){
				LockSupport.unpark(waiter.getThread());
				return;
			}
		}
		if (!isCompete)pConn.setConnectionState(PooledConnectionState.IDLE);
	}
}