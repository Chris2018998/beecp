/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp.pool;

import cn.beecp.BeeDataSourceConfig;

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
class PooledConnection implements Cloneable{
    private static final boolean[] FALSE_ARRAY = new boolean[6];
    final boolean defaultAutoCommit;
    final boolean defaultReadOnly;
    final String defaultCatalog;
    final String defaultSchema;
    final int defaultTransactionIsolation;
    final int defaultNetworkTimeout;

    Connection rawCon;
    volatile int state;
    ProxyConnectionBase proxyCon;
    volatile long lastAccessTime;
    boolean commitDirtyInd;
    boolean curAutoCommit;
    int openStmSize;
    private int resetCnt;// reset count
    private boolean[] resetInd;
    private ProxyStatementBase[] openStatements;
    private ThreadPoolExecutor networkTimeoutExecutor;
    private FastConnectionPool pool;

    public PooledConnection(FastConnectionPool pool, BeeDataSourceConfig config) {
        this.pool = pool;
        this.defaultReadOnly = config.isDefaultReadOnly();
        this.defaultCatalog = config.getDefaultCatalog();
        this.defaultSchema = config.getDefaultSchema();
        this.defaultAutoCommit = config.isDefaultAutoCommit();
        this.defaultTransactionIsolation = config.getDefaultTransactionIsolationCode();
        this.curAutoCommit = defaultAutoCommit;
        this.defaultNetworkTimeout = pool.getNetworkTimeout();
        this.networkTimeoutExecutor = pool.getNetworkTimeoutExecutor();
    }

    public final PooledConnection clone()throws CloneNotSupportedException  {
        return (PooledConnection) super.clone();
    }

    final void fillRawConnection(Connection rawConn, int state) {
        this.state = state;
        this.rawCon = rawConn;
        this.openStatements = new ProxyStatementBase[10];
        this.resetInd = new boolean[FALSE_ARRAY.length];
        lastAccessTime = currentTimeMillis();//first tim
    }

    boolean supportNetworkTimeout() {
        return pool.supportNetworkTimeout();
    }

    final void updateAccessTime() {//for update,insert.select,delete and so on DML
        commitDirtyInd = !curAutoCommit;
        lastAccessTime = currentTimeMillis();
    }

    //called by pool before remove from pool
    final void onBeforeRemove() {
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
    final void recycleSelf() throws SQLException {
        try {
            proxyCon = null;
            resetRawConn();
            pool.recycle(this);
        } catch (Throwable e) {
            pool.abandonOnReturn(this);
            throw (e instanceof SQLException) ? (SQLException) e : new SQLException(e);
        }
    }

    final void setResetInd(int p, boolean chgd) {
        if (!resetInd[p] && chgd)//false ->true       +1
            resetCnt++;
        else if (resetInd[p] && !chgd)//true-->false  -1
            resetCnt--;
        resetInd[p] = chgd;
        //lastAccessTime=currentTimeMillis();
    }

    final void resetRawConn() throws SQLException {
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
    final void registerStatement(ProxyStatementBase s) {
        if (openStmSize == openStatements.length) {//full
            ProxyStatementBase[] newArray = new ProxyStatementBase[openStatements.length << 1];
            arraycopy(openStatements, 0, newArray, 0, openStatements.length);
            openStatements = newArray;
        }
        openStatements[openStmSize++] = s;
    }

    final void unregisterStatement(ProxyStatementBase s) {
        for (int i = 0; i < openStmSize; i++)
            if (s == openStatements[i]) {
                int m = openStmSize - i - 1;
                if (m > 0) arraycopy(openStatements, i + 1, openStatements, i, m);//move to ahead
                openStatements[--openStmSize] = null; // clear to let GC do its work
                return;
            }
    }

    final void clearStatement() {
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