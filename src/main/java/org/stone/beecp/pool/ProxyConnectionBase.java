/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;

import static org.stone.beecp.pool.ConnectionPoolStatics.*;
import static org.stone.tools.CommonUtil.objectEquals;

/**
 * connection proxy
 *
 * @author Chris Liao
 * @version 1.0
 */
public abstract class ProxyConnectionBase extends ProxyBaseWrapper implements Connection {
    protected Connection raw;

    ProxyConnectionBase(PooledConnection p) {
        super(p);
        raw = p.rawConn;
        p.proxyInUsing = this;
    }

    //***************************************************************************************************************//
    //                                             self-define methods(5)                                            //
    //***************************************************************************************************************//
    public final long getCreationTime() {
        return p.creationTime;
    }

    public final long getLassAccessTime() {
        return p.lastAccessTime;
    }

    public String toString() {
        return raw.toString();
    }

    final void checkClosed() throws SQLException {
        if (this.isClosed) throw new SQLException("No operations allowed after connection closed");
    }

    synchronized final void registerStatement(ProxyStatementBase s) {
        this.p.registerStatement(s);
    }

    synchronized final void unregisterStatement(ProxyStatementBase s) {
        this.p.unregisterStatement(s);
    }

    //***************************************************************************************************************//
    //                                              override methods (11)                                            //
    //***************************************************************************************************************//
    public boolean isClosed() {
        return this.isClosed;
    }

    //call by borrower,then return PooledConnection to pool
    public final void close() throws SQLException {
        synchronized (this) {//safe close
            if (this.isClosed) return;
            this.isClosed = true;
            this.raw = CLOSED_CON;
            if (this.p.openStmSize > 0) this.p.clearStatement();
        }
        this.p.recycleSelf();
    }

    public final void setAutoCommit(boolean autoCommit) throws SQLException {
        if (p.commitDirtyInd) throw new SQLException("Change forbidden when in transaction");
        this.raw.setAutoCommit(autoCommit);
        this.p.curAutoCommit = autoCommit;
        this.p.setResetInd(PS_AUTO, autoCommit != this.p.defaultAutoCommit);
    }

    public final void commit() throws SQLException {
        this.raw.commit();
        this.p.commitDirtyInd = false;
        this.p.lastAccessTime = System.currentTimeMillis();
    }

    public final void rollback() throws SQLException {
        this.raw.rollback();
        this.p.commitDirtyInd = false;
        this.p.lastAccessTime = System.currentTimeMillis();
    }

    public void setTransactionIsolation(int level) throws SQLException {
        this.raw.setTransactionIsolation(level);
        this.p.setResetInd(PS_TRANS, level != this.p.defaultTransactionIsolation);
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        this.raw.setReadOnly(readOnly);
        this.p.setResetInd(PS_READONLY, readOnly != this.p.defaultReadOnly);
    }

    public void setCatalog(String catalog) throws SQLException {
        this.raw.setCatalog(catalog);
        this.p.setResetInd(PS_CATALOG, p.forceDirtyOnCatalogAfterSet || !objectEquals(catalog, this.p.defaultCatalog));
    }

    //--------------------------JDBC 4.1 -----------------------------
    public void setSchema(String schema) throws SQLException {
        this.raw.setSchema(schema);
        this.p.setResetInd(PS_SCHEMA, p.forceDirtyOnSchemaAfterSet || !objectEquals(schema, this.p.defaultSchema));
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        if (this.p.supportNetworkTimeoutSet()) {
            this.raw.setNetworkTimeout(executor, milliseconds);
            this.p.setResetInd(PS_NETWORK, milliseconds != this.p.defaultNetworkTimeout);
        } else {
            throw new SQLException("Driver not support 'networkTimeout'");
        }
    }

    //*Terminates an open connection.Calling <code>abort</code> results in:
    //*<ul>
    //*<li>The connection marked as closed
    //*<li>Closes any physical connection to the database
    //*<li>Releases resources used by the connection
    //*<li>Insures that any thread that is currently accessing the connection
    //*will either progress to completion or throw an <code>SQLException</code>.
    public void abort(Executor executor) {
        synchronized (this) {//safe close
            if (this.isClosed) return;
            this.isClosed = true;
            this.raw = CLOSED_CON;
            if (this.p.openStmSize > 0) this.p.clearStatement();
        }
        this.p.removeSelf();//close raw connection and remove from pool
    }
    //for JDK1.7 end
}
