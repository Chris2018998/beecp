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

import static org.jmin.bee.pool.util.ConnectionUtil.oclose;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Statement resultset proxy super class
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public abstract class ProxyResultSetBase implements ResultSet {
	private volatile boolean isClosed;
	protected ResultSet delegate;
	private ProxyStatementBase proxyStatement;
	private PooledConnection pooledConn;
	
	public ProxyResultSetBase(ResultSet delegate,ProxyStatementBase proxyStatement) {
		this.delegate = delegate;
		this.proxyStatement = proxyStatement;
		pooledConn=proxyStatement.pooledConn;
	}
	public boolean isClosed()throws SQLException{
		return isClosed;
	}
	protected void checkClose() throws SQLException {
		if (isClosed)throw new SQLException("ResultSet has been closed,access forbidden");
		proxyStatement.checkClose();
	}
	protected void updateAccessTime() throws SQLException {
		pooledConn.updateAccessTime();
	}
	public Statement getStatement() throws SQLException{
		checkClose();
		return (Statement)proxyStatement;
	}
	public void close() throws SQLException {
		try{
			checkClose();
		}finally{
			isClosed = true;
			oclose(delegate);
			delegate = null;
			proxyStatement=null;
			pooledConn=null;
		}
	}
}
