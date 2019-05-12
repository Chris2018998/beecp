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
import java.util.concurrent.atomic.AtomicReference;

/**
 * pooled connection Borrower
 *
 * @author Chris.Liao
 * @version 1.0
 */
final class Borrower {
	private Thread thread=null;
	private volatile Borrower pre=null;
	private AtomicReference<Borrower> nextRef=null;
	private volatile PooledConnection transferedConnection=null;
	
	private PooledConnection lastUsedConnection = null;
	private List<PooledConnection> badConnectionList = new LinkedList<PooledConnection>();
	public Borrower() {
		this.thread = Thread.currentThread();
		this.nextRef=new AtomicReference(null);
	}
	public Thread getThread() {
		return thread;
	}
	public Borrower getPre() {
		return pre;
	}
	public void setPre(Borrower pre) {
		this.pre = pre;
	}
	
	public boolean isOffChain(){
		return nextRef.get()==this;
	}
	public Borrower getNext() {
		return nextRef.get();
	}
	public void setNext(Borrower next) {
		nextRef.set(next);
	}
	public boolean compareAndSetNext(Borrower cur,Borrower next) {
		return nextRef.compareAndSet(cur, next);
	}
	
	public List<PooledConnection> getBadConnectionList() {
		return badConnectionList;
	}
	public PooledConnection getLastUsedConnection() {
		return lastUsedConnection;
	}
	public void setLastUsedConnection(PooledConnection lastUsedConnection) {
		this.lastUsedConnection = lastUsedConnection;
	}

	public PooledConnection getTransferedConnection() {
		return transferedConnection;
	}
	public void setTransferedConnection(PooledConnection transferedConnection) {
	   this.transferedConnection=transferedConnection;
	}	
}