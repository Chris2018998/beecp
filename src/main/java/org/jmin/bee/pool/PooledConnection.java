/*
 * Copyright Chris2018998
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
package org.jmin.bee.pool;
import static java.lang.System.currentTimeMillis;
import static org.jmin.bee.pool.PoolObjectsState.CONNECTION_IDLE;
import static org.jmin.bee.pool.util.ConnectionUtil.oclose;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import org.jmin.bee.BeeDataSourceConfig;

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
	//pool info
	private BeeDataSourceConfig poolConfig;

	private final static AtomicIntegerFieldUpdater<PooledConnection> updater = AtomicIntegerFieldUpdater.newUpdater(PooledConnection.class,"state");
	
	public PooledConnection(Connection connection, ConnectionPool connPool) {
		this(connection, 16, connPool);
	}
	public PooledConnection(Connection phConn, int stCacheSize, ConnectionPool connPool) {
		pool = connPool;
		connection= phConn;
		state =CONNECTION_IDLE;
		poolConfig=connPool.poolConfig;
	    mapCache = new StatementCache((stCacheSize<=0)?16:stCacheSize);
		updateLastActivityTime();
	}
	public StatementCache getStatementCache() {
		return mapCache;
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

	private void resetConnectionBeforeRelease() {
		try {
			if (proxyConnection != null) {
				boolean isAutoCommit=connection.getAutoCommit();
				
				if(isAutoCommit){
					if(poolConfig.isCommitOnReturn()){
						connection.commit();
					}else if(poolConfig.isRollbackOnReturn()){
						connection.rollback();
					}
				}
				if(isAutoCommit!=poolConfig.isDefaultAutoCommit()){
					connection.setAutoCommit(poolConfig.isDefaultAutoCommit());
				}
				if(connection.getTransactionIsolation()!=poolConfig.getDefaultTransactionIsolation()){
					connection.setTransactionIsolation(poolConfig.getDefaultTransactionIsolation());
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
		resetConnectionBeforeRelease();
		bindProxyConnection(null);
		
		mapCache.clear();
		oclose(connection);
	}
    void returnToPoolBySelf() throws SQLException {
		resetConnectionBeforeRelease();
		bindProxyConnection(null);
		pool.release(this,false);
	} 
}