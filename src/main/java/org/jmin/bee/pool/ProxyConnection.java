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

import java.sql.Connection;
import java.sql.SQLException;

/**
 * physical connection wrapper
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public abstract class ProxyConnection implements Connection {
	private boolean isClosed;
	protected Connection delegate;
	protected PooledConnection pooledConnection;
	private boolean autoCommitValue = false;
	private boolean autoCommitChanged = false;
	private boolean transactionLevlChanged = false;

	public ProxyConnection(PooledConnection pooledConnection) {
		this.pooledConnection = pooledConnection;
		this.delegate = pooledConnection.getPhisicConnection();
		this.autoCommitValue = pooledConnection.isAutoCommit();
	}

	public boolean isClosed() {
		return isClosed;
	}

	public boolean isUseStatementCache() {
		return pooledConnection.isUseStatementCache();
	}

	public boolean isAutoCommitChanged() {
		return autoCommitChanged;
	}

	public boolean isTransactionLevlChanged() {
		return transactionLevlChanged;
	}

	protected void updateLastActivityTime() throws SQLException {
		if (isClosed)
			throw new SQLException("Connection has been closed");
		else
			this.pooledConnection.updateLastActivityTime();
	}

	public void setAutoCommit(boolean autoCommit) throws SQLException {
		this.updateLastActivityTime();
		this.autoCommitValue = autoCommit;
		this.delegate.setAutoCommit(autoCommit);
		this.autoCommitChanged = (autoCommit != pooledConnection.isAutoCommit());
	}

	public boolean isAutoCommitValue() {
		return autoCommitValue;
	}

	public void setTransactionIsolation(int level) throws SQLException {
		this.updateLastActivityTime();
		this.delegate.setTransactionIsolation(level);
		this.transactionLevlChanged = (level != pooledConnection.getTransactionIsolationLevl());
	}

	void setConnectionDataToNull() {
		this.isClosed = true;
		this.delegate = null;
		this.pooledConnection = null;
	}

	public void close() throws SQLException {
		if (this.isClosed) {
			throw new SQLException("Connection has been closed");
		} else {
			this.pooledConnection.returnToPoolBySelf();
		}
	}
}
