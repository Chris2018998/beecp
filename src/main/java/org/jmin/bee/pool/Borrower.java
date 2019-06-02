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

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
/**
 * pooled connection Borrower
 *
 * @author Chris.Liao
 * @version 1.0
 */
final class Borrower {
	private Thread thread=null;
	private PooledConnection lastUsedConnection=null;
	private volatile PooledConnection transferConn=null;
	private final static AtomicReferenceFieldUpdater<Borrower,PooledConnection> updater = AtomicReferenceFieldUpdater.newUpdater(Borrower.class,PooledConnection.class,"transferConn");
	
	public Borrower() {
		thread = Thread.currentThread();
	}
	public Thread getThread() {
		return thread;
	}
	public boolean equals(Object o){
		return this==o;
	}
	public PooledConnection getLastUsedConn() {
		return lastUsedConnection;
	}
	public void setLastUsedConn(PooledConnection pConn) {
		lastUsedConnection = pConn;
	}
	
	public void setTransferConnAsNull() {
		this.transferConn = null;
	}	
	public PooledConnection getTransferConn() {
		return transferConn;
	}
	public boolean setTransferConn(PooledConnection pConn) {
		return updater.compareAndSet(this, null, pConn);
	}
}