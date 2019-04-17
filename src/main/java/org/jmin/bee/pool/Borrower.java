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

/**
 * pooled connection Borrower
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class Borrower {
	private Thread borrowerThread = null;
	private PooledConnection lastUsedConnection = null;
	private List<PooledConnection> badConnectionList = new LinkedList<PooledConnection>();
	public Borrower() {
		this.borrowerThread = Thread.currentThread();
	}
	public Thread getBorrowerThread() {
		return borrowerThread;
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
}