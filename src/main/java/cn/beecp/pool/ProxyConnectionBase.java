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
    protected final PooledConnection p;//called by subclass to update time
    protected Connection raw;
    private boolean isClosed;

    public ProxyConnectionBase(PooledConnection p) {
        this.p = p;
        this.raw = p.raw;
        p.proxyCon = this;
    }

    /*******************************************************************************************
     *                                                                                         *
     *                         Below are self define methods                                          *
     *                                                                                         *
     ********************************************************************************************/

    final void checkClosed() throws SQLException {
        if (isClosed) throw ConnectionClosedException;
    }

    public Connection getRaw() throws SQLException {
        checkClosed();
        return raw;
    }

    synchronized final void registerStatement(ProxyStatementBase s) {
        p.registerStatement(s);
    }

    synchronized final void unregisterStatement(ProxyStatementBase s) {
        p.unregisterStatement(s);
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
            raw = CLOSED_CON;
            if (p.openStmSize > 0) p.clearStatement();
        }
        p.recycleSelf();
    }

    public final void setAutoCommit(boolean autoCommit) throws SQLException {
        if (p.commitDirtyInd) throw AutoCommitChangeForbiddenException;
        raw.setAutoCommit(autoCommit);
        p.curAutoCommit = autoCommit;
        p.setResetInd(PS_AUTO, autoCommit != p.defAutoCommit);
    }

    public void setTransactionIsolation(int level) throws SQLException {
        raw.setTransactionIsolation(level);
        p.setResetInd(PS_TRANS, level != p.defTransactionIsolation);
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        raw.setReadOnly(readOnly);
        p.setResetInd(PS_READONLY, readOnly != p.defReadOnly);
    }

    public void setCatalog(String catalog) throws SQLException {
        raw.setCatalog(catalog);
        p.setResetInd(PS_CATALOG, !PoolStaticCenter.equals(catalog, p.defCatalog));
    }

    //for JDK1.7 begin
    public void setSchema(String schema) throws SQLException {
        raw.setSchema(schema);
        p.setResetInd(PS_SCHEMA, !PoolStaticCenter.equals(schema, p.defSchema));
    }

    public void abort(Executor executor) throws SQLException {
        if (executor == null) throw new SQLException("executor can't be null");
        executor.execute(new ProxyConnectionCloseTask(this));
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        if (p.supportNetworkTimeout()) {
            raw.setNetworkTimeout(executor, milliseconds);
            p.setResetInd(PS_NETWORK, milliseconds != p.defNetworkTimeout);
        } else {
            throw DriverNotSupportNetworkTimeoutException;
        }
    }
    //for JDK1.7 end

    public void commit() throws SQLException {
        raw.commit();
        p.lastAccessTime = currentTimeMillis();
        p.commitDirtyInd = false;
    }

    public void rollback() throws SQLException {
        raw.rollback();
        p.lastAccessTime = currentTimeMillis();
        p.commitDirtyInd = false;
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
