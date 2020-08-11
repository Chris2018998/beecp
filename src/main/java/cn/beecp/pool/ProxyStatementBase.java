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

import java.sql.*;

import static cn.beecp.util.BeecpUtil.oclose;
import static cn.beecp.pool.PoolExceptionList.StatementClosedException;

/**
 * ProxyBaseStatement
 *
 * @author Chris.Liao
 * @version 1.0
 */
class ProxyStatementBase{
	private boolean isClosed;
	private boolean cacheInd;
	protected Statement delegate;
	protected PooledConnection pConn;//called by subclass to update time
	protected ProxyConnectionBase proxyConn;//called by subclass to check close state

	public ProxyStatementBase(Statement delegate,ProxyConnectionBase proxyConn,PooledConnection pConn,boolean cacheInd){
		this.pConn=pConn;
		this.proxyConn=proxyConn;
		this.delegate=delegate;
		this.cacheInd=cacheInd;
	}
	public Connection getConnection() throws SQLException{
		checkClosed();
		return proxyConn;
	}
	public boolean isClosed()throws SQLException{return isClosed;}
	protected void checkClosed() throws SQLException {
		if(isClosed)throw StatementClosedException;
		proxyConn.checkClosed();
	}
	public void close() throws SQLException {
		checkClosed();
		isClosed=true;
		if(!cacheInd)oclose(delegate);
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		checkClosed();
		return iface.isInstance(this);
	}
	public <T> T unwrap(Class<T> iface) throws SQLException{
		checkClosed();
		if(iface.isInstance(this))
			return (T)this;
		else
			throw new SQLException("Wrapped object is not an instance of "+iface);
	}
}
