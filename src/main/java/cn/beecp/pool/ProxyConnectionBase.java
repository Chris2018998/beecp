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
package cn.beecp.pool;

import cn.beecp.BeeDataSourceConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.concurrent.Executor;

import static cn.beecp.pool.PoolExceptionList.AutoCommitChangeForbiddennException;
import static cn.beecp.pool.PoolExceptionList.ConnectionClosedException;
import static cn.beecp.util.BeecpUtil.equalsText;
/**
 * raw connection wrapper
 * 
 * @author Chris.Liao
 * @version 1.0
 */
abstract class ProxyConnectionBase implements Connection{
	private boolean isClosed;
	protected Connection delegate;
	protected PooledConnection pConn;//called by subClsss to update time
	private BeeDataSourceConfig pConfig;
	
	public ProxyConnectionBase(PooledConnection pConn) {
		this.pConn=pConn;
		delegate=pConn.rawConn;
		pConfig=pConn.pConfig;
	}
	void setAsClosed(){
		isClosed=true;
	}
	protected final void checkClose() throws SQLException {
		if(isClosed)throw ConnectionClosedException;
	}
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		checkClose();
		if(!pConn.curAutoCommit && pConn.commitDirtyInd)
		  throw AutoCommitChangeForbiddennException;
		
		delegate.setAutoCommit(autoCommit);
		pConn.setCurAutoCommit(autoCommit);
		pConn.setChangedInd(PooledConnection.Pos_AutoCommitInd,autoCommit!=pConfig.isDefaultAutoCommit());
		if(autoCommit)pConn.commitDirtyInd=false;
	}
	public void setTransactionIsolation(int level) throws SQLException {
		checkClose();
		delegate.setTransactionIsolation(level);
		pConn.setChangedInd(PooledConnection.Pos_TransactionIsolationInd,level!=pConfig.getDefaultTransactionIsolationCode());
	}
	public void setReadOnly(boolean readOnly) throws SQLException {
		checkClose();
		delegate.setReadOnly(readOnly);
		pConn.setChangedInd(PooledConnection.Pos_ReadOnlyInd,readOnly!=pConfig.isDefaultReadOnly());
	}
	public void setCatalog(String catalog) throws SQLException {
		checkClose();
		delegate.setCatalog(catalog);
		pConn.setChangedInd(PooledConnection.Pos_CatalogInd,!equalsText(catalog, pConfig.getDefaultCatalog()));
	}
	//for JDK1.7 begin
	public void setSchema(String schema) throws SQLException {
		checkClose();
		delegate.setSchema(schema);
		pConn.setChangedInd(PooledConnection.Pos_SchemaInd,!equalsText(schema,pConfig.getDefaultSchema()));
	}
	public void abort(Executor executor) throws SQLException{
		checkClose();
		if(executor == null) throw new SQLException("executor can't be null");
		executor.execute(new Runnable() {
			public void run() {
				try {
					ProxyConnectionBase.this.close();
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		checkClose();
		delegate.setNetworkTimeout(executor,milliseconds);
		pConn.setChangedInd(PooledConnection.Pos_NetworkTimeoutInd,true);
	}
	//for JDK1.7 end

	public void commit() throws SQLException{
		checkClose();
		delegate.commit();
		pConn.updateAccessTime();
		pConn.commitDirtyInd=false;
	}
	public void rollback() throws SQLException{
		checkClose();
		delegate.rollback();
		pConn.updateAccessTime();
		pConn.commitDirtyInd=false;
	}
	public void rollback(Savepoint savepoint) throws SQLException{
		checkClose();
		delegate.rollback(savepoint);
		pConn.updateAccessTime();
		pConn.commitDirtyInd=false;
	}
	public final boolean isWrapperFor(Class<?> iface) throws SQLException {
		checkClose();
		return iface.isInstance(delegate);
	}
	@SuppressWarnings("unchecked")
	public final <T> T unwrap(Class<T> iface) throws SQLException{
	  checkClose();
	  if (iface.isInstance(delegate)) {
         return (T)this;
      }else {
    	  throw new SQLException("Wrapped object is not an instance of " + iface);
      } 
	}
	public final void close() throws SQLException {
		this.checkClose();
		isClosed = true;
		pConn.returnToPoolBySelf();
	}
}
