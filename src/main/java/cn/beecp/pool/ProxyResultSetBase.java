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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static cn.beecp.pool.PoolConstants.ResultSetClosedException;
import static cn.beecp.util.BeecpUtil.oclose;

/**
 * ResultSet proxy base class
 *
 * @author Chris.Liao
 * @version 1.0
 */
abstract class ProxyResultSetBase implements ResultSet {
    protected ResultSet delegate;
    protected PooledConnection pConn;//called by subclass to update tim
    private ProxyStatementBase owner;//called by subclass to check close state
    private boolean isClosed;

    public ProxyResultSetBase(ResultSet delegate, PooledConnection pConn) {
        this.pConn = pConn;
        this.delegate = delegate;
    }

    public ProxyResultSetBase(ResultSet delegate, ProxyStatementBase owner, PooledConnection pConn) {
        this.pConn = pConn;
        this.delegate = delegate;
        this.owner = owner;
        owner.setOpenResultSet(this);
}

    private final void checkClosed() throws SQLException {
        if (isClosed) throw ResultSetClosedException;
    }

    public Statement getStatement() throws SQLException {
        checkClosed();
        return owner;
    }

    public void close() throws SQLException {
        if (setAsClosed()) {
            if (owner != null)
                owner.openResultSet = null;
        } else {
            throw ResultSetClosedException;
        }
    }

    final boolean setAsClosed() {//call by ProxyStatementBase.close
        if (!isClosed) {
            oclose(delegate);
            return isClosed = true;
        } else {
            return false;
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
