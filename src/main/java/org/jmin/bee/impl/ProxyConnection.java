/*
 * Copyright (C) Chris Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmin.bee.impl;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * physical connection wrapper
 * 
 * @author Chris
 * @version 1.0
 */
public abstract class ProxyConnection implements Connection {
	private boolean isClosed;
	protected Connection delegate;
	private FastConnectionPool connectionPool;
	protected PooledConnection pooledConnection;
	private boolean autoCommitValue=false;
	private boolean autoCommitChanged = false;
	private boolean transactionLevlChanged = false;

	public ProxyConnection(PooledConnection pooledConnection, FastConnectionPool connectionPool) {
		this.connectionPool = connectionPool;
		this.pooledConnection = pooledConnection;
		this.delegate = pooledConnection.getPhisicConnection();
		this.autoCommitValue=pooledConnection.isAutoCommit();
	}

	public boolean isClosed() {
		return isClosed;
	}

	public void checkClosed() throws SQLException {
		if (isClosed)
			throw new SQLException("Connection has been closed,access forbidden");
	}

	public boolean isAutoCommitChanged() {
		return autoCommitChanged;
	}

	public boolean isTransactionLevlChanged() {
		return transactionLevlChanged;
	}

	protected void updateLastActivityTime() throws SQLException {
		this.checkClosed();
		this.pooledConnection.updateLastActivityTime();
	}

	public void setAutoCommit(boolean autoCommit) throws SQLException {
		this.checkClosed();
		this.autoCommitValue=autoCommit;
		this.delegate.setAutoCommit(autoCommit);
		this.autoCommitChanged=(autoCommit != pooledConnection.isAutoCommit());
	}

	public boolean isAutoCommitValue() {
		return autoCommitValue;
	}

	public void setTransactionIsolation(int level) throws SQLException {
		this.checkClosed();
		this.delegate.setTransactionIsolation(level);
		this.transactionLevlChanged=(level != pooledConnection.getTransactionIsolationLevl());
	}

	protected boolean isSupportStateCache() {
		return pooledConnection.isSupportStateCache();
	}

	void setConnectionDataToNull() {
		this.isClosed = true;
		this.delegate = null;
		this.connectionPool = null;
		this.pooledConnection = null;
	}

	public void close() throws SQLException {
		if (!this.isClosed) {
			this.connectionPool.releasePooledConnection(this.pooledConnection);
			this.setConnectionDataToNull();
		} else {
			throw new SQLException("Connection has been closed");
		}
	}
}
