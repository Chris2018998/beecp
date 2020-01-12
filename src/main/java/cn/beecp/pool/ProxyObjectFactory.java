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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Proxy Object Factory
 *
 * @author Chris.Liao
 * @version 1.0
 */
class ProxyObjectFactory {
    // create proxy to wrap connection as result
    public final static ProxyConnectionBase createProxyConnection(PooledConnection pConn,Borrower borrower) throws SQLException {
        // borrower.setBorrowedConnection(pConn);
        // return pConn.proxyConnCurInstance=new ProxyConnection(pConn);
        throw new SQLException("Proxy classes not be generated,please execute 'ProxyClassUtil' after project compile");
    }
    // create proxy to wrap ResultSetB as result
    public final static ProxyResultSetBase createProxyResultSet(ResultSet delegate,ProxyStatementBase proxyStatement,PooledConnection pConn) throws SQLException {
        // borrower.setBorrowedConnection(pConn);
        // return pConn.proxyConnCurInstance=new ProxyConnection(pConn);
        throw new SQLException("Proxy classes not be generated,please execute 'ProxyClassUtil' after project compile");
    }
}

