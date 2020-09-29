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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static cn.beecp.pool.PoolConstants.ResultSetClosedException;

/**
 * ResultSet proxy base class
 *
 * @author Chris.Liao
 * @version 1.0
 */
abstract class ProxyResultSetBase implements ResultSet {
    private static Logger log = LoggerFactory.getLogger(ProxyResultSetBase.class);
    protected ResultSet delegate;
    protected PooledConnection pConn;//called by subclass to update tim
    boolean isClosed;
    private ProxyStatementBase owner;//called by subclass to check close state

    public ProxyResultSetBase(ResultSet delegate, PooledConnection pConn) {
        this.delegate = delegate;
        this.pConn = pConn;
    }

    public ProxyResultSetBase(ResultSet delegate, ProxyStatementBase owner, PooledConnection pConn) {
        this.delegate = delegate;
        this.owner = owner;
        this.pConn = pConn;
        owner.setOpenResultSet(this);
    }

    boolean isDelegate(ResultSet delegate) {
        return this.delegate == delegate;
    }

    public Statement getStatement() throws SQLException {
        checkClosed();
        return owner;
    }

    public boolean isClosed() throws SQLException {
        return isClosed;
    }

    private void checkClosed() throws SQLException {
        if (isClosed) throw ResultSetClosedException;
    }

    public final void close() throws SQLException {
        if (!isClosed) {
            isClosed = true;
            delegate.close();
        }
    }

     void setAsClosed() {//call by ProxyStatementBase.close
        try {
            close();
        } catch (SQLException e) {
            log.error("Warning:error at closing resultSet:", e);
        }
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
}
