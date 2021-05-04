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
    protected PooledConnection pCon;//called by subclass to update time
    private boolean isClosed;

    public ProxyConnectionBase(PooledConnection pCon) {
        this.pCon = pCon;
        pCon.proxyCon = this;
        this.delegate = pCon.rawCon;
    }

    /*******************************************************************************************
     *                                                                                         *
     *                         Below are self methods                                          *
     *                                                                                         *
     ********************************************************************************************/

    final void checkClosed() throws SQLException {
        if (isClosed) throw ConnectionClosedException;
    }

    public Connection getDelegate() throws SQLException {
        checkClosed();
        return delegate;
    }

    synchronized final void registerStatement(ProxyStatementBase s) {
        pCon.registerStatement(s);
    }

    synchronized final void unregisterStatement(ProxyStatementBase s) {
        pCon.unregisterStatement(s);
    }

    /******************************************************************************************
     *                                                                                        *
     *                        Below are override methods                                      *
     *                                                                                        *
     ******************************************************************************************/

    public final boolean isClosed() throws SQLException {
        return isClosed;
    }

    //call by borrower,then return PooledConnection to pool
    public final void close() throws SQLException {
        synchronized (this) {//safe close
            if (isClosed) return;
            isClosed = true;
            delegate = CLOSED_CON;
            if (pCon.openStmSize > 0) pCon.clearStatement();
        }
        pCon.recycleSelf();
    }

    public final void setAutoCommit(boolean autoCommit) throws SQLException {
        if (!pCon.curAutoCommit && pCon.commitDirtyInd)
            throw AutoCommitChangeForbiddenException;
        delegate.setAutoCommit(autoCommit);
        pCon.curAutoCommit = autoCommit;
        if (autoCommit) pCon.commitDirtyInd = false;
        pCon.setResetInd(PS_AUTO, autoCommit != pCon.cfgAutoCommit);
    }

    public void setTransactionIsolation(int level) throws SQLException {
        delegate.setTransactionIsolation(level);
        pCon.setResetInd(PS_TRANS, level != pCon.cfgTransactionIsolationCode);
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        delegate.setReadOnly(readOnly);
        pCon.setResetInd(PS_READONLY, readOnly != pCon.cfgReadOnly);
    }

    public void setCatalog(String catalog) throws SQLException {
        delegate.setCatalog(catalog);
        pCon.setResetInd(PS_CATALOG, !PoolStaticCenter.equals(catalog, pCon.cfgCatalog));
    }

    //for JDK1.7 begin
    public void setSchema(String schema) throws SQLException {
        delegate.setSchema(schema);
        pCon.setResetInd(PS_SCHEMA, !PoolStaticCenter.equals(schema, pCon.cfgSchema));
    }

    public void abort(Executor executor) throws SQLException {
        if (executor == null) throw new SQLException("executor can't be null");
        executor.execute(new ProxyConnectionCloseTask(this));
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        if (pCon.supportNetworkTimeout()) {
            delegate.setNetworkTimeout(executor, milliseconds);
            pCon.setResetInd(PS_NETWORK, milliseconds != pCon.cfgNetworkTimeout);
        } else {
            throw DriverNotSupportNetworkTimeoutException;
        }
    }
    //for JDK1.7 end

    public void commit() throws SQLException {
        delegate.commit();
        pCon.lastAccessTime = currentTimeMillis();
        pCon.commitDirtyInd = false;
    }

    public void rollback() throws SQLException {
        delegate.rollback();
        pCon.lastAccessTime = currentTimeMillis();
        pCon.commitDirtyInd = false;
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

    private static final class ProxyConnectionCloseTask implements Runnable {
        private ProxyConnectionBase proxyCon;

        public ProxyConnectionCloseTask(ProxyConnectionBase proxyCon) {
            this.proxyCon = proxyCon;
        }

        public void run() {
            try {
                proxyCon.close();
            } catch (Throwable e) {
            }
        }
    }
}
