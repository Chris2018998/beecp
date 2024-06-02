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

import org.stone.beecp.BeeConnectionPredicate;
import org.stone.beecp.pool.exception.ConnectionRecycleException;

import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static org.stone.beecp.pool.ConnectionPoolStatics.*;
import static org.stone.tools.BeanUtil.CommonLog;
import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * Pooled Connection
 *
 * @author Chris Liao
 * @version 1.0
 */
final class PooledConnection implements Cloneable {
    private static final boolean[] FALSE = new boolean[6];
    final boolean defaultAutoCommit;
    final int defaultTransactionIsolation;
    final boolean defaultReadOnly;
    final String defaultCatalog;
    final String defaultSchema;
    final int defaultNetworkTimeout;
    final boolean forceDirtyOnSchemaAfterSet;
    final boolean forceDirtyOnCatalogAfterSet;
    private final boolean defaultCatalogIsNotBlank;
    private final boolean defaultSchemaIsNotBlank;
    private final boolean supportNetworkTimeoutInd;
    private final ThreadPoolExecutor networkTimeoutExecutor;
    private final FastConnectionPool pool;
    private final boolean enableDefaultOnCatalog;
    private final boolean enableDefaultOnSchema;
    private final boolean enableDefaultOnReadOnly;
    private final boolean enableDefaultOnAutoCommit;
    private final boolean enableDefaultOnTransactionIsolation;
    private final List<Integer> sqlExceptionCodeList;
    private final List<String> sqlExceptionStateList;
    private final BeeConnectionPredicate predicate;

    long creationTime;//milliseconds
    Connection rawConn;//maybe from XAConnection
    XAResource rawXaRes;//from XAConnection
    volatile int state;
    volatile long lastAccessTime;//milliseconds
    int openStmSize;
    boolean curAutoCommit;
    boolean commitDirtyInd;
    ProxyConnectionBase proxyInUsing;//default is null

    private int resetCnt;//reset count
    private boolean[] resetFlags;
    private ProxyStatementBase[] openStatements;

    //template pooled connection to create other pooled connections with clone way
    PooledConnection(
            FastConnectionPool pool,
            //1:defaultAutoCommit
            boolean enableDefaultOnAutoCommit,
            boolean defaultAutoCommit,
            //2:defaultTransactionIsolation
            boolean enableDefaultOnTransactionIsolation,
            int defaultTransactionIsolation,
            //3:defaultReadOnly
            boolean enableDefaultOnReadOnly,
            boolean defaultReadOnly,
            //4:defaultCatalog
            boolean enableDefaultOnCatalog,
            String defaultCatalog,
            boolean forceDirtyOnCatalogAfterSet,
            //5:defaultSchema
            boolean enableDefaultOnSchema,
            String defaultSchema,
            boolean forceDirtyOnSchemaAfterSet,
            //6:defaultNetworkTimeout
            boolean supportNetworkTimeoutInd,
            int defaultNetworkTimeout,
            ThreadPoolExecutor networkTimeoutExecutor,
            //7:others
            List<Integer> sqlExceptionCodeList,
            List<String> sqlExceptionStateList,
            BeeConnectionPredicate predicate) {

        //1:defaultAutoCommit
        this.enableDefaultOnAutoCommit = enableDefaultOnAutoCommit;
        this.defaultAutoCommit = defaultAutoCommit;
        //2:defaultTransactionIsolation
        this.enableDefaultOnTransactionIsolation = enableDefaultOnTransactionIsolation;
        this.defaultTransactionIsolation = defaultTransactionIsolation;
        //3:defaultReadOnly
        this.enableDefaultOnReadOnly = enableDefaultOnReadOnly;
        this.defaultReadOnly = defaultReadOnly;
        //4:defaultCatalog
        this.enableDefaultOnCatalog = enableDefaultOnCatalog;
        this.defaultCatalog = defaultCatalog;
        this.defaultCatalogIsNotBlank = isNotBlank(defaultCatalog);
        this.forceDirtyOnCatalogAfterSet = forceDirtyOnCatalogAfterSet;
        //5:defaultSchema
        this.enableDefaultOnSchema = enableDefaultOnSchema;
        this.defaultSchema = defaultSchema;
        this.defaultSchemaIsNotBlank = isNotBlank(defaultSchema);
        this.forceDirtyOnSchemaAfterSet = forceDirtyOnSchemaAfterSet;
        //6:defaultNetworkTimeout
        this.supportNetworkTimeoutInd = supportNetworkTimeoutInd;
        this.defaultNetworkTimeout = defaultNetworkTimeout;
        this.networkTimeoutExecutor = networkTimeoutExecutor;
        //7:others
        this.sqlExceptionCodeList = sqlExceptionCodeList;
        this.sqlExceptionStateList = sqlExceptionStateList;
        this.predicate = predicate;

        this.pool = pool;
        this.curAutoCommit = defaultAutoCommit;
    }

    PooledConnection createFirstByClone(Connection rawConn, int state, XAResource rawXaRes) throws CloneNotSupportedException {
        PooledConnection p = (PooledConnection) clone();
        p.state = state;
        p.rawConn = rawConn;
        p.rawXaRes = rawXaRes;
        p.resetFlags = FALSE.clone();
        p.openStatements = new ProxyStatementBase[10];
        p.creationTime = System.currentTimeMillis();
        p.lastAccessTime = p.creationTime;
        return p;
    }

    PooledConnection setDefaultAndCreateByClone(Connection rawConn, int state, XAResource rawXaRes) throws SQLException, CloneNotSupportedException {
        if (enableDefaultOnAutoCommit && defaultAutoCommit != rawConn.getAutoCommit())
            rawConn.setAutoCommit(defaultAutoCommit);
        if (enableDefaultOnTransactionIsolation && defaultTransactionIsolation != rawConn.getTransactionIsolation())
            rawConn.setTransactionIsolation(defaultTransactionIsolation);
        if (enableDefaultOnReadOnly && defaultReadOnly != rawConn.isReadOnly())
            rawConn.setReadOnly(defaultReadOnly);
        if (enableDefaultOnCatalog && defaultCatalogIsNotBlank && !defaultCatalog.equals(rawConn.getCatalog()))
            rawConn.setCatalog(defaultCatalog);
        if (enableDefaultOnSchema && defaultSchemaIsNotBlank && !defaultSchema.equals(rawConn.getSchema()))
            rawConn.setSchema(defaultSchema);

        PooledConnection p = (PooledConnection) clone();
        p.state = state;
        p.rawConn = rawConn;
        p.rawXaRes = rawXaRes;
        p.resetFlags = FALSE.clone();
        p.openStatements = new ProxyStatementBase[10];
        p.creationTime = System.currentTimeMillis();
        p.lastAccessTime = p.creationTime;
        return p;
    }

    //***************************************************************************************************************//
    //                                      1:connection recycle(call by proxy connection)                           //                                                                                  //
    //***************************************************************************************************************//

    /**
     * remove connection from pool,method called by {@link ProxyConnectionBase#abort}
     */
    void removeSelf() {
        pool.abandonOnReturn(this, DESC_RM_ABORT);
    }

    /**
     * return borrowed connection to pool,method called by {@link ProxyConnectionBase#close}
     *
     * @throws SQLException when error occurs during recycle
     */
    void recycleSelf() throws SQLException {
        try {
            this.proxyInUsing = null;
            this.resetRawConn();
            this.pool.recycle(this);
        } catch (Throwable e) {
            this.pool.abandonOnReturn(this, DESC_RM_BAD);
            throw e instanceof SQLException ? (SQLException) e : new ConnectionRecycleException(e);
        }
    }

    //***************************************************************************************************************//
    //                                    2:call back method                                                         //                                                                                  //
    //***************************************************************************************************************//

    /**
     * call back while remove pooledConnection from pool
     */
    void onBeforeRemove() {
        try {
            this.state = CON_CLOSED;
            this.resetRawConn();
        } catch (Throwable e) {
            if (pool.isPrintRuntimeLog()) CommonLog.warn("BeeCP({})Resetting connection failed", pool.getPoolName(), e);
        } finally {
            oclose(this.rawConn);
            this.rawXaRes = null;
        }
    }

    //***************************************************************************************************************//
    //                                     3:statement cache maintenance                                             //                                                                                  //
    //***************************************************************************************************************//
    void registerStatement(ProxyStatementBase s) {
        if (this.openStmSize == this.openStatements.length) {//full
            ProxyStatementBase[] array = new ProxyStatementBase[this.openStmSize << 1];
            System.arraycopy(this.openStatements, 0, array, 0, this.openStmSize);
            this.openStatements = array;
        }
        this.openStatements[this.openStmSize++] = s;
    }

    void unregisterStatement(ProxyStatementBase s) {
        for (int i = this.openStmSize - 1; i >= 0; i--) {
            if (s == this.openStatements[i]) {
                int m = this.openStmSize - i - 1;
                if (m > 0) System.arraycopy(this.openStatements, i + 1, this.openStatements, i, m);//move ahead
                this.openStatements[--this.openStmSize] = null; // clear to let GC do its work
                return;
            }
        }
    }

    void clearStatement() {
        for (int i = 0; i < this.openStmSize; i++) {
            ProxyStatementBase s = this.openStatements[i];
            if (s != null) {
                s.registered = false;
                this.openStatements[i] = null;
                oclose(s);
            }
        }
        this.openStmSize = 0;
    }

    //***************************************************************************************************************//
    //                                     4:other methods                                                           //                                                                                  //
    //***************************************************************************************************************//
    void updateAccessTime() {//for update,insert.select,delete and so on DML
        this.commitDirtyInd = !this.curAutoCommit;
        this.lastAccessTime = System.currentTimeMillis();
    }

    void checkSQLException(SQLException e) {//Fatal error code check
        ProxyConnectionBase proxyInUsing = this.proxyInUsing;
        if (proxyInUsing == null) return;

        if (predicate != null) {
            String msg = predicate.evictTest(e);
            if (isNotBlank(msg)) {
                if (pool.isPrintRuntimeLog())
                    CommonLog.warn("BeeCP({})Connection has been broken because of predicate result({})", pool.getPoolName(), msg);
                proxyInUsing.abort(null);//remove connection from pool and add re-try count for other borrowers
            }
        } else {
            int code = e.getErrorCode();
            if (code != 0 && sqlExceptionCodeList != null && sqlExceptionCodeList.contains(code)) {
                if (pool.isPrintRuntimeLog())
                    CommonLog.warn("BeeCP({})Connection has been broken because of error code({})", pool.getPoolName(), code);
                proxyInUsing.abort(null);//remove connection from pool and add re-try count for other borrowers
                return;
            }

            String state = e.getSQLState();
            if (state != null && sqlExceptionStateList != null && sqlExceptionStateList.contains(state)) {
                if (pool.isPrintRuntimeLog())
                    CommonLog.warn("BeeCP({})Connection has been broken because of SQL state({})", pool.getPoolName(), state);
                proxyInUsing.abort(null);//remove connection from pool and add re-try count for other borrowers
            }
        }
    }

    //***************************************************************************************************************//
    //                                     5:dirty record or reset                                                   //                                                                                  //
    //***************************************************************************************************************//
    boolean supportNetworkTimeoutSet() {
        return this.supportNetworkTimeoutInd;
    }

    void setResetInd(int i, boolean changed) {
        if (this.resetFlags[i] != changed) {
            this.resetFlags[i] = changed;
            this.resetCnt += changed ? 1 : -1;
        }
    }

    private void resetRawConn() throws SQLException {
        if (this.commitDirtyInd) { //Roll back when commit dirty
            this.rawConn.rollback();
            this.commitDirtyInd = false;
        }
        //reset begin
        if (this.resetCnt > 0) {
            if (this.resetFlags[PS_AUTO]) {//reset autoCommit
                this.rawConn.setAutoCommit(this.defaultAutoCommit);
                this.curAutoCommit = this.defaultAutoCommit;
            }
            if (this.resetFlags[PS_TRANS])
                this.rawConn.setTransactionIsolation(this.defaultTransactionIsolation);
            if (this.resetFlags[PS_READONLY]) //reset readonly
                this.rawConn.setReadOnly(this.defaultReadOnly);
            if (this.defaultCatalogIsNotBlank && this.resetFlags[PS_CATALOG]) //reset catalog
                this.rawConn.setCatalog(this.defaultCatalog);

            //for JDK1.7 begin
            if (this.defaultSchemaIsNotBlank && this.resetFlags[PS_SCHEMA]) //reset schema
                this.rawConn.setSchema(this.defaultSchema);
            if (this.resetFlags[PS_NETWORK]) //reset networkTimeout
                this.rawConn.setNetworkTimeout(this.networkTimeoutExecutor, this.defaultNetworkTimeout);
            //for JDK1.7 end
            this.resetCnt = 0;
            System.arraycopy(PooledConnection.FALSE, 0, this.resetFlags, 0, 6);
        }//reset end
        //clear warnings
        this.rawConn.clearWarnings();
    }
}