/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package cn.beecp.pool;

import cn.beecp.pool.exception.ConnectionRecycleException;

import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static cn.beecp.pool.ConnectionPoolStatics.*;

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

    Connection rawConn;//maybe from XAConnection
    XAResource rawXaRes;//from XAConnection
    volatile int state;
    volatile long lastAccessTime;
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
            List<String> sqlExceptionStateList) {

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
        this.defaultCatalogIsNotBlank = !isBlank(defaultCatalog);
        this.forceDirtyOnCatalogAfterSet = forceDirtyOnCatalogAfterSet;
        //5:defaultSchema
        this.enableDefaultOnSchema = enableDefaultOnSchema;
        this.defaultSchema = defaultSchema;
        this.defaultSchemaIsNotBlank = !isBlank(defaultSchema);
        this.forceDirtyOnSchemaAfterSet = forceDirtyOnSchemaAfterSet;
        //6:defaultNetworkTimeout
        this.supportNetworkTimeoutInd = supportNetworkTimeoutInd;
        this.defaultNetworkTimeout = defaultNetworkTimeout;
        this.networkTimeoutExecutor = networkTimeoutExecutor;
        //7:others
        this.sqlExceptionCodeList = sqlExceptionCodeList;
        this.sqlExceptionStateList = sqlExceptionStateList;

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
        p.lastAccessTime = System.currentTimeMillis();
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
        p.lastAccessTime = System.currentTimeMillis();
        return p;
    }

    //***************************************************************************************************************//
    //                                      1:connection recycle(call by proxy connection)                           //                                                                                  //
    //***************************************************************************************************************//

    /**
     * remove connection from pool,method called by {@link ProxyConnectionBase#abort}
     */
    final void removeSelf() {
        pool.abandonOnReturn(this, DESC_RM_ABORT);
    }

    /**
     * return borrowed connection to pool,method called by {@link ProxyConnectionBase#close}
     *
     * @throws SQLException when error occurs during recycle
     */
    final void recycleSelf() throws SQLException {
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
    final void onBeforeRemove() {
        try {
            this.state = CON_CLOSED;
            this.resetRawConn();
        } catch (Throwable e) {
            CommonLog.error("Connection close error", e);
        } finally {
            oclose(this.rawConn);
            this.rawXaRes = null;
        }
    }

    //***************************************************************************************************************//
    //                                     3:statement cache maintenance                                             //                                                                                  //
    //***************************************************************************************************************//
    final void registerStatement(ProxyStatementBase s) {
        if (this.openStmSize == this.openStatements.length) {//full
            ProxyStatementBase[] array = new ProxyStatementBase[this.openStmSize << 1];
            System.arraycopy(this.openStatements, 0, array, 0, this.openStmSize);
            this.openStatements = array;
        }
        this.openStatements[this.openStmSize++] = s;
    }

    final void unregisterStatement(ProxyStatementBase s) {
        for (int i = this.openStmSize - 1; i >= 0; i--) {
            if (s == this.openStatements[i]) {
                int m = this.openStmSize - i - 1;
                if (m > 0) System.arraycopy(this.openStatements, i + 1, this.openStatements, i, m);//move ahead
                this.openStatements[--this.openStmSize] = null; // clear to let GC do its work
                return;
            }
        }
    }

    final void clearStatement() {
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
    final void updateAccessTime() {//for update,insert.select,delete and so on DML
        this.commitDirtyInd = !this.curAutoCommit;
        this.lastAccessTime = System.currentTimeMillis();
    }

    void checkSQLException(SQLException e) {//Fatal error code check
        int code = e.getErrorCode();
        if (code != 0 && proxyInUsing != null && sqlExceptionCodeList != null && sqlExceptionCodeList.contains(code)) {
            CommonLog.error("Connection will be aborted from pool,because of sql exception code:{}", code);
            this.proxyInUsing.abort(null);//will remove from pool
            this.proxyInUsing = null;
            return;
        }

        String state = e.getSQLState();
        if (state != null && proxyInUsing != null && sqlExceptionStateList != null && sqlExceptionStateList.contains(state)) {
            CommonLog.error("Connection will be aborted from pool,because of sql exception state:{}", state);
            this.proxyInUsing.abort(null);//will remove from pool
            this.proxyInUsing = null;
        }
    }

    //***************************************************************************************************************//
    //                                     5:dirty record or reset                                                   //                                                                                  //
    //***************************************************************************************************************//
    final boolean supportNetworkTimeoutSet() {
        return this.supportNetworkTimeoutInd;
    }

    final void setResetInd(int i, boolean changed) {
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
