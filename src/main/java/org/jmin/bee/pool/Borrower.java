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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * pooled connection Borrower
 *
 * @author Chris.Liao
 * @version 1.0
 */
final class Borrower {
	public static final int STATE_NORMAL        = 0;
	public static final int STATE_WAIT_INIT     = 1;
	public static final int STATE_WAITING       = 2;
	public static final int STATE_TRANSFERED    = 3;

	private Thread borrowerThread = null;
	private PooledConnection lastUsedConnection = null;
	private PooledConnection transferedConnection = null;
	private AtomicInteger state=new AtomicInteger(STATE_NORMAL);
	private List<PooledConnection> badConnectionList = new LinkedList<PooledConnection>();
	
	public Borrower() {
		this.borrowerThread = Thread.currentThread();
	}
	public Thread getThread() {
		return borrowerThread;
	}
	public int getState() {
		return this.state.get();
	}
	public void seState(int update) {
		this.state.set(update);
	}
	public boolean compareAndSetState(int expect, int update) {
		return this.state.compareAndSet(expect, update);
	}
	public PooledConnection getLastUsedConnection() {
		return lastUsedConnection;
	}
	public void setLastUsedConnection(PooledConnection lastUsedConnection) {
		this.lastUsedConnection = lastUsedConnection;
	}
	public List<PooledConnection> getBadConnectionList() {
		return badConnectionList;
	}
	public PooledConnection getTransferedConnection() {
		return transferedConnection;
	}
	public void setTransferedConnection(PooledConnection transferedConnection) {
		this.transferedConnection = transferedConnection;
	}
}