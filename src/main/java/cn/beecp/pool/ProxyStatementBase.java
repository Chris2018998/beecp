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

import static cn.beecp.pool.PoolExceptionList.StatementClosedException;
import static cn.beecp.util.BeecpUtil.oclose;

/**
 * ProxyBaseStatement
 *
 * @author Chris.Liao
 * @version 1.0
 */
class ProxyStatementBase{
	private boolean isClosed;
	private int statementType=0;
	private boolean inCacheInd=false;
	protected Statement delegate;
	protected PreparedStatement delegate1;
	protected CallableStatement delegate2;

	protected PooledConnection pConn;//called by subClsss to update time
	protected ProxyConnectionBase proxyConn;//called by subClsss to check close state

	public ProxyStatementBase(Statement delegate,ProxyConnectionBase proxyConn,PooledConnection pConn){
		this.pConn=pConn;
		this.proxyConn=proxyConn;
		this.delegate = delegate;
	}
	public ProxyStatementBase(PreparedStatement delegate,boolean inCacheInd,ProxyConnectionBase proxyConn,PooledConnection pConn){
		this(delegate,proxyConn,pConn);
		this.statementType=1;
		this.delegate1=delegate;
		this.inCacheInd=inCacheInd;
	}
	public ProxyStatementBase(CallableStatement delegate,boolean inCacheInd,ProxyConnectionBase proxyConn,PooledConnection pConn,boolean cs){
		this(delegate,proxyConn,pConn);
		this.statementType=2;
		this.delegate2=delegate;
		this.inCacheInd=inCacheInd;
	}
	public Connection getConnection() throws SQLException{
		checkClose();
		return proxyConn;
	}
	protected final void checkClose() throws SQLException {
		if(isClosed)throw StatementClosedException;
		proxyConn.checkClose();
	}
	public final void close() throws SQLException {
		checkClose();
		this.isClosed=true;
		if(!inCacheInd)
			oclose(delegate);
		this.delegate=null;
		this.delegate1=null;
		this.delegate2=null;

	}
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		checkClose();
		switch(statementType){
			case 0: return iface.isInstance(delegate);
			case 1: return iface.isInstance(delegate1);
			case 2: return iface.isInstance(delegate2);
			default:return false;
		}
	}
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException{
		checkClose();
		String message="Wrapped object is not an instance of "+iface;
		switch(statementType){
			case 0: if(iface.isInstance(delegate))return (T)this; else throw new SQLException(message);
			case 1: if(iface.isInstance(delegate1))return (T)this; else throw new SQLException(message);
			case 2: if(iface.isInstance(delegate2))return (T)this; else throw new SQLException(message);
			default:throw new SQLException(message);
		}
	}
}
