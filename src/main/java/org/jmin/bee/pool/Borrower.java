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
	private PooledConnection lastUsedConnection = null;
	private AtomicReference<PooledConnection>transferRef=null;
	private List<PooledConnection> badConnectionList = new LinkedList<PooledConnection>();
	public Borrower() {
		thread = Thread.currentThread();
		transferRef=new AtomicReference<PooledConnection>(null);
	}
	public Thread getThread() {
		return thread;
	}
	public boolean equals(Object o){
		return this==o;
	}
	public List<PooledConnection> getBadConnectionList() {
		return badConnectionList;
	}
	public PooledConnection getLastUsedConn() {
		return lastUsedConnection;
	}
	public void setLastUsedConn(PooledConnection pConn) {
		lastUsedConnection = pConn;
	}
	public AtomicReference getTransferRef(){
		return transferRef;
	}	
}