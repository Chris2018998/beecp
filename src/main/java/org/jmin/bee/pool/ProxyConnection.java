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

import java.sql.Connection;
import java.sql.SQLException;

import org.jmin.bee.BeeDataSourceConfig;
import org.jmin.bee.pool.util.ConnectionUtil;

/**
 * physical connection wrapper
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public abstract class ProxyConnection implements Connection {
	private boolean isClosed;
	protected Connection delegate;
	private PooledConnection pooledConn;
	private BeeDataSourceConfig poolConfig;
	public ProxyConnection(PooledConnection pooledConn) {
		this.pooledConn=pooledConn;
		this.delegate=pooledConn.getPhisicConnection();
		this.poolConfig=pooledConn.poolConfig;
	}
	public boolean isClosed() {
		return isClosed;
	}
	public PooledConnection getPooledConnection() {
		 return pooledConn;
	}
	protected StatementCache getStatementCache() {
	  return pooledConn.getStatementCache();
	}
	protected void updateLastActivityTime() throws SQLException {
		if (isClosed)throw new SQLException("Connection has been closed");
		this.pooledConn.updateLastActivityTime();
	}
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		updateLastActivityTime();
		delegate.setAutoCommit(autoCommit);
		pooledConn.setCurAutoCommit(autoCommit);
		pooledConn.setChangedInd(PooledConnection.Pos_AutoCommitInd,autoCommit!=poolConfig.isDefaultAutoCommit());
	}
	public void setTransactionIsolation(int level) throws SQLException {
		updateLastActivityTime();
		delegate.setTransactionIsolation(level);
		pooledConn.setChangedInd(PooledConnection.Pos_TransactionIsolationInd,level!=poolConfig.getDefaultTransactionIsolation());
	}
	public void setReadOnly(boolean readOnly) throws SQLException {
		updateLastActivityTime();
		delegate.setReadOnly(readOnly);
		pooledConn.setChangedInd(PooledConnection.Pos_ReadOnlyInd,readOnly!=poolConfig.isReadOnly());
	}
	public void setCatalog(String catalog) throws SQLException {
		updateLastActivityTime();
		delegate.setCatalog(catalog);
		pooledConn.setChangedInd(PooledConnection.Pos_CatalogInd,!ConnectionUtil.equals(poolConfig.getCatalog(),catalog));
	}
	
	void setConnectionDataToNull() {
		isClosed = true;
		delegate = null;
		pooledConn = null;
	}

	public void close() throws SQLException {
		updateLastActivityTime();
		pooledConn.returnToPoolBySelf();
	}
}
