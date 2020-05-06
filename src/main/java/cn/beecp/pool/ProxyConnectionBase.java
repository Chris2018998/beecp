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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.concurrent.Executor;

import static cn.beecp.pool.PoolExceptionList.AutoCommitChangeForbiddennException;
import static cn.beecp.pool.PoolExceptionList.ConnectionClosedException;
import static cn.beecp.util.BeecpUtil.equalsText;
import static java.lang.System.currentTimeMillis;

/**
 * raw connection wrapper
 * 
 * @author Chris.Liao
 * @version 1.0
 */
abstract class ProxyConnectionBase implements Connection{
	private boolean isClosed;
	protected Connection delegate;
	protected PooledConnection pConn;//called by subclass to update time

	private final static int Pos_AutoCommitInd=0;
	private final static int Pos_TransactionIsolationInd=1;
	private final static int Pos_ReadOnlyInd=2;
	private final static int Pos_CatalogInd=3;
	private final static int Pos_SchemaInd=4;
	private final static int Pos_NetworkTimeoutInd=5;

	public ProxyConnectionBase(PooledConnection pConn) {
		this.pConn=pConn;
		delegate=pConn.rawConn;
		pConn.proxyConn=this;
	}
	void setAsClosed(){
		isClosed=true;
	}
	protected void checkClose() throws SQLException {
		if(isClosed)throw ConnectionClosedException;
	}
	public void close() throws SQLException {
		this.checkClose();
		isClosed = true;
		pConn.returnToPoolBySelf();
	}

	public void setAutoCommit(boolean autoCommit) throws SQLException {
		checkClose();
		if(!pConn.curAutoCommit && pConn.commitDirtyInd)
		  throw AutoCommitChangeForbiddennException;
		
		delegate.setAutoCommit(autoCommit);
		pConn.curAutoCommit = autoCommit;
		pConn.setChangedInd(Pos_AutoCommitInd,autoCommit!=pConn.defaultAutoCommit);
		if(autoCommit)pConn.commitDirtyInd=false;
	}
	public void setTransactionIsolation(int level) throws SQLException {
		checkClose();
		delegate.setTransactionIsolation(level);
		pConn.setChangedInd(Pos_TransactionIsolationInd,level!=pConn.defaultTransactionIsolationCode);
	}
	public void setReadOnly(boolean readOnly) throws SQLException {
		checkClose();
		delegate.setReadOnly(readOnly);
		pConn.setChangedInd(Pos_ReadOnlyInd,readOnly!=pConn.defaultReadOnly);
	}
	public void setCatalog(String catalog) throws SQLException {
		checkClose();
		delegate.setCatalog(catalog);
		pConn.setChangedInd(Pos_CatalogInd,!equalsText(catalog, pConn.defaultCatalog));
	}
	public boolean isValid(int timeout) throws SQLException {
		checkClose();
		if (pConn.isSupportValidTest()){
			return delegate.isValid(timeout);
		}else{
			throw PoolExceptionList.FeatureNotSupportedException;
		}
	}
	//for JDK1.7 begin
	public void setSchema(String schema) throws SQLException {
		checkClose();
		if (pConn.isSupportSchema()){
			delegate.setSchema(schema);
			pConn.setChangedInd(Pos_SchemaInd, !equalsText(schema, pConn.defaultSchema));
		}else{
			throw PoolExceptionList.FeatureNotSupportedException;
		}
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
	public int getNetworkTimeout() throws SQLException{
		checkClose();
		if (pConn.isSupportNetworkTimeout()) {
			return 	delegate.getNetworkTimeout();
		}else{
			throw PoolExceptionList.FeatureNotSupportedException;
		}
	}
	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		checkClose();
		if (pConn.isSupportNetworkTimeout()) {
			delegate.setNetworkTimeout(executor, milliseconds);
			pConn.setChangedInd(Pos_NetworkTimeoutInd, true);
		}else{
			throw PoolExceptionList.FeatureNotSupportedException;
		}
	}
	//for JDK1.7 end

	public void commit() throws SQLException{
		checkClose();
		delegate.commit();
		pConn.lastAccessTime=currentTimeMillis();
		pConn.commitDirtyInd=false;
	}
	public void rollback() throws SQLException{
		checkClose();
		delegate.rollback();
		pConn.lastAccessTime=currentTimeMillis();
		pConn.commitDirtyInd=false;
	}
	public void rollback(Savepoint savepoint) throws SQLException{
		checkClose();
		delegate.rollback(savepoint);
		pConn.lastAccessTime=currentTimeMillis();
		pConn.commitDirtyInd=false;
	}
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		checkClose();
		return iface.isInstance(delegate);
	}
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException{
	  checkClose();
	  if (iface.isInstance(delegate)) {
         return (T)this;
      }else {
    	  throw new SQLException("Wrapped object is not an instance of " + iface);
      } 
	}
}
