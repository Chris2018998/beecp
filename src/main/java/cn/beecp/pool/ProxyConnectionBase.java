/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp.pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;

import static cn.beecp.pool.PoolStaticCenter.*;
import static java.lang.System.currentTimeMillis;

/**
 * raw connection wrapper
 *
 * @author Chris.Liao
 * @version 1.0
 */
public abstract class ProxyConnectionBase implements Connection {
    protected Connection delegate;
    protected PooledConnection pConn;//called by subclass to update time
    private boolean isClosed;

    public ProxyConnectionBase(PooledConnection pConn) {
        this.pConn = pConn;
        pConn.proxyConn = this;
        this.delegate = pConn.rawConn;
    }

    public Connection getDelegate() throws SQLException {
        checkClosed();
        return delegate;
    }

    public final boolean isClosed() throws SQLException {
        return isClosed;
    }

    protected final void checkClosed() throws SQLException {
        if (isClosed) throw ConnectionClosedException;
    }

    public final void close() throws SQLException {
        synchronized (this) {//safe close
            if (isClosed) return;
            isClosed = true;
            delegate = CLOSED_CON;
            if (pConn.statementPos > 0)
                pConn.clearStatement();
        }
        pConn.recycleSelf();
    }

    final void trySetAsClosed() {//called from FastConnectionPool
        try {
            close();
        } catch (Throwable e) {
        }
    }

    /************* statement trace :logic from mysql driver******************************/
    synchronized final boolean registerStatement(ProxyStatementBase st) {
        pConn.registerStatement(st);
        return true;
    }

    synchronized final void unregisterStatement(ProxyStatementBase st) {
        pConn.unregisterStatement(st);
    }

    /************* statement trace ******************************/

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        checkClosed();
        if (!pConn.curAutoCommit && pConn.commitDirtyInd)
            throw AutoCommitChangeForbiddenException;

        delegate.setAutoCommit(autoCommit);
        pConn.curAutoCommit = autoCommit;
        if (autoCommit) pConn.commitDirtyInd = false;
        pConn.setResetInd(POS_AUTO, autoCommit != pConn.defaultAutoCommit);
    }

    public void setTransactionIsolation(int level) throws SQLException {
        delegate.setTransactionIsolation(level);
        pConn.setResetInd(POS_TRANS, level != pConn.defaultTransactionIsolationCode);
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        delegate.setReadOnly(readOnly);
        pConn.setResetInd(POS_READONLY, readOnly != pConn.defaultReadOnly);
    }

    public void setCatalog(String catalog) throws SQLException {
        delegate.setCatalog(catalog);
        pConn.setResetInd(POS_CATALOG, !PoolStaticCenter.equals(catalog, pConn.defaultCatalog));
    }

    public boolean isValid(int timeout) throws SQLException {
        return delegate.isValid(timeout);
    }

    //for JDK1.7 begin
    public void setSchema(String schema) throws SQLException {
        delegate.setSchema(schema);
        pConn.setResetInd(POS_SCHEMA, !PoolStaticCenter.equals(schema, pConn.defaultSchema));
    }

    public void abort(Executor executor) throws SQLException {
        checkClosed();
        if (executor == null) throw new SQLException("executor can't be null");
        executor.execute(new Runnable() {
            public void run() {
                ProxyConnectionBase.this.trySetAsClosed();
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
            pConn.setResetInd(POS_NETWORK, milliseconds != pConn.defaultNetworkTimeout);
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
