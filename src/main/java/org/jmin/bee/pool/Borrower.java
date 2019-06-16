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
	private volatile Object transferVal=null;
	private PooledConnection lastUsedConn=null;
	private final static AtomicReferenceFieldUpdater<Borrower,Object> updater = AtomicReferenceFieldUpdater.newUpdater(Borrower.class,Object.class,"transferVal");
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
		return lastUsedConn;
	}
	public void setLastUsedConn(PooledConnection pConn) {
		lastUsedConn = pConn;
	}
	public void setTransferValAsNull() {
		this.transferVal=null;
	}
	public Object getTransferVal() {
		return transferVal;
	}
	public boolean setTransferVal(Object val) {
		return updater.compareAndSet(this, null, val);
	}
}