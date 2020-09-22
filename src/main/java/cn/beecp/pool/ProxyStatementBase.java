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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static cn.beecp.pool.PoolConstants.CLOSED_CSTM;
import static cn.beecp.pool.PoolConstants.StatementClosedException;

/**
 * ProxyStatementBase
 *
 * @author Chris.Liao
 * @version 1.0
 */
abstract class ProxyStatementBase implements Statement {
    private static Logger log = LoggerFactory.getLogger(ProxyStatementBase.class);
    protected Statement delegate;
    protected PooledConnection pConn;//called by subclass to update time
    private ProxyResultSetBase curResultSet;
    private boolean registered;
    private boolean isClosed;

    public ProxyStatementBase(Statement delegate,PooledConnection pConn) {
        this.pConn = pConn;
        this.delegate = delegate;
        if (registered = pConn.traceStatement)
            pConn.registerStatement(this);
    }

    private final void checkClosed() throws SQLException {
        if (isClosed) throw StatementClosedException;
    }

    public Connection getConnection() throws SQLException {
        checkClosed();
        return pConn.proxyConn;
    }

    public boolean isClosed() throws SQLException {
        return isClosed;
    }

    public void close() throws SQLException {
        if (!isClosed) {
            isClosed = true;
            if (curResultSet != null && !curResultSet.isClosed) curResultSet.setAsClosed();
            if (registered) pConn.unregisterStatement(this);

            try {
                delegate.close();
            } finally {
                delegate = CLOSED_CSTM;
            }
        }
    }

    void setAsClosed() {//call by PooledConnection.cleanOpenStatements
        try {
            registered = false;
            close();
        } catch (SQLException e) {
            log.error("Warning:error at closing statement:", e);
        }
    }

    public ResultSet getResultSet() throws SQLException {
        checkClosed();
        return curResultSet;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this))
            return (T) this;
        else
            throw new SQLException("Wrapped object is not an instance of " + iface);
    }

    void setOpenResultSet(ProxyResultSetBase resultSetNew) {//call by ProxyResultSetBase.constructor
        if (curResultSet != null) curResultSet.setAsClosed();
        this.curResultSet = resultSetNew;
    }
}
