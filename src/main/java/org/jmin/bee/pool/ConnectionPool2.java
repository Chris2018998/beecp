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
	private static PooledConnection DummyPooledConn = new PooledConnection();
	private ConcurrentLinkedQueue<Borrower> transferQueue = new ConcurrentLinkedQueue<Borrower>();

	public ConnectionPool2(BeeDataSourceConfig poolInfo) throws SQLException {
		super(poolInfo);
	}

	protected int getWaiterSize() {
		return transferQueue.size();
	}

	public PooledConnection waitForOne(long timeout, TimeUnit unit, Borrower waiter) {
		PooledConnection transConn = null;
		
		try {
			transferQueue.offer(waiter);
			if (waiter.getTransferConn() == null){ 
				LockSupport.parkNanos(unit.toNanos(timeout));
				if (waiter.getTransferConn()==null && !transferQueue.remove(waiter))
					waiter.setTransferConn(DummyPooledConn);
			}
			
			transConn=waiter.getTransferConn();
			return(transConn==DummyPooledConn)?null:transConn;
		} finally {
			waiter.setTransferConnAsNull();
		}
	}

	public void release(final PooledConnection pConn) throws SQLException {
		if (isCompete)
			pConn.setConnectionState(PooledConnectionState.IDLE);

		Borrower waiter = null;
		for (;;) {
			if (isCompete && pConn.getConnectionState() != PooledConnectionState.IDLE)
				return;
			
			if ((waiter = transferQueue.poll()) != null){
				if (waiter.setTransferConn(pConn)) {
					LockSupport.unpark(waiter.getThread());
					return;
				}
			}else{
				break;
			}
		}

		if (!isCompete)
			pConn.setConnectionState(PooledConnectionState.IDLE);
	}
}