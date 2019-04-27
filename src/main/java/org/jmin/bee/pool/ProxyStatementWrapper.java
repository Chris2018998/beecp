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
import java.sql.Statement;

import org.jmin.bee.pool.util.ConnectionUtil;

/**
 * ProxyBaseStatement
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public class ProxyStatementWrapper {
	protected boolean isClosed;
	protected Statement delegate;
	protected ProxyConnection proxyConnection;
	protected boolean isUseStatementCache;

	public ProxyStatementWrapper(Statement delegate, ProxyConnection proxyConnection, boolean isUseStatementCache) {
		this.delegate = delegate;
		this.proxyConnection = proxyConnection;
		this.isUseStatementCache = isUseStatementCache;
		this.isClosed = false;
	}

	public boolean isClosed() {
		return isClosed;
	}

	protected void updateLastActivityTime() throws SQLException {
		if (isClosed)
			throw new SQLException("Statement has been closed,access forbidden");
		this.proxyConnection.updateLastActivityTime();
	}

	public void close() throws SQLException {
		if (this.isClosed) {
			throw new SQLException("Statement has been closed");
		} else {
			this.isClosed = true;
			if (!this.isUseStatementCache) {
				ConnectionUtil.close(delegate);
				this.delegate = null;
				this.proxyConnection = null;
			}
		}
	}
}
