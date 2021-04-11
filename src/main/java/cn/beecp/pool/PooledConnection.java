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

import static cn.beecp.pool.PoolStaticCenter.commonLog;
import static cn.beecp.pool.PoolStaticCenter.oclose;
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
    volatile int state;
    Connection rawConn;
    ProxyConnectionBase proxyConn;
    volatile long lastAccessTime;
    boolean commitDirtyInd;
    boolean curAutoCommit;
    boolean defaultAutoCommit;
    int defaultTransactionIsolationCode;
    boolean defaultReadOnly;
    String defaultCatalog;
    String defaultSchema;
    int defaultNetworkTimeout;
    int statementPos;
    private ThreadPoolExecutor defaultNetworkTimeoutExecutor;
    private FastConnectionPool pool;
    private int resetCnt;// reset count
    private boolean[] resetInd = new boolean[FALSE_ARRAY.length];
    private ProxyStatementBase[] statements;

    public PooledConnection(Connection rawConn, int connState, FastConnectionPool connPool, BeeDataSourceConfig config) {
        pool = connPool;
        state = connState;
        this.rawConn = rawConn;

        //default value
        defaultTransactionIsolationCode = config.getDefaultTransactionIsolationCode();
        defaultReadOnly = config.isDefaultReadOnly();
        defaultCatalog = config.getDefaultCatalog();
        defaultSchema = config.getDefaultSchema();
        defaultNetworkTimeout = pool.getNetworkTimeout();
        defaultNetworkTimeoutExecutor = pool.getNetworkTimeoutExecutor();
        defaultAutoCommit = config.isDefaultAutoCommit();
        curAutoCommit = defaultAutoCommit;
        //default value

        statements = new ProxyStatementBase[10];
        lastAccessTime = currentTimeMillis();//first time
    }

    //close raw connection
    void closeRawConn() {//called by pool
        try {
            resetRawConnOnReturn();
        } catch (Throwable e) {
            commonLog.error("Connection close error", e);
        } finally {
            oclose(rawConn);
        }
    }

    //***************called by connection proxy ********//
    final void recycleSelf() throws SQLException {
        try {
            proxyConn = null;
            resetRawConnOnReturn();
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

    boolean supportIsValid() {
        return pool.supportIsValid();
    }

    boolean supportSchema() {
        return pool.supportSchema();
    }

    boolean supportNetworkTimeout() {
        return pool.supportNetworkTimeout();
    }

    final void resetRawConnOnReturn() throws SQLException {
        if (!curAutoCommit && commitDirtyInd) { //Roll back when commit dirty
            rawConn.rollback();
            commitDirtyInd = false;
        }

        //reset begin
        if (resetCnt > 0) {
            if (resetInd[0]) {//reset autoCommit
                rawConn.setAutoCommit(defaultAutoCommit);
                curAutoCommit = defaultAutoCommit;
            }
            if (resetInd[1])
                rawConn.setTransactionIsolation(defaultTransactionIsolationCode);
            if (resetInd[2]) //reset readonly
                rawConn.setReadOnly(defaultReadOnly);
            if (resetInd[3]) //reset catalog
                rawConn.setCatalog(defaultCatalog);

            //for JDK1.7 begin
            if (resetInd[4]) //reset schema
                rawConn.setSchema(defaultSchema);
            if (resetInd[5]) //reset networkTimeout
                rawConn.setNetworkTimeout(defaultNetworkTimeoutExecutor, defaultNetworkTimeout);
            //for JDK1.7 end

            resetCnt = 0;
            arraycopy(FALSE_ARRAY, 0, resetInd, 0, 6);
        }//reset end

        //clear warnings
        rawConn.clearWarnings();
    }

    //****************below are some statement trace methods***************************/
    final void registerStatement(ProxyStatementBase e) {
        if (statementPos == statements.length) {
            ProxyStatementBase[] newArray = new ProxyStatementBase[statements.length << 1];
            System.arraycopy(statements, 0, newArray, 0, statements.length);
            statements = newArray;
        }
        statements[statementPos++] = e;
    }

    final void unregisterStatement(ProxyStatementBase e) {
        for (int i = 0; i < statementPos; i++)
            if (e == statements[i]) {
                int m = statementPos - i - 1;
                if (m > 0) System.arraycopy(statements, i + 1, statements, i, m);//move to ahead
                statements[--statementPos] = null; // clear to let GC do its work
                return;
            }
    }

    final void clearStatement() {
        // if (pos > 0) {
        for (int i = 0; i < statementPos; i++) {
            if (statements[i] != null) {
                statements[i].setAsClosed();
                statements[i] = null;// clear to let GC do its work
            }
        }
        statementPos = 0;
        //}
    }
}