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
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBC Proxy Factory
 *
 * @author Chris.Liao
 * @version 1.0
 */
class ProxyObjectFactory {

    public static final Connection createProxyConnection(PooledConnection pConn, FastConnectionPool.Borrower borrower)
            throws SQLException {
        // borrower.setBorrowedConnection(pConn);
        // return pConn.proxyConnCurInstance=new ProxyConnection(pConn);
        throw new SQLException("Proxy classes not be generated,please execute 'ProxyClassGenerator' after compile");
    }

    public final static ResultSet createProxyResultSet(ResultSet delegate,ProxyStatementBase proxyStatement,PooledConnection pConn) throws SQLException {
        // return new ProxyResultSet(delegate,pConn);
        throw new SQLException("Proxy classes not be generated,please execute 'ProxyClassGenerator' after compile");
    }
}
