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
import org.stone.beecp.exception.ConnectionRecycledException;

import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

import static org.stone.beecp.pool.ConnectionPoolStatics.*;
import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * Pooled Connection
 *
 * @author Chris Liao
 * @version 1.0
 */
final class PooledConnection {
    private static final boolean[] Default_Reset_Flags = new boolean[6];
    private final FastConnectionPool pool;

    //***************************************************************************************************************//
    //                                      section-A: fields of default value                                       //                                                                                  //
    //***************************************************************************************************************//
    boolean defaultAutoCommit;
    int defaultTransactionIsolation;
    boolean defaultReadOnly;
    String defaultCatalog;
    String defaultSchema;
    int defaultNetworkTimeout;
    boolean forceDirtyWhenSetSchema;
    boolean forceDirtyWhenSetCatalog;
    //***************************************************************************************************************//
    //                                     section-B: runtime change fields                                          //                                                                                  //
    //***************************************************************************************************************//
    volatile ConnectionCreatingInfo creatingInfo;

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

    //***************************************************************************************************************//
    //                                     section-C: some switch fields and others                                  //                                                                                  //
    //***************************************************************************************************************//
    private boolean enableDefaultCatalog;
    private boolean enableDefaultSchema;
    private boolean enableDefaultReadOnly;
    private boolean enableDefaultAutoCommit;
    private boolean enableDefaultTransactionIsolation;
    private boolean supportNetworkTimeoutInd;
    private ThreadPoolExecutor networkTimeoutExecutor;
    private List<Integer> sqlExceptionCodeList;
    private List<String> sqlExceptionStateList;
    private BeeConnectionPredicate predicate;

    //***************************************************************************************************************//
    //                                      1: constructor                                                           //                                                                                  //
    //***************************************************************************************************************//
    PooledConnection(FastConnectionPool pool) {
        this.pool = pool;
    }

    //***************************************************************************************************************//
    //                                      2: init method                                                           //                                                                                  //
    //***************************************************************************************************************//
    void init(
            //1:defaultAutoCommit
            boolean useDefaultAutoCommit,
            boolean defaultAutoCommit,
            //2:defaultTransactionIsolation
            boolean useDefaultTransactionIsolation,
            int defaultTransactionIsolation,
            //3:defaultReadOnly
            boolean useDefaultReadOnly,
            boolean defaultReadOnly,
            //4:defaultCatalog
            boolean useDefaultCatalog,
            String defaultCatalog,
            boolean forceDirtyWhenSetCatalog,
            //5:defaultSchema
            boolean useDefaultSchema,
            String defaultSchema,
            boolean forceDirtyWhenSetSchema,
            //6:defaultNetworkTimeout
            boolean supportNetworkTimeoutInd,
            int defaultNetworkTimeout,
            ThreadPoolExecutor networkTimeoutExecutor,
            //7:others
            List<Integer> sqlExceptionCodeList,
            List<String> sqlExceptionStateList,
            BeeConnectionPredicate predicate) {

        //1:defaultAutoCommit
        this.enableDefaultAutoCommit = useDefaultAutoCommit;
        this.defaultAutoCommit = defaultAutoCommit;
        //2:defaultTransactionIsolation
        this.enableDefaultTransactionIsolation = useDefaultTransactionIsolation;
        this.defaultTransactionIsolation = defaultTransactionIsolation;
        //3:defaultReadOnly
        this.enableDefaultReadOnly = useDefaultReadOnly;
        this.defaultReadOnly = defaultReadOnly;
        //4:defaultCatalog
        this.enableDefaultCatalog = useDefaultCatalog;
        this.defaultCatalog = defaultCatalog;
        this.forceDirtyWhenSetCatalog = forceDirtyWhenSetCatalog;
        //5:defaultSchema
        this.enableDefaultSchema = useDefaultSchema;
        this.defaultSchema = defaultSchema;
        this.forceDirtyWhenSetSchema = forceDirtyWhenSetSchema;
        //6:defaultNetworkTimeout
        this.supportNetworkTimeoutInd = supportNetworkTimeoutInd;
        this.defaultNetworkTimeout = defaultNetworkTimeout;
        this.networkTimeoutExecutor = networkTimeoutExecutor;
        //7:others
        this.sqlExceptionCodeList = sqlExceptionCodeList;
        this.sqlExceptionStateList = sqlExceptionStateList;
        this.predicate = predicate;
    }

    //***************************************************************************************************************//
    //                                      3: set a created connection and set default on it                        //                                                                                  //
    //***************************************************************************************************************//
    void setRawConnection(int state, Connection rawConn, XAResource rawXaRes) throws SQLException {
        if (enableDefaultAutoCommit && defaultAutoCommit != rawConn.getAutoCommit())
            rawConn.setAutoCommit(defaultAutoCommit);
        if (enableDefaultTransactionIsolation && defaultTransactionIsolation - rawConn.getTransactionIsolation() != 0)
            rawConn.setTransactionIsolation(defaultTransactionIsolation);
        if (enableDefaultReadOnly && defaultReadOnly != rawConn.isReadOnly())
            rawConn.setReadOnly(defaultReadOnly);
        if (enableDefaultCatalog && !Objects.equals(defaultCatalog, rawConn.getCatalog()))
            rawConn.setCatalog(defaultCatalog);
        if (enableDefaultSchema && !Objects.equals(defaultSchema, rawConn.getSchema()))
            rawConn.setSchema(defaultSchema);

        this.setRawConnection2(state, rawConn, rawXaRes);
    }

    void setRawConnection2(int state, Connection rawConn, XAResource rawXaRes) {
        this.rawConn = rawConn;
        this.rawXaRes = rawXaRes;
        this.resetFlags = new boolean[6];
        this.curAutoCommit = defaultAutoCommit;

        this.openStatements = new ProxyStatementBase[10];
        this.state = state;
        this.lastAccessTime = System.currentTimeMillis();
    }

    //***************************************************************************************************************//
    //                                     5: connection recycle(call by proxy connection)                           //                                                                                  //
    //***************************************************************************************************************//

    /**
     * remove connection from pool,method called by {@link ProxyConnectionBase#abort}
     */
    void abortSelf() {
        pool.abort(this, DESC_RM_CON_ABORT);
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
            this.pool.abort(this, DESC_RM_CON_BAD);
            throw e instanceof SQLException ? (SQLException) e : new ConnectionRecycledException(e);
        }
    }

    //***************************************************************************************************************//
    //                                    6:call back method                                                         //                                                                                  //
    //***************************************************************************************************************//

    /**
     * call back while remove pooledConnection from pool
     */
    void onRemove(String msg) {
        pool.logPrinter.info("BeeCP({})-begin to remove a pooled connection:{} for cause:{}", pool.poolName, this, msg);

        try {
            this.resetRawConn();
        } catch (Throwable e) {
            pool.logPrinter.warn("BeeCP({})-resetting connection failed", pool.poolName, e);
        } finally {
            oclose(this.rawConn);

            this.rawConn = null;
            this.rawXaRes = null;
            this.proxyInUsing = null;
            this.resetFlags = null;
            this.openStatements = null;
            this.state = CON_CLOSED;
        }
    }

    //***************************************************************************************************************//
    //                                     7: statement cache maintenance                                            //                                                                                  //
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
                s.unregister = true;
                this.openStatements[i] = null;
                oclose(s);
            }
        }
        this.openStmSize = 0;
    }

    //***************************************************************************************************************//
    //                                     8: other methods                                                          //                                                                                  //
    //***************************************************************************************************************//
    void updateAccessTime() {//for update,insert.select,delete and so on DML
        this.commitDirtyInd = !this.curAutoCommit;
        this.lastAccessTime = System.currentTimeMillis();
    }

    void checkSQLException(SQLException e) {//Fatal error code check
        ProxyConnectionBase proxyInUsing = this.proxyInUsing;
        if (proxyInUsing == null) return;

        if (predicate != null) {
            String msg = predicate.evictionTest(e);
            if (isNotBlank(msg)) {
                pool.logPrinter.warn("BeeCP({})-connection has been broken because of predicate result({})", pool.poolName, msg);
                proxyInUsing.abort(null);//remove connection from pool and add re-try count for other borrowers
            }
        } else {
            int code = e.getErrorCode();
            if (code != 0 && sqlExceptionCodeList != null && sqlExceptionCodeList.contains(code)) {
                pool.logPrinter.warn("BeeCP({})-connection has been broken because of error code({})", pool.poolName, code);
                proxyInUsing.abort(null);//remove connection from pool and add re-try count for other borrowers
                return;
            }

            String state = e.getSQLState();
            if (state != null && sqlExceptionStateList != null && sqlExceptionStateList.contains(state)) {
                pool.logPrinter.warn("BeeCP({})-connection has been broken because of SQL state({})", pool.poolName, state);
                proxyInUsing.abort(null);//remove connection from pool and add re-try count for other borrowers
            }
        }
    }

    //***************************************************************************************************************//
    //                                     9: dirty record or reset                                                  //                                                                                  //
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
            if (this.resetFlags[PS_CATALOG]) //reset catalog
                this.rawConn.setCatalog(this.defaultCatalog);

            //for JDK1.7 begin
            if (this.resetFlags[PS_SCHEMA]) //reset schema
                this.rawConn.setSchema(this.defaultSchema);
            if (this.resetFlags[PS_NETWORK]) //reset networkTimeout
                this.rawConn.setNetworkTimeout(this.networkTimeoutExecutor, this.defaultNetworkTimeout);
            //for JDK1.7 end
            this.resetCnt = 0;
            System.arraycopy(PooledConnection.Default_Reset_Flags, 0, this.resetFlags, 0, 6);
        }//reset end
        //clear warnings
        this.rawConn.clearWarnings();
    }
}