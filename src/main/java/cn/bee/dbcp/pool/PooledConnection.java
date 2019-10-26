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
package cn.bee.dbcp.pool;

import static cn.bee.dbcp.pool.PoolObjectsState.CONNECTION_IDLE;
import static cn.bee.dbcp.pool.util.ConnectionUtil.oclose;
import static java.lang.System.currentTimeMillis;

import java.sql.Connection;
import java.sql.SQLException;

import cn.bee.dbcp.BeeDataSourceConfig;
import cn.bee.dbcp.pool.util.ConnectionUtil;

/**
 * JDBC connection wrapper
 *
 * @author Chris.Liao
 * @version 1.0
 */
final class PooledConnection{
	volatile int state;
	boolean stmCacheInd;
	StatementCache stmCache;
	Connection connection;
	BeeDataSourceConfig poolConfig;
	
	private ConnectionPool pool;
	private volatile long lastAccessTime;
	private ProxyConnectionBase proxyConnection;
	private boolean curAutoCommit=true;
	
	//changed indicator
	private boolean[] changedInds=new boolean[4]; //0:autoCommit,1:transactionIsolation,2:readOnly,3:catalog
	private short changedFieldsts=Short.parseShort("0000",2); //pos:last ---> head;0:unchanged,1:changed
	public static short Pos_AutoCommitInd=0;
	public static short Pos_TransactionIsolationInd=1;
	public static short Pos_ReadOnlyInd=2;
	public static short Pos_CatalogInd=3;
	
	public PooledConnection(Connection phConn, ConnectionPool connPool) {
		pool = connPool;
		connection= phConn;
		
		state=CONNECTION_IDLE;
		poolConfig=connPool.poolConfig;
		curAutoCommit=poolConfig.isDefaultAutoCommit();
		int stCacheSize=poolConfig.getPreparedStatementCacheSize();
		stmCacheInd=connPool.isStatementCacheInd();
		if(stmCacheInd)stmCache = new StatementCache(stCacheSize);
		setDefault();
		updateAccessTime();
	}

	private void setDefault(){
		try {
			connection.setAutoCommit(poolConfig.isDefaultAutoCommit());
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			connection.setTransactionIsolation(poolConfig.getDefaultTransactionIsolation());
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			connection.setReadOnly(poolConfig.isDefaultReadOnly());
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			if(!ConnectionUtil.isNull(poolConfig.getDefaultCatalog()))
		      connection.setCatalog(poolConfig.getDefaultCatalog());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public long getLastAccessTime() {
		return lastAccessTime;
	}
	public void updateAccessTime() {
	  lastAccessTime=currentTimeMillis();
	}
	public Connection getPhisicConnection() {
		return connection;
	}
	public ProxyConnectionBase getProxyConnection() {
		return proxyConnection;
	}
	public void bindProxyConnection(ProxyConnectionBase proxyConnection) {
		this.proxyConnection = proxyConnection;
	}
	public boolean equals(Object obj) {
		return this==obj;
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
					updateAccessTime();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

			if (changedInds[1]) {
				try {
					connection.setTransactionIsolation(poolConfig.getDefaultTransactionIsolation());
					changedInds[1] = false;
					updateAccessTime();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

			if (changedInds[2]) {
				try {
					connection.setReadOnly(poolConfig.isDefaultReadOnly());
					changedInds[2] = false;
					updateAccessTime();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

			if (changedInds[3]) {
				try {
					connection.setCatalog(poolConfig.getDefaultCatalog());
					changedInds[3] = false;
					updateAccessTime();
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
			stmCache.clear();
			stmCache=null;
			poolConfig=null;
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
			resetConnectionBeforeRelease();
			pool.release(this,true);
    	}finally{
    		proxyConnection = null;
		}
	} 
}