/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.jta;

import cn.beecp.BeeDataSource;

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
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static javax.transaction.Status.STATUS_ACTIVE;

/**
 * DataSource implementation for jta
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class BeeJtaDataSource extends TimerTask implements DataSource {
    private final Timer transactionTimer = new Timer(true);
    private final ConcurrentHashMap<Transaction, Connection> transactionMap = new ConcurrentHashMap<Transaction, Connection>(10);
    private BeeDataSource ds;
    private TransactionManager tm;

    public BeeJtaDataSource() {
        this(null, null);
    }

    public BeeJtaDataSource(BeeDataSource ds, TransactionManager tm) {
        this.ds = ds;
        this.tm = tm;
        transactionTimer.schedule(this, 0, 6000);
    }

    public void setDataSource(BeeDataSource ds) {
        this.ds = ds;
    }

    public void setTransactionManager(TransactionManager tm) {
        this.tm = tm;
    }

    public Connection getConnection() throws SQLException {
        if (ds == null) throw new SQLException("dataSource not set");
        if (tm == null) throw new SQLException("transactionManager not set");

        //step1: try to get connection by transaction
        Transaction transaction;
        try {
            transaction = tm.getTransaction();
            int statusCode = transaction.getStatus();
            if (STATUS_ACTIVE != statusCode)
                throw new SQLException("Current transaction status code is not expect value:" + statusCode);
            Connection conn = transactionMap.get(transaction);
            if (conn != null) return conn;
        } catch (Throwable e) {
            throw e instanceof SQLException ? (SQLException) e : new SQLException(e);
        }

        //step2: try to get connection by XAConnection
        XAConnection xaConn = null;
        try {
            xaConn = ds.getXAConnection();
            Connection conn = xaConn.getConnection();
            if (transaction.enlistResource(xaConn.getXAResource())) {
                transactionMap.put(transaction, conn);
                Synchronization synchronization = new BeeJtaSynchronization(transaction, transactionMap);
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
        Iterator<Map.Entry<Transaction, Connection>> iterator = transactionMap.entrySet().iterator();
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

            }
        }
    }

    public Connection getConnection(String username, String password) throws SQLException {
        throw new SQLFeatureNotSupportedException("Not supported");
    }

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
}
