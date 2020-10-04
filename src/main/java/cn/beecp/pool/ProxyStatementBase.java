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
import java.util.ArrayList;

import static cn.beecp.pool.PoolConstants.CLOSED_CSTM;
import static cn.beecp.pool.PoolConstants.StatementClosedException;

/**
 * ProxyStatementBase
 *
 * @author Chris.Liao
 * @version 1.0
 */
abstract class ProxyStatementBase implements Statement {
    protected Statement delegate;
    protected PooledConnection pConn;//called by subclass to update time
    private ProxyResultSetBase curRe;
    private boolean registered;
    private boolean isClosed;
    private int resultOpenCode = CLOSE_CURRENT_RESULT;
    private ArrayList<ProxyResultSetBase> results = new ArrayList<>();

    public ProxyStatementBase(Statement delegate, PooledConnection pConn) {
        this.delegate = delegate;
        this.pConn = pConn;
        if (registered = pConn.traceStatement)
            pConn.registerStatement(this);
    }

    private void checkClosed() throws SQLException {
        if (isClosed) throw StatementClosedException;
    }

    public Connection getConnection() throws SQLException {
        checkClosed();
        return pConn.proxyConn;
    }

    public boolean isClosed() throws SQLException {
        return isClosed;
    }

    public final void close() throws SQLException {
        if (!isClosed) {
            isClosed = true;
            if (curRe != null && !curRe.isClosed)
                curRe.setAsClosed();
            if (results.size() > 0) {
                for (ProxyResultSetBase re : results)
                    re.setAsClosed();
                results.clear();
            }
            if (registered)
                pConn.unregisterStatement(this);

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
        }
    }

    final void setOpenResultSet(ProxyResultSetBase resultSetNew) {//call by ProxyResultSetBase.constructor
        switch (resultOpenCode) {
            case CLOSE_CURRENT_RESULT: {
                if (curRe != null && !curRe.isClosed) curRe.setAsClosed();
                break;
            }
            case KEEP_CURRENT_RESULT: {
                if (curRe != null && !curRe.isClosed) results.add(curRe);
                break;
            }
            case CLOSE_ALL_RESULTS: {
                if (curRe != null && !curRe.isClosed)
                    curRe.setAsClosed();
                for (ProxyResultSetBase openRe : results)
                    if (!openRe.isClosed) openRe.setAsClosed();
                results.clear();
                break;
            }
            default:
                break;
        }
        this.curRe = resultSetNew;
    }

    public boolean getMoreResults() throws SQLException {
        return getMoreResults(CLOSE_CURRENT_RESULT);
    }

    public boolean getMoreResults(int current) throws SQLException {
        checkClosed();
        resultOpenCode = current;
        return delegate.getMoreResults(current);
    }

    public ResultSet getResultSet() throws SQLException {
        ResultSet re = delegate.getResultSet();
        if (re == null) {
            setOpenResultSet(null);
            return null;
        } else {
            if (curRe != null && curRe.isDelegate(re))
                return curRe;

            return ProxyObjectFactory.createProxyResultSet(re, this, pConn);
        }
    }

    public void setPoolable(boolean var1) throws SQLException {
        checkClosed();
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
