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
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import org.jmin.bee.pool.util.ConnectionUtil;
import org.jmin.bee.pool.util.SystemClock;

/**
 * JDBC connection wrapper
 *
 * @author Chris.Liao
 * @version 1.0
 */

public final class PooledConnection {
	// state
	private volatile int state;
	// last activity time
	private volatile long lastActiveTime;
	// related pool
	private ConnectionPool pool;
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
	private final static AtomicIntegerFieldUpdater<PooledConnection> updater = AtomicIntegerFieldUpdater.newUpdater(PooledConnection.class,"state");
	
	PooledConnection() {}
	public PooledConnection(Connection connection, ConnectionPool connectionPool) {
		this(connection, 10, connectionPool);
	}
	public PooledConnection(Connection phConn, int stCacheSize, ConnectionPool connpool) {
		pool = connpool;
		connection= phConn;
		state = PooledConnectionState.IDLE;
	    statementCache = new StatementCache((stCacheSize<=0)?16:stCacheSize);
	    
		try {
			autoCommit = connection.getAutoCommit();
			transactionIsolationLevlOrig = connection.getTransactionIsolation();
		} catch (Throwable e) {}
		updateLastActivityTime();
	}
	
	public StatementCache getStatementCache() {
		return statementCache;
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
		lastActiveTime = SystemClock.currentTimeMillis();
	}

	public Connection getPhisicConnection() {
		return connection;
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
		return state;
	}
	public void setConnectionState(int update) {
		state=update;
	}
	public boolean compareAndSet(int expect, int update) {
		return updater.compareAndSet(this, expect, update);//state.compareAndSet(expect, update);
	}
	public void resetConnectionAfterRelease() throws SQLException {
		if (!proxyConnection.isAutoCommitValue()) {
			connection.rollback();
		}
		if (proxyConnection.isAutoCommitChanged()) {
			connection.setAutoCommit(autoCommit);
		}
		if (proxyConnection.isTransactionLevlChanged()) {
			connection.setTransactionIsolation(this.transactionIsolationLevlOrig);
		}
	}
	
	//called for pool
	public void closePhysicalConnection() {
		if (proxyConnection != null) {
			proxyConnection.setConnectionDataToNull();
			proxyConnection = null;
		}

		this.statementCache.clear();
		try {
			if (!connection.getAutoCommit())
				connection.rollback();
		} catch (Throwable e) {
		}
		ConnectionUtil.close(connection);
	}

	void returnToPoolBySelf() throws SQLException {
		if (state == PooledConnectionState.USING) {
			if (proxyConnection != null) {
				resetConnectionAfterRelease();
				proxyConnection.setConnectionDataToNull();
			}
			
			bindProxyConnection(null);
			pool.release(this);
		}
	}
}