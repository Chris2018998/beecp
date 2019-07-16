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
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import static org.jmin.bee.pool.PoolObjectsState.BORROWER_NORMAL;
 
/**
 * pooled connection Borrower
 *
 * @author Chris.Liao
 * @version 1.0
 */
final class Borrower{
	volatile int transferState;
	private volatile Thread thread=null;
	private PooledConnection lastUsedConn=null;
	private volatile PooledConnection transferedConn=null;
	private volatile SQLException transferedException=null;
	private final static AtomicIntegerFieldUpdater<Borrower> updater = AtomicIntegerFieldUpdater.newUpdater(Borrower.class,"transferState");

	public Borrower(ConnectionPool pool) {
		thread = Thread.currentThread();
	}
	public Thread getThread() {
		return thread;
	}
	public boolean equals(Object o){
		return this==o;
	}
	public PooledConnection getLastUsedConn() {
		return lastUsedConn;
	}
	public void setLastUsedConn(PooledConnection pConn) {
		lastUsedConn = pConn;
	}
	public int getState() {
		return updater.get(this);
	}
	public void resetState() {
		this.transferedConn=null;
		this.transferedException=null;
		updater.set(this,BORROWER_NORMAL);
	}
	public boolean compareAndSetState(int cur,int exp) {
		return updater.compareAndSet(this,cur,exp);
	}
	public PooledConnection getTransferedConn() {
		return transferedConn;
	}
	public void setTransferedConn(PooledConnection transferedConn) {
		this.transferedConn = transferedConn;
	}
	public SQLException getTransferedException() {
		return transferedException;
	}
	public void setTransferedException(SQLException transferedException) {
		this.transferedException = transferedException;
	}
}