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
class PooledConnection {
    private static final boolean[] FALSE_ARRAY = new boolean[6];
    final boolean cfgAutoCommit;
    final int cfgTransactionIsolationCode;
    final boolean cfgReadOnly;
    final String cfgCatalog;
    final String cfgSchema;
    final int cfgNetworkTimeout;
    private final ThreadPoolExecutor networkTimeoutExecutor;
    private final FastConnectionPool pool;
    private final boolean[] resetInd = new boolean[FALSE_ARRAY.length];

    volatile int state;
    Connection rawCon;
    ProxyConnectionBase proxyCon;
    volatile long lastAccessTime;
    boolean commitDirtyInd;
    boolean curAutoCommit;
    int openStmSize;
    private int resetCnt;// reset count
    private ProxyStatementBase[] openStatements;

    public PooledConnection(Connection rawConn, int connState, FastConnectionPool connPool, BeeDataSourceConfig config) {
        pool = connPool;
        state = connState;
        this.rawCon = rawConn;
        //default value
        cfgTransactionIsolationCode = config.getDefaultTransactionIsolationCode();
        cfgReadOnly = config.isDefaultReadOnly();
        cfgCatalog = config.getDefaultCatalog();
        cfgSchema = config.getDefaultSchema();
        cfgNetworkTimeout = pool.getNetworkTimeout();
        networkTimeoutExecutor = pool.getNetworkTimeoutExecutor();
        cfgAutoCommit = config.isDefaultAutoCommit();
        curAutoCommit = cfgAutoCommit;
        //default value
        openStatements = new ProxyStatementBase[10];
        lastAccessTime = currentTimeMillis();//first time
    }

    //called by pool before remove from pool
    final void onBeforeRemove() {
        try {
            this.state = CON_CLOSED;
            if (proxyCon != null) {
                proxyCon.setAsClosed();
                proxyCon = null;
            }
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

    final void updateAccessTime() {//for update,insert.select,delete and so on DML
        commitDirtyInd = !curAutoCommit;
        lastAccessTime = currentTimeMillis();
    }

    final void setResetInd(int p, boolean chgd) {
        if (!resetInd[p] && chgd)//false ->true       +1
            resetCnt++;
        else if (resetInd[p] && !chgd)//true-->false  -1
            resetCnt--;
        resetInd[p] = chgd;
        //lastAccessTime=currentTimeMillis();
    }

    boolean supportNetworkTimeout() {
        return pool.supportNetworkTimeout();
    }

    final void resetRawConn() throws SQLException {
        if (!curAutoCommit && commitDirtyInd) { //Roll back when commit dirty
            rawCon.rollback();
            commitDirtyInd = false;
        }
        //reset begin
        if (resetCnt > 0) {
            if (resetInd[0]) {//reset autoCommit
                rawCon.setAutoCommit(cfgAutoCommit);
                curAutoCommit = cfgAutoCommit;
            }
            if (resetInd[1])
                rawCon.setTransactionIsolation(cfgTransactionIsolationCode);
            if (resetInd[2]) //reset readonly
                rawCon.setReadOnly(cfgReadOnly);
            if (resetInd[3]) //reset catalog
                rawCon.setCatalog(cfgCatalog);
            //for JDK1.7 begin
            if (resetInd[4]) //reset schema
                rawCon.setSchema(cfgSchema);
            if (resetInd[5]) //reset networkTimeout
                rawCon.setNetworkTimeout(networkTimeoutExecutor, cfgNetworkTimeout);
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
            if (openStatements[i] != null) {
                openStatements[i].setAsClosed();
                openStatements[i] = null;// clear to let GC do its work
            }
        }
        openStmSize = 0;
    }
}