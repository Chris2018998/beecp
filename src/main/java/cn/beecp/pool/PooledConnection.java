/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp.pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ThreadPoolExecutor;

import static cn.beecp.pool.PoolStaticCenter.*;
import static java.lang.System.arraycopy;
import static java.lang.System.currentTimeMillis;

/**
 * Pooled Connection
 *
 * @author Chris.Liao
 * @version 1.0
 */
final class PooledConnection implements Cloneable {
    private static final boolean[] FALSE_ARRAY = new boolean[6];
    public final boolean defaultAutoCommit;
    public final boolean defaultReadOnly;
    public final String defaultCatalog;
    public final String defaultSchema;
    public final int defaultTransactionIsolation;
    public final int defaultNetworkTimeout;
    private final boolean defaultCatalogIsNotBlank;
    private final boolean defaultSchemaIsNotBlank;
    private final boolean supportNetworkTimeout;
    private final ThreadPoolExecutor networkTimeoutExecutor;
    private final FastConnectionPool pool;

    public ProxyConnectionBase proxyCon;
    public boolean commitDirtyInd;
    public boolean curAutoCommit;
    public int openStmSize;
    public Connection rawCon;
    public volatile int state;
    public volatile long lastAccessTime;
    private int resetCnt;// reset count
    private boolean[] resetInd;
    private ProxyStatementBase[] openStatements;

    public PooledConnection(FastConnectionPool pool,
                            boolean defaultAutoCommit,
                            boolean defaultReadOnly,
                            String defaultCatalog,
                            String defaultSchema,
                            int defaultTransactionIsolation,
                            boolean supportNetworkTimeout,
                            int defaultNetworkTimeout,
                            ThreadPoolExecutor networkTimeoutExecutor) {
        this.pool = pool;
        this.defaultAutoCommit = defaultAutoCommit;
        this.defaultReadOnly = defaultReadOnly;
        this.defaultCatalog = defaultCatalog;
        this.defaultSchema = defaultSchema;
        this.defaultTransactionIsolation = defaultTransactionIsolation;
        this.supportNetworkTimeout = supportNetworkTimeout;
        this.defaultNetworkTimeout = defaultNetworkTimeout;
        this.networkTimeoutExecutor = networkTimeoutExecutor;
        this.curAutoCommit = defaultAutoCommit;
        this.defaultCatalogIsNotBlank = !isBlank(defaultCatalog);
        this.defaultSchemaIsNotBlank = !isBlank(defaultSchema);
    }

    public final PooledConnection copy(Connection rawConn, int state) throws CloneNotSupportedException, SQLException {
        rawConn.setAutoCommit(defaultAutoCommit);
        rawConn.setTransactionIsolation(defaultTransactionIsolation);
        rawConn.setReadOnly(defaultReadOnly);
        if (defaultCatalogIsNotBlank)
            rawConn.setCatalog(defaultCatalog);
        if (defaultSchemaIsNotBlank)
            rawConn.setSchema(defaultSchema);
        PooledConnection p = (PooledConnection) clone();
        p.state = state;
        p.rawCon = rawConn;
        p.resetInd = new boolean[6];
        p.openStatements = new ProxyStatementBase[10];
        p.lastAccessTime = currentTimeMillis();//first time
        return p;
    }

    public boolean supportNetworkTimeout() {
        return supportNetworkTimeout;
    }

    public final void updateAccessTime() {//for update,insert.select,delete and so on DML
        commitDirtyInd = !curAutoCommit;
        lastAccessTime = currentTimeMillis();
    }

    //called by pool before remove from pool
    public final void onBeforeRemove() {
        try {
            state = CON_CLOSED;
            resetRawConn();
        } catch (Throwable e) {
            commonLog.error("Connection close error", e);
        } finally {
            oclose(rawCon);
        }
    }

    //***************called by connection proxy ********//
    public final void recycleSelf() throws SQLException {
        try {
            proxyCon = null;
            resetRawConn();
            pool.recycle(this);
        } catch (Throwable e) {
            pool.abandonOnReturn(this);
            throw (e instanceof SQLException) ? (SQLException) e : new SQLException(e);
        }
    }

    public final void setResetInd(int p, boolean c) {
        if (!resetInd[p] && c)//false ->true       +1
            resetCnt++;
        else if (resetInd[p] && !c)//true-->false  -1
            resetCnt--;
        resetInd[p] = c;
        //lastAccessTime=currentTimeMillis();
    }

    public final void resetRawConn() throws SQLException {
        if (commitDirtyInd) { //Roll back when commit dirty
            rawCon.rollback();
            commitDirtyInd = false;
        }
        //reset begin
        if (resetCnt > 0) {
            if (resetInd[0]) {//reset autoCommit
                rawCon.setAutoCommit(defaultAutoCommit);
                curAutoCommit = defaultAutoCommit;
            }
            if (resetInd[1])
                rawCon.setTransactionIsolation(defaultTransactionIsolation);
            if (resetInd[2]) //reset readonly
                rawCon.setReadOnly(defaultReadOnly);
            if (resetInd[3]) //reset catalog
                rawCon.setCatalog(defaultCatalog);
            //for JDK1.7 begin
            if (resetInd[4]) //reset schema
                rawCon.setSchema(defaultSchema);
            if (resetInd[5]) //reset networkTimeout
                rawCon.setNetworkTimeout(networkTimeoutExecutor, defaultNetworkTimeout);
            //for JDK1.7 end
            resetCnt = 0;
            arraycopy(FALSE_ARRAY, 0, resetInd, 0, 6);
        }//reset end
        //clear warnings
        rawCon.clearWarnings();
    }

    //****************below are some statement trace methods***************************/
    public final void registerStatement(ProxyStatementBase s) {
        if (openStmSize == openStatements.length) {//full
            ProxyStatementBase[] newArray = new ProxyStatementBase[openStmSize << 1];
            arraycopy(openStatements, 0, newArray, 0, openStmSize);
            openStatements = newArray;
        }
        openStatements[openStmSize++] = s;
    }

    public final void unregisterStatement(ProxyStatementBase s) {
        for (int i = 0; i < openStmSize; i++)
            if (s == openStatements[i]) {
                int m = openStmSize - i - 1;
                if (m > 0) arraycopy(openStatements, i + 1, openStatements, i, m);//move to ahead
                openStatements[--openStmSize] = null; // clear to let GC do its work
                return;
            }
    }

    public final void clearStatement() {
        for (int i = 0; i < openStmSize; i++) {
            ProxyStatementBase s = openStatements[i];
            if (s != null) {
                openStatements[i] = null;
                s.registered = false;
                oclose(s);
            }
        }
        openStmSize = 0;
    }
}