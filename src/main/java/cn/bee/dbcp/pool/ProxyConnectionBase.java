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

import java.sql.Connection;
import java.sql.SQLException;
import cn.bee.dbcp.BeeDataSourceConfig;
import cn.bee.dbcp.pool.util.ConnectionUtil;

/**
 * physical connection wrapper
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public abstract class ProxyConnectionBase implements Connection{
	protected Connection delegate;
	private PooledConnection pooledConn;
	private volatile boolean isClosed=false;
	
	protected boolean stmCacheInd;
	protected StatementCache stmCache;
	private BeeDataSourceConfig poolConfig;
	private static final SQLException ClosedException = new SQLException("Connection has been closed");
	
	public ProxyConnectionBase(PooledConnection pooledConn) {
		this.pooledConn=pooledConn;
		this.delegate=pooledConn.connection;
		this.poolConfig=pooledConn.poolConfig;
		stmCache=pooledConn.stmCache;
		stmCacheInd=pooledConn.stmCacheInd;
	}
	
	protected void checkClose() throws SQLException {
		if(isClosed)throw ClosedException;
	}
	protected void updateAccessTime() throws SQLException {
		pooledConn.updateAccessTime();
	}
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		checkClose();
		delegate.setAutoCommit(autoCommit);
		updateAccessTime();
		pooledConn.setCurAutoCommit(autoCommit);
		pooledConn.setChangedInd(PooledConnection.Pos_AutoCommitInd,autoCommit!=poolConfig.isDefaultAutoCommit()); 
	}
	public void setTransactionIsolation(int level) throws SQLException {
		checkClose();
		delegate.setTransactionIsolation(level);
		updateAccessTime();
		pooledConn.setChangedInd(PooledConnection.Pos_TransactionIsolationInd,level!=poolConfig.getDefaultTransactionIsolation());
	}
	public void setReadOnly(boolean readOnly) throws SQLException {
		checkClose();
		delegate.setReadOnly(readOnly);
		updateAccessTime();
		pooledConn.setChangedInd(PooledConnection.Pos_ReadOnlyInd,readOnly!=poolConfig.isDefaultReadOnly());
	}
	public void setCatalog(String catalog) throws SQLException {
		checkClose();
		delegate.setCatalog(catalog);
		updateAccessTime();
		pooledConn.setChangedInd(PooledConnection.Pos_CatalogInd,!ConnectionUtil.equals(catalog, poolConfig.getDefaultCatalog()));
	}
	void setConnectionDataToNull() {
		isClosed=true;
		delegate=null;
		pooledConn=null;
		stmCache=null;
		poolConfig=null;
	}
	public void close() throws SQLException {
		try{
			isClosed = true;
			pooledConn.returnToPoolBySelf();
		}finally{
			setConnectionDataToNull();
		}
	}
}
