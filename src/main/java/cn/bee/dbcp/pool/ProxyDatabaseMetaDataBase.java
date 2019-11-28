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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * DatabaseMetaDataBase wrapper
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public abstract class ProxyDatabaseMetaDataBase implements DatabaseMetaData {
	protected DatabaseMetaData delegate;
	private ProxyConnectionBase proxyConnection;
	public ProxyDatabaseMetaDataBase(DatabaseMetaData metaData,ProxyConnectionBase proxyConnection){
		this.delegate=metaData; 
		this.proxyConnection=proxyConnection;
	}
	public Connection getConnection() throws SQLException{
		return proxyConnection;
	}
	protected void checkClose() throws SQLException {
		proxyConnection.checkClose();
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
}
