/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.jta;

import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeMethodLog;
import org.stone.beecp.BeeMethodLogListener;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static javax.transaction.Status.STATUS_ACTIVE;


/**
 * DataSource implementation for jta
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeJtaDataSource extends TimerTask implements DataSource, AutoCloseable {
    private final Timer transactionTimer = new Timer(true);
    private final ConcurrentHashMap<Transaction, Connection> transactionMap = new ConcurrentHashMap<>(10);
    private BeeDataSource ds;
    private TransactionManager tm;

    public BeeJtaDataSource() {
        this(null, null);
    }

    public BeeJtaDataSource(BeeDataSource ds, TransactionManager tm) {
        this.ds = ds;
        this.tm = tm;
        this.transactionTimer.schedule(this, 0, 6000);
    }

    public void setDataSource(BeeDataSource ds) {
        this.ds = ds;
    }

    public void setTransactionManager(TransactionManager tm) {
        this.tm = tm;
    }

    //***************************************************************************************************************//
    //                                         2: Pooled connections get                                             //
    //***************************************************************************************************************//
    public Connection getConnection(String username, String password) throws SQLException {
        checkDataSource();
        return this.ds.getConnection();
    }

    public Connection getConnection() throws SQLException {
        checkDataSource();
        if (this.tm == null) throw new SQLException("transactionManager not set");

        //step1: try to get connection by transaction
        Transaction transaction;
        try {
            transaction = this.tm.getTransaction();
            int statusCode = transaction.getStatus();
            if (STATUS_ACTIVE != statusCode)
                throw new SQLException("Current transaction status code is not expect value:" + statusCode);
            Connection conn = this.transactionMap.get(transaction);
            if (conn != null) return conn;
        } catch (SQLException e) {
            throw e;
        } catch (Throwable e) {
            throw new SQLException(e);
        }

        //step2: try to get connection by XAConnection
        XAConnection xaConn = null;
        try {
            xaConn = this.ds.getXAConnection();
            Connection conn = xaConn.getConnection();
            if (transaction.enlistResource(xaConn.getXAResource())) {
                this.transactionMap.put(transaction, conn);
                Synchronization synchronization = new BeeJtaSynchronization(transaction, this.transactionMap);
                transaction.registerSynchronization(synchronization);
                return conn;
            } else {
                throw new SQLException("Failed to enlist resource in transaction");
            }
        } catch (Throwable e) {
            if (xaConn != null) xaConn.close();//let connection return to pool
            throw e instanceof SQLException ? (SQLException) e : new SQLException(e);
        }
    }

    public void run() {
        Iterator<Map.Entry<Transaction, Connection>> iterator = this.transactionMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Transaction, Connection> entry = iterator.next();
            Transaction transaction = entry.getKey();

            try {
                int statusCode = transaction.getStatus();
                if (statusCode == Status.STATUS_COMMITTED || statusCode == Status.STATUS_ROLLING_BACK) {
                    iterator.remove();
                    entry.getValue().close();
                }
            } catch (Throwable e) {
                //do nothing
            }
        }
    }

    private void checkDataSource() throws SQLException {
        if (this.ds == null || ds.isClosed()) throw new SQLException("Inner data source not set or has been closed");
    }

    //***************************************************************************************************************//
    //                                         3: Inner Data source restart(2)                                       //
    //***************************************************************************************************************//
    public void restart() throws SQLException {
        restart(false);
    }

    public void restart(boolean force) throws SQLException {
        checkDataSource();
        this.ds.restart(force);
    }

    //****************************************************************************************************************//
    //                                         4: Override methods                                                    //
    //****************************************************************************************************************//
    public PrintWriter getLogWriter() throws SQLException {
        throw new SQLFeatureNotSupportedException("Not supported");
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new SQLFeatureNotSupportedException("Not supported");
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("Not supported");
    }

    public int getLoginTimeout() throws SQLException {
        throw new SQLFeatureNotSupportedException("Not supported");
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        throw new SQLFeatureNotSupportedException("Not supported");
    }

    public boolean isWrapperFor(Class<?> clazz) {
        return clazz != null && clazz.isInstance(this);
    }

    public <T> T unwrap(Class<T> clazz) throws SQLException {
        if (clazz != null && clazz.isInstance(this))
            return clazz.cast(this);
        else
            throw new SQLException("Wrapped object was not an instance of " + clazz);
    }

    public BeeConnectionPoolMonitorVo getPoolMonitorVo() throws SQLException {
        checkDataSource();
        return ds.getPoolMonitorVo();
    }

    //***************************************************************************************************************//
    //                                         5: runtime logs print(1)                                              //
    //***************************************************************************************************************//
    public void enableLogPrinter(boolean printRuntimeLog) throws SQLException {
        checkDataSource();
        ds.enableLogPrinter(printRuntimeLog);
    }

    //***************************************************************************************************************//
    //                                         6: Method Execution Log Cache(4)                                      //
    //***************************************************************************************************************//
    public void enableLogCache(boolean enable) throws SQLException {
        checkDataSource();
        this.ds.enableLogCache(enable);
    }

    public void changeLogListener(BeeMethodLogListener listener) throws SQLException {
        checkDataSource();
        this.ds.changeLogListener(listener);
    }

    public List<BeeMethodLog> getLogs(int type) throws SQLException {
        checkDataSource();
        return this.ds.getLogs(type);
    }

    public void clearLogs(int type) throws SQLException {
        checkDataSource();
        this.ds.clearLogs(type);
    }

    public boolean cancelStatement(String logId) throws SQLException {
        checkDataSource();
        return this.ds.cancelStatement(logId);
    }

    //***************************************************************************************************************//
    //                                         7: close methods(2)                                                   //
    //***************************************************************************************************************//
    public boolean isClosed() throws SQLException {
        checkDataSource();
        return ds.isClosed();
    }

    public void close() throws SQLException {
        checkDataSource();
        this.ds.close();
        this.transactionTimer.cancel();
    }
}
