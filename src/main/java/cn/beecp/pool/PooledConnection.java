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
    private static final boolean[] FALSE = new boolean[6];
    public final boolean defAutoCommit;
    public final boolean defReadOnly;
    public final String defCatalog;
    public final String defSchema;
    public final int defTransactionIsolation;
    public final int defNetworkTimeout;
    private final FastConnectionPool pool;
    private final boolean defCatalogSetInd;
    private final boolean defSchemaSetInd;
    private final boolean supportNetworkTimeout;
    private final ThreadPoolExecutor networkTimeoutExecutor;
    public boolean curAutoCommit;
    public boolean commitDirtyInd;
    public ProxyConnectionBase proxyCon;

    public Connection raw;
    public volatile int state;
    public volatile long lastAccessTime;
    public int openStmSize;
    private int resetCnt;// reset count
    private boolean[] resetInd;
    private ProxyStatementBase[] openStatements;

    public PooledConnection(FastConnectionPool pool,
                            boolean defAutoCommit,
                            boolean defReadOnly,
                            String defCatalog,
                            String defSchema,
                            int defTransactionIsolation,
                            boolean supportNetworkTimeout,
                            int defNetworkTimeout,
                            ThreadPoolExecutor networkTimeoutExecutor) {
        this.pool = pool;
        this.defAutoCommit = defAutoCommit;
        this.defReadOnly = defReadOnly;
        this.defCatalog = defCatalog;
        this.defSchema = defSchema;
        this.defTransactionIsolation = defTransactionIsolation;
        this.defNetworkTimeout = defNetworkTimeout;
        this.supportNetworkTimeout = supportNetworkTimeout;
        this.networkTimeoutExecutor = networkTimeoutExecutor;
        this.defCatalogSetInd = !isBlank(defCatalog);
        this.defSchemaSetInd = !isBlank(defSchema);
        this.curAutoCommit = defAutoCommit;
    }

    public final PooledConnection copy(Connection raw, int state) throws CloneNotSupportedException, SQLException {
        raw.setAutoCommit(defAutoCommit);
        raw.setTransactionIsolation(defTransactionIsolation);
        raw.setReadOnly(defReadOnly);
        if (defCatalogSetInd)
            raw.setCatalog(defCatalog);
        if (defSchemaSetInd)
            raw.setSchema(defSchema);

        PooledConnection p = (PooledConnection) clone();
        p.state = state;
        p.raw = raw;
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
            oclose(raw);
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
            throw e instanceof SQLException ? (SQLException) e : new SQLException(e);
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
            raw.rollback();
            commitDirtyInd = false;
        }
        //reset begin
        if (resetCnt > 0) {
            if (resetInd[0]) {//reset autoCommit
                raw.setAutoCommit(defAutoCommit);
                curAutoCommit = defAutoCommit;
            }
            if (resetInd[1])
                raw.setTransactionIsolation(defTransactionIsolation);
            if (resetInd[2]) //reset readonly
                raw.setReadOnly(defReadOnly);
            if (resetInd[3]) //reset catalog
                raw.setCatalog(defCatalog);
            //for JDK1.7 begin
            if (resetInd[4]) //reset schema
                raw.setSchema(defSchema);
            if (resetInd[5]) //reset networkTimeout
                raw.setNetworkTimeout(networkTimeoutExecutor, defNetworkTimeout);
            //for JDK1.7 end
            resetCnt = 0;
            arraycopy(FALSE, 0, resetInd, 0, 6);
        }//reset end
        //clear warnings
        raw.clearWarnings();
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