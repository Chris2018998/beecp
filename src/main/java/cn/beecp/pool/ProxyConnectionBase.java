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

import cn.beecp.util.BeeJdbcUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;

import static cn.beecp.pool.PoolConstants.*;
import static java.lang.System.currentTimeMillis;

/**
 * raw connection wrapper
 *
 * @author Chris.Liao
 * @version 1.0
 */
public abstract class ProxyConnectionBase implements Connection {
    private final static int Pos_AutoCommitInd = 0;
    private final static int Pos_TransactionIsolationInd = 1;
    private final static int Pos_ReadOnlyInd = 2;
    private final static int Pos_CatalogInd = 3;
    private final static int Pos_SchemaInd = 4;
    private final static int Pos_NetworkTimeoutInd = 5;

    protected Connection delegate;
    protected PooledConnection pConn;//called by subclass to update time
    private boolean isClosed;

    protected ProxyConnectionBase(){}
    public ProxyConnectionBase(PooledConnection pConn) {
        this.pConn = pConn;
        pConn.proxyConn = this;
        this.delegate = pConn.rawConn;
    }

    public Connection getDelegate() throws SQLException {
        checkClosed();
        return delegate;
    }

    public boolean isClosed() throws SQLException {
        return isClosed;
    }

    protected void checkClosed() throws SQLException {
        if (isClosed) throw ConnectionClosedException;
    }

    public final void close() throws SQLException {
        synchronized (pConn) {
            if (isClosed) return;
            isClosed = true;
            if (pConn.tracedPos > 0)
                pConn.cleanTracedStatements();
        }
        delegate = CLOSED_CON;
        pConn.recycleSelf();
    }

    final void trySetAsClosed() {//called from FastConnectionPool
        try {
            close();
        } catch (SQLException e) {
        }
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        checkClosed();
        if (!pConn.curAutoCommit && pConn.commitDirtyInd)
            throw AutoCommitChangeForbiddenException;

        delegate.setAutoCommit(autoCommit);
        pConn.curAutoCommit = autoCommit;
        if (autoCommit) pConn.commitDirtyInd = false;
        pConn.setResetInd(Pos_AutoCommitInd, autoCommit != pConn.defaultAutoCommit);
    }

    public void setTransactionIsolation(int level) throws SQLException {
        delegate.setTransactionIsolation(level);
        pConn.setResetInd(Pos_TransactionIsolationInd, level != pConn.defaultTransactionIsolationCode);
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        delegate.setReadOnly(readOnly);
        pConn.setResetInd(Pos_ReadOnlyInd, readOnly != pConn.defaultReadOnly);
    }

    public void setCatalog(String catalog) throws SQLException {
        delegate.setCatalog(catalog);
        pConn.setResetInd(Pos_CatalogInd, !BeeJdbcUtil.equals(catalog, pConn.defaultCatalog));
    }

    public boolean isValid(int timeout) throws SQLException {
        return delegate.isValid(timeout);
    }

    //for JDK1.7 begin
    public void setSchema(String schema) throws SQLException {
        delegate.setSchema(schema);
        pConn.setResetInd(Pos_SchemaInd, !BeeJdbcUtil.equals(schema, pConn.defaultSchema));
    }

    public void abort(Executor executor) throws SQLException {
        checkClosed();
        if (executor == null) throw new SQLException("executor can't be null");
        executor.execute(new Runnable() {
            public void run() {
                try {
                    ProxyConnectionBase.this.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public int getNetworkTimeout() throws SQLException {
        return delegate.getNetworkTimeout();
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        checkClosed();
        if (pConn.supportNetworkTimeout()) {
            delegate.setNetworkTimeout(executor, milliseconds);
            pConn.setResetInd(Pos_NetworkTimeoutInd, milliseconds != pConn.defaultNetworkTimeout);
        } else {
            throw DriverNotSupportNetworkTimeoutException;
        }
    }
    //for JDK1.7 end

    public void commit() throws SQLException {
        delegate.commit();
        pConn.lastAccessTime = currentTimeMillis();
        pConn.commitDirtyInd = false;
    }

    public void rollback() throws SQLException {
        delegate.rollback();
        pConn.lastAccessTime = currentTimeMillis();
        pConn.commitDirtyInd = false;
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
