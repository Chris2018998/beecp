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
import static java.lang.System.currentTimeMillis;
import static org.jmin.bee.pool.PoolObjectsState.CONNECTION_IDLE;
import static org.jmin.bee.pool.util.ConnectionUtil.oclose;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * JDBC connection wrapper
 *
 * @author Chris.Liao
 * @version 1.0
 */

final class PooledConnection{
	// state
	private volatile int state;
	// last activity time
	private volatile long lastActiveTime;
	// related pool
	private ConnectionPool pool;
	// physical connection
	private Connection connection;
	// Statement cache
	private StatementCache mapCache;
	// physical connection wrapper
	private ProxyConnection proxyConnection;
	// autoCommit
	private boolean autoCommit;
	// transaction level
	private int transactionIsolationLevlOrig = Connection.TRANSACTION_READ_COMMITTED;
	private final static AtomicIntegerFieldUpdater<PooledConnection> updater = AtomicIntegerFieldUpdater.newUpdater(PooledConnection.class,"state");
	
	public PooledConnection(Connection connection, ConnectionPool connPool) {
		this(connection, 16, connPool);
	}
	public PooledConnection(Connection phConn, int stCacheSize, ConnectionPool connPool) {
		pool = connPool;
		connection= phConn;
		state =CONNECTION_IDLE;
	    mapCache = new StatementCache((stCacheSize<=0)?16:stCacheSize);
		try {
			autoCommit = connection.getAutoCommit();
			transactionIsolationLevlOrig = connection.getTransactionIsolation();
		} catch (Throwable e) {}
		updateLastActivityTime();
	}
	public StatementCache getStatementCache() {
		return mapCache;
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
		lastActiveTime=currentTimeMillis();
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
	
	public int getState() {
		return state;
	}
	public void setState(int update) {
		state=update;
	}
	public boolean compareAndSetState(int expect, int update) {
		return updater.compareAndSet(this, expect, update);
	}

	private void resetConnectionAfterRelease() {
		try {
			if (proxyConnection != null) {
				if (!proxyConnection.isAutoCommitValue()) {
					connection.rollback();
				}
				if (proxyConnection.isAutoCommitChanged()) {
					connection.setAutoCommit(autoCommit);
				}
				if (proxyConnection.isTransactionLevlChanged()) {
					connection.setTransactionIsolation(this.transactionIsolationLevlOrig);
				}

				proxyConnection.setConnectionDataToNull();
				proxyConnection = null;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	//called for pool
	void closePhysicalConnection() {
		resetConnectionAfterRelease();
		bindProxyConnection(null);
		
		mapCache.clear();
		oclose(connection);
	}
    void returnToPoolBySelf() throws SQLException {
		resetConnectionAfterRelease();
		bindProxyConnection(null);
		pool.release(this,false);
	}
}