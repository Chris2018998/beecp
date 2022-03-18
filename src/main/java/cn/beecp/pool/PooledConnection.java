/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool;

import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ThreadPoolExecutor;

import static cn.beecp.pool.PoolStaticCenter.*;

/**
 * Pooled Connection
 *
 * @author Chris.Liao
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
    private final boolean defaultCatalogIsNotBlank;
    private final boolean defaultSchemaIsNotBlank;
    private final boolean supportNetworkTimeoutInd;
    private final ThreadPoolExecutor networkTimeoutExecutor;
    private final FastConnectionPool pool;

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
            boolean defaultAutoCommit,
            int defaultTransactionIsolation,
            boolean defaultReadOnly,
            String defaultCatalog,
            String defaultSchema,
            int defaultNetworkTimeout,
            boolean supportNetworkTimeoutInd,
            ThreadPoolExecutor networkTimeoutExecutor,
            FastConnectionPool pool) {

        this.defaultAutoCommit = defaultAutoCommit;
        this.defaultTransactionIsolation = defaultTransactionIsolation;
        this.defaultReadOnly = defaultReadOnly;
        this.defaultCatalog = defaultCatalog;
        this.defaultSchema = defaultSchema;
        this.defaultNetworkTimeout = defaultNetworkTimeout;
        this.defaultCatalogIsNotBlank = !isBlank(defaultCatalog);
        this.defaultSchemaIsNotBlank = !isBlank(defaultSchema);
        this.supportNetworkTimeoutInd = supportNetworkTimeoutInd;
        this.networkTimeoutExecutor = networkTimeoutExecutor;
        this.pool = pool;
        this.curAutoCommit = defaultAutoCommit;
    }

    PooledConnection setDefaultAndCopy(Connection rawConn, int state, XAResource rawXaRes) throws SQLException, CloneNotSupportedException {
        if (defaultAutoCommit != rawConn.getAutoCommit())
            rawConn.setAutoCommit(defaultAutoCommit);
        if (defaultTransactionIsolation != rawConn.getTransactionIsolation())
            rawConn.setTransactionIsolation(defaultTransactionIsolation);
        if (defaultReadOnly != rawConn.isReadOnly())
            rawConn.setReadOnly(defaultReadOnly);
        if (defaultCatalogIsNotBlank && !defaultCatalog.equals(rawConn.getCatalog()))
            rawConn.setCatalog(defaultCatalog);
        if (defaultSchemaIsNotBlank && !defaultSchema.equals(rawConn.getSchema()))
            rawConn.setSchema(defaultSchema);

        PooledConnection p = (PooledConnection) super.clone();
        p.state = state;
        p.rawConn = rawConn;
        p.rawXaRes = rawXaRes;
        p.resetFlags = new boolean[6];
        p.openStatements = new ProxyStatementBase[10];
        p.lastAccessTime = System.currentTimeMillis();//first time
        return p;
    }

    boolean supportNetworkTimeoutSet() {
        return supportNetworkTimeoutInd;
    }

    void updateAccessTime() {//for update,insert.select,delete and so on DML
        commitDirtyInd = !curAutoCommit;
        lastAccessTime = System.currentTimeMillis();
    }

    //called by pool before remove from pool
    void onBeforeRemove() {
        try {
            state = CON_CLOSED;
            resetRawConn();
        } catch (Throwable e) {
            CommonLog.error("Connection close error", e);
        } finally {
            oclose(rawConn);
            rawXaRes = null;
            //rawXaConn = null;
        }
    }

    //***************called by connection proxy ********//
    void recycleSelf() throws SQLException {
        try {
            proxyInUsing = null;
            resetRawConn();
            pool.recycle(this);
        } catch (Throwable e) {
            pool.abandonOnReturn(this);
            throw e instanceof SQLException ? (SQLException) e : new SQLException(e);
        }
    }

    void setResetInd(int i, boolean changed) {
        if (resetFlags[i] != changed) {
            resetFlags[i] = changed;
            resetCnt += changed ? 1 : -1;
        }
    }

    private void resetRawConn() throws SQLException {
        if (commitDirtyInd) { //Roll back when commit dirty
            rawConn.rollback();
            commitDirtyInd = false;
        }
        //reset begin
        if (resetCnt > 0) {
            if (resetFlags[PS_AUTO]) {//reset autoCommit
                rawConn.setAutoCommit(defaultAutoCommit);
                curAutoCommit = defaultAutoCommit;
            }
            if (resetFlags[PS_TRANS])
                rawConn.setTransactionIsolation(defaultTransactionIsolation);
            if (resetFlags[PS_READONLY]) //reset readonly
                rawConn.setReadOnly(defaultReadOnly);
            if (defaultCatalogIsNotBlank && resetFlags[PS_CATALOG]) //reset catalog
                rawConn.setCatalog(defaultCatalog);

            //for JDK1.7 begin
            if (defaultSchemaIsNotBlank && resetFlags[PS_SCHEMA]) //reset schema
                rawConn.setSchema(defaultSchema);
            if (resetFlags[PS_NETWORK]) //reset networkTimeout
                rawConn.setNetworkTimeout(networkTimeoutExecutor, defaultNetworkTimeout);
            //for JDK1.7 end
            resetCnt = 0;
            System.arraycopy(FALSE, 0, resetFlags, 0, 6);
        }//reset end
        //clear warnings
        rawConn.clearWarnings();
    }

    //****************below are some statement trace methods***************************/
    void registerStatement(ProxyStatementBase s) {
        if (openStmSize == openStatements.length) {//full
            ProxyStatementBase[] array = new ProxyStatementBase[openStmSize << 1];
            System.arraycopy(openStatements, 0, array, 0, openStmSize);
            openStatements = array;
        }
        openStatements[openStmSize++] = s;
    }

    void unregisterStatement(ProxyStatementBase s) {
        for (int i = openStmSize - 1; i >= 0; i--) {
            if (s == openStatements[i]) {
                int m = openStmSize - i - 1;
                if (m > 0) System.arraycopy(openStatements, i + 1, openStatements, i, m);//move ahead
                openStatements[--openStmSize] = null; // clear to let GC do its work
                return;
            }
        }
    }

    void clearStatement() {
        for (int i = 0; i < openStmSize; i++) {
            ProxyStatementBase s = openStatements[i];
            if (s != null) {
                s.registered = false;
                openStatements[i] = null;
                oclose(s);
            }
        }
        openStmSize = 0;
    }
}