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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jmin.bee.impl.util.ConnectionUtil;

/**
 * JDBC connection wrapper
 *
 * @author Chris liao
 * @version 1.0
 */

public class PooledConnection {

	// state
	private AtomicInteger state;
	// last activity time
	private long lastActiveTime;
	// physical connection
	private Connection connection;
	// support PreparedStatement cache indicator
	private boolean supportStateCache;
	// PreparedStatement cache
	private StatementCache statementCache;
	// physical connection wrapper
	private ProxyConnection proxyConnection;
	// autoCommit
	private boolean autoCommit;
	// transaction level
	private int transactionIsolationLevlOrig = Connection.TRANSACTION_READ_COMMITTED;

	public PooledConnection(Connection connection) {
		this(connection, 20);
	}

	public PooledConnection(Connection connection, int statementCacheSize) {
		this.connection = connection;
		this.state = new AtomicInteger(PooledConnectionState.IDLE);
		this.supportStateCache = (statementCacheSize > 0);
		this.statementCache = new StatementCache(statementCacheSize);
		this.updateLastActivityTime();
		try {
			this.autoCommit=this.connection.getAutoCommit();
			this.transactionIsolationLevlOrig = this.connection.getTransactionIsolation();
		} catch (Throwable e) {}
	}
	
	public boolean isAutoCommit() {
		return autoCommit;
	}

	public int getTransactionIsolationLevl() {
		return transactionIsolationLevlOrig;
	}

	public long getLastActiveTime() {
		return lastActiveTime;
	}

	public void updateLastActivityTime() {
		this.lastActiveTime = ConnectionUtil.getTimeMillis();
	}

	public Connection getPhisicConnection() {
		return this.connection;
	}

	public boolean isSupportStateCache() {
		return this.supportStateCache;
	}

	public PreparedStatement getStatement(Object key) {
		return (this.statementCache.size() > 0) ? this.statementCache.get(key) : null;
	}

	public void putStatement(Object key, PreparedStatement value) {
		if(this.supportStateCache)
			this.statementCache.put(key, value);
	}
	
	public void renoveStatement(Object key, PreparedStatement value) {
		if(this.supportStateCache)
			this.statementCache.remove(key);
	}
	
	public ProxyConnection getProxyConnection() {
		return proxyConnection;
	}

	public void setProxyConnection(ProxyConnection proxyConnection) {
		this.proxyConnection = proxyConnection;
	}

	public int hashCode() {
		return connection.hashCode();
	}

	public int getState() {
		return this.state.get();
	}

	public void setState(int update) {
		this.state.set(update);
	}

	public boolean compareAndSet(int expect, int update) {
		return this.state.compareAndSet(expect, update);
	}

	public void resetConnectionAfterRelease() throws SQLException {
		if(!proxyConnection.isAutoCommitValue()) {
			this.connection.rollback();
		}
		if (proxyConnection.isAutoCommitChanged()) {
			this.connection.setAutoCommit(autoCommit);
		}
		if (proxyConnection.isTransactionLevlChanged()) {
			this.connection.setTransactionIsolation(this.transactionIsolationLevlOrig);
		}
	}

	public boolean equals(Object obj) {
		if (obj instanceof PooledConnection) {
			PooledConnection oPool = (PooledConnection) obj;
			return this.connection == oPool.connection;
		} else {
			return false;
		}
	}

	public void closePhisicConnection() {
		if (this.proxyConnection != null) {
			proxyConnection.setConnectionDataToNull();
			proxyConnection = null;
		}

		this.statementCache.clear();
		try {
			if (!connection.getAutoCommit())
				connection.rollback();
		} catch (Throwable e) {
		}

		try {
			this.connection.close();
		} catch (Throwable e) {
		}
	}
}