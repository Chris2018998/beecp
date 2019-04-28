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
import java.util.concurrent.atomic.AtomicInteger;

import org.jmin.bee.pool.util.SystemClock;

/**
 * JDBC connection wrapper
 *
 * @author Chris.Liao
 * @version 1.0
 */

public final class PooledConnection {
	// state
	private AtomicInteger state;
	// last activity time
	private long lastActiveTime;
	// physical connection
	private Connection connection;
	// PreparedStatement cache
	private StatementCache statementCache;
	// physical connection wrapper
	private ProxyConnection proxyConnection;
	// autoCommit
	private boolean autoCommit;
	// transaction level
	private int transactionIsolationLevlOrig = Connection.TRANSACTION_READ_COMMITTED;
	// related pool
	private ConnectionPool connectionPool;
	//isSurpportSetQueryTimeout
	private boolean isSurpportSetQueryTimeout=true;
	private final SystemClock systemClock=SystemClock.clock;
	public PooledConnection(Connection connection, ConnectionPool connectionPool) {
		this(connection, 10, connectionPool);
	}

	public PooledConnection(Connection connection, int statementCacheSize, ConnectionPool connectionPool) {
		this.connection = connection;
		this.state = new AtomicInteger(PooledConnectionState.IDLE);
		this.statementCache = new StatementCache(statementCacheSize);
		this.connectionPool = connectionPool;
		try {
			this.autoCommit = this.connection.getAutoCommit();
			this.transactionIsolationLevlOrig = this.connection.getTransactionIsolation();
		} catch (Throwable e) {}
		this.updateLastActivityTime();
	}
	
	public StatementCache getStatementCache() {
		return statementCache;
	}

	public boolean isSurpportSetQueryTimeout() {
		return isSurpportSetQueryTimeout;
	}

	public void setSurpportSetQueryTimeout(boolean isSurpportSetQueryTimeout) {
		this.isSurpportSetQueryTimeout = isSurpportSetQueryTimeout;
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
		this.lastActiveTime = systemClock.currentTimeMillis();
	}

	public Connection getPhisicConnection() {
		return this.connection;
	}

	public ProxyConnection getProxyConnection() {
		return proxyConnection;
	}

	public void bindProxyConnection(ProxyConnection proxyConnection) {
		this.proxyConnection = proxyConnection;
	}
	
	public boolean equals(Object obj) {
		return this==obj;
	}

	public String toString() {
		return connection.toString();
	}

	public int getConnectionState() {
		return this.state.get();
	}

	public void setConnectionState(int update) {
		this.state.set(update);
	}

	public boolean compareAndSet(int expect, int update) {
		return this.state.compareAndSet(expect, update);
	}

	public void resetConnectionAfterRelease() throws SQLException {
		if (!proxyConnection.isAutoCommitValue()) {
			this.connection.rollback();
		}
		if (proxyConnection.isAutoCommitChanged()) {
			this.connection.setAutoCommit(autoCommit);
		}
		if (proxyConnection.isTransactionLevlChanged()) {
			this.connection.setTransactionIsolation(this.transactionIsolationLevlOrig);
		}
	}
	
	public void removeFromPool() {
		if (this.proxyConnection != null) {
			proxyConnection.setConnectionDataToNull();
			proxyConnection = null;
		}

		this.statementCache.clearAllStatement();
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

	void returnToPoolBySelf() throws SQLException {
		if (this.state.get() == PooledConnectionState.USING) {
			if (proxyConnection != null) {
				this.resetConnectionAfterRelease();
				this.proxyConnection.setConnectionDataToNull();
			}
			
			this.bindProxyConnection(null);
			this.updateLastActivityTime();
			this.connectionPool.releasePooledConnection(this);
		}
	}
}