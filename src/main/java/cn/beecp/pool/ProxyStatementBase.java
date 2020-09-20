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
import java.sql.Statement;

import static cn.beecp.pool.PoolConstants.StatementClosedException;
import static cn.beecp.util.BeecpUtil.oclose;

/**
 * ProxyStatementBase
 *
 * @author Chris.Liao
 * @version 1.0
 */
abstract class ProxyStatementBase implements Statement {
    protected Statement delegate;
    protected PooledConnection pConn;//called by subclass to update time
    protected ProxyConnectionBase owner;//called by subclass to check close state
    ProxyResultSetBase openResultSet;
    private boolean isClosed;
    //private Object cacheKey;

    public ProxyStatementBase(Statement delegate, ProxyConnectionBase proxyConn, PooledConnection pConn) {
        // this(delegate, proxyConn, pConn, null);
        this.pConn = pConn;
        this.owner = proxyConn;
        this.delegate = delegate;
        if (pConn.traceStatement)
            pConn.registerStatement(this);
    }

    private final void checkClosed() throws SQLException {
        if (isClosed) throw StatementClosedException;
    }

    public Connection getConnection() throws SQLException {
        checkClosed();
        return owner;
    }

    public void close() throws SQLException {
        if (setAsClosed()) {
            if (pConn.traceStatement)
                pConn.unregisterStatement(this);//remove trace
        } else {
            throw StatementClosedException;
        }
    }

    boolean setAsClosed() {//call by PooledConnection.cleanOpenStatements
        if (!isClosed) {
            if (openResultSet != null) openResultSet.setAsClosed();
            oclose(delegate);
            return isClosed = true;
        } else {
            return false;
        }
    }

    public ResultSet getResultSet() throws SQLException {
        checkClosed();
        return openResultSet;
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
        if (openResultSet != null) openResultSet.setAsClosed();
        this.openResultSet = resultSetNew;
    }
}
