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
	BeeDataSourceConfig poolConfig;
	private boolean curAutoCommit=true;
	//changed indicator
	private boolean[] changedInds=new boolean[4]; //0:autoCommit,1:transactionIsolation,2:readOnly,3:catalog
	private short changedFieldsts=Short.parseShort("0000",2); //pos:last ---> head;0:unchanged,1:changed
	public static short Pos_AutoCommitInd=0;
	public static short Pos_TransactionIsolationInd=1;
	public static short Pos_ReadOnlyInd=2;
	public static short Pos_CatalogInd=3;
	
	private boolean statementCacheInd;
	private final static AtomicIntegerFieldUpdater<PooledConnection> updater = AtomicIntegerFieldUpdater.newUpdater(PooledConnection.class,"state");
	
	public PooledConnection(Connection phConn, ConnectionPool connPool) {
		pool = connPool;
		connection= phConn;
		state =CONNECTION_IDLE;
		poolConfig=connPool.poolConfig;
		curAutoCommit=poolConfig.isDefaultAutoCommit();
		int stCacheSize=poolConfig.getPreparedStatementCacheSize();
		statementCacheInd=connPool.isStatementCacheInd();
		if(statementCacheInd)mapCache = new StatementCache(stCacheSize);
		updateLastActivityTime();
	}
	public boolean isStatementCacheInd() {
		return statementCacheInd;
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
		updateLastActivityTime();
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
	
	int getChangedInd(int pos){
		return (changedFieldsts>>pos)&1 ;
	}
    void setChangedInd(int pos,boolean changed){
    	int bitV=changed?1:0;
    	changedFieldsts=changedFieldsts^=(changedFieldsts&(1<<pos))^(bitV<<pos);
    	changedInds[pos]=changed;
    }
	void setCurAutoCommit(boolean curAutoCommit) {
		this.curAutoCommit = curAutoCommit;
	}
	private void resetConnectionBeforeRelease() {
		if (changedFieldsts > 0) {// exists field changed
			if (changedInds[0]) {
				try {
					if(!curAutoCommit)connection.rollback();
					connection.setAutoCommit(poolConfig.isDefaultAutoCommit());
					changedInds[0]=false;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

			if (changedInds[1]) {
				try {
					connection.setTransactionIsolation(poolConfig.getDefaultTransactionIsolation());
					changedInds[1] = false;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

			if (changedInds[2]) {
				try {
					connection.setReadOnly(poolConfig.isReadOnly());
					changedInds[2] = false;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

			if (changedInds[3]) {
				try {
					connection.setCatalog(poolConfig.getCatalog());
					changedInds[3] = false;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			changedFieldsts=0;
		}
	}
	
	//called for pool
	void closePhysicalConnection() {
		try{
			resetConnectionBeforeRelease();
			mapCache.clear();
			oclose(connection);
		}finally{
			if(proxyConnection!=null){
				proxyConnection.setConnectionDataToNull();
				proxyConnection = null;
			}
		}
	}
    void returnToPoolBySelf() throws SQLException {
    	try{
	    	updateLastActivityTime();
			resetConnectionBeforeRelease();
			pool.release(this,false);
    	}finally{
    		proxyConnection = null;
		}
	} 
}