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

import static cn.bee.dbcp.pool.util.ConnectionUtil.oclose;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * ProxyBaseStatement
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public class ProxyStatementBase {
	protected volatile boolean isClosed;
	protected boolean cacheInd;
	protected Statement delegate;
	protected ProxyConnectionBase proxyConnection;
	
	public ProxyStatementBase(Statement delegate, ProxyConnectionBase proxyConnection, boolean cacheInd) {
		this.delegate = delegate;
		this.proxyConnection = proxyConnection;
		this.cacheInd = cacheInd;
	}
	protected void checkClose() throws SQLException {
		if(isClosed)throw new SQLException("Statement has been closed,access forbidden");
		proxyConnection.checkClose();
	}
	public void close() throws SQLException {
		try{
			checkClose();
		}finally{
			this.isClosed = true;
			if(!cacheInd)oclose(delegate);
			this.delegate = null;
			this.proxyConnection =null;
		}
	}
}
