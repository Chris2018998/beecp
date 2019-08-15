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
	
	private boolean autoCommitVal=true;
	private boolean readOnlValChanged=false;
	private boolean catalogValChanged=false;
	private boolean autoCommitChanged=false;
	private boolean transactionLevlChanged=false;
	private BeeDataSourceConfig poolConfig;
	public ProxyConnection(PooledConnection pooledConn) {
		this.pooledConn=pooledConn;
		this.delegate=pooledConn.getPhisicConnection();
		this.poolConfig=pooledConn.poolConfig;
		autoCommitVal=poolConfig.isDefaultAutoCommit();
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
	
	public boolean isReadOnlValChanged() {
		return readOnlValChanged;
	}
	public boolean isAutoCommitChanged() {
		return autoCommitChanged;
	}
	public boolean getAutoCommitValue() {
		return autoCommitVal;
	}
	public boolean isTransactionLevlChanged() {
		return transactionLevlChanged;
	}
	public boolean isCatalogValChanged() {
		return catalogValChanged;
	}
	public void setCatalog(String catalog) throws SQLException {
		updateLastActivityTime();
		delegate.setCatalog(catalog);
		catalogValChanged=!poolConfig.getCatalog().equals(catalog);
	}
	
	public void setReadOnly(boolean readOnly) throws SQLException {
		updateLastActivityTime();
		delegate.setReadOnly(readOnly);
		readOnlValChanged=(readOnly!=poolConfig.isReadOnly());
	}
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		this.updateLastActivityTime();
		this.autoCommitVal=autoCommit;
		this.delegate.setAutoCommit(autoCommit);
		this.autoCommitChanged=(autoCommit != poolConfig.isDefaultAutoCommit());
	}
	public void setTransactionIsolation(int level) throws SQLException {
		updateLastActivityTime();
		delegate.setTransactionIsolation(level);
		transactionLevlChanged = (level!=poolConfig.getDefaultTransactionIsolation());
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
