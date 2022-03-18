/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;

import static cn.beecp.pool.PoolStaticCenter.*;

/**
 * rawConn connection wrapper
 *
 * @author Chris.Liao
 * @version 1.0
 */
abstract class ProxyConnectionBase extends ProxyBaseWrapper implements Connection {
    protected Connection raw;

    public ProxyConnectionBase(PooledConnection p) {
        super(p);
        this.raw = p.rawConn;
        p.proxyInUsing = this;
    }

    //***************************************************************************************************************//
    //                                             self-define methods(4)                                            //
    //***************************************************************************************************************//
    public boolean getClosedInd() {
        return isClosed;
    }

    public void checkClosed() throws SQLException {
        if (isClosed) throw ConnectionClosedException;
    }

    synchronized void registerStatement(ProxyStatementBase s) {
        p.registerStatement(s);
    }

    synchronized void unregisterStatement(ProxyStatementBase s) {
        p.unregisterStatement(s);
    }

    //***************************************************************************************************************//
    //                                              override methods (11)                                            //
    //***************************************************************************************************************//
    public boolean isClosed() {
        return isClosed;
    }

    //call by borrower,then return PooledConnection to pool
    public void close() throws SQLException {
        synchronized (this) {//safe close
            if (isClosed) return;
            isClosed = true;
            raw = CLOSED_CON;
            if (p.openStmSize > 0) p.clearStatement();
        }
        p.recycleSelf();
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        //if (p.commitDirtyInd) throw DirtyTransactionException;
        raw.setAutoCommit(autoCommit);
        p.curAutoCommit = autoCommit;
        p.setResetInd(PS_AUTO, autoCommit != p.defaultAutoCommit);
    }

    public void commit() throws SQLException {
        raw.commit();
        p.commitDirtyInd = false;
        p.lastAccessTime = System.currentTimeMillis();
    }

    public void rollback() throws SQLException {
        raw.rollback();
        p.commitDirtyInd = false;
        p.lastAccessTime = System.currentTimeMillis();
    }

    public void setTransactionIsolation(int level) throws SQLException {
        raw.setTransactionIsolation(level);
        p.setResetInd(PS_TRANS, level != p.defaultTransactionIsolation);
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        raw.setReadOnly(readOnly);
        p.setResetInd(PS_READONLY, readOnly != p.defaultReadOnly);
    }

    public void setCatalog(String catalog) throws SQLException {
        raw.setCatalog(catalog);
        p.setResetInd(PS_CATALOG, !PoolStaticCenter.equals(catalog, p.defaultCatalog));
    }

    //for JDK1.7 begin
    public void setSchema(String schema) throws SQLException {
        raw.setSchema(schema);
        p.setResetInd(PS_SCHEMA, !PoolStaticCenter.equals(schema, p.defaultSchema));
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        if (p.supportNetworkTimeoutSet()) {
            raw.setNetworkTimeout(executor, milliseconds);
            p.setResetInd(PS_NETWORK, milliseconds != p.defaultNetworkTimeout);
        } else {
            throw DriverNotSupportNetworkTimeoutException;
        }
    }

    public void abort(Executor executor) throws SQLException {
        if (executor == null) throw new SQLException("executor can't be null");
        executor.execute(new ProxyConnectionCloseTask(this));
    }
    //for JDK1.7 end
}
