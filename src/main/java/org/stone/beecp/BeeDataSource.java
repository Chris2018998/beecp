/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp;

import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;
import org.stone.beecp.pool.exception.PoolCreateFailedException;
import org.stone.beecp.pool.exception.PoolNotCreatedException;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.stone.beecp.pool.ConnectionPoolStatics.*;

/**
 * A entrance object to operation on connection pool
 * <p>
 * Email: Chris2018998@tom.com
 * Project: https://github.com/Chris2018998/stone
 *
 * @author Chris Liao
 * @version 1.0
 */
//fix BeeCP-Starter-#6 Chris-2020-09-01 start
//public final class BeeDataSource extends BeeDataSourceConfig implements DataSource {
public class BeeDataSource extends BeeDataSourceConfig implements DataSource, XADataSource {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private long maxWaitNanos = SECONDS.toNanos(8);//default vale same to config
    private BeeConnectionPool pool;
    private CommonDataSource subDs;//used to set loginTimeout
    private boolean ready;//true,means that inner pool has created
    private SQLException cause;//inner pool create failed cause

    //***************************************************************************************************************//
    //                                             1:constructors(3)                                                 //
    //***************************************************************************************************************//
    public BeeDataSource() {
    }

    public BeeDataSource(String driver, String url, String user, String password) {
        super(driver, url, user, password);
    }

    public BeeDataSource(BeeDataSourceConfig config) {
        try {
            config.copyTo(this);
            BeeDataSource.createPool(this);
            this.maxWaitNanos = MILLISECONDS.toNanos(config.getMaxWait());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createPool(BeeDataSource ds) throws SQLException {
        try {
            Class<?> poolClass = Class.forName(ds.getPoolImplementClassName());
            BeeConnectionPool pool = (BeeConnectionPool) createClassInstance(poolClass, BeeConnectionPool.class, "pool");
            pool.init(ds);
            ds.pool = pool;

            Object connectionFactory = ds.getConnectionFactory();
            if (connectionFactory instanceof CommonDataSource)
                ds.subDs = (CommonDataSource) connectionFactory;
            else
                ds.subDs = Dummy_CommonDataSource;

            //timeout on connect to db
            if (ds.getConnectTimeout() > 0) ds.subDs.setLoginTimeout(ds.getConnectTimeout());
            ds.ready = true;
        } catch (SQLException e) {
            throw e;
        } catch (Throwable e) {
            throw new PoolCreateFailedException("Failed to create connection pool by class:" + ds.getPoolImplementClassName(), e);
        }
    }

    //***************************************************************************************************************//
    //                                          2: below are override methods(11)                                    //
    //***************************************************************************************************************//
    public final Connection getConnection() throws SQLException {
        if (this.ready) return pool.getConnection();
        return createPoolByLock().getConnection();
    }

    public final XAConnection getXAConnection() throws SQLException {
        if (this.ready) return pool.getXAConnection();
        return createPoolByLock().getXAConnection();
    }

    private BeeConnectionPool createPoolByLock() throws SQLException {
        if (!lock.isWriteLocked() && lock.writeLock().tryLock()) {
            try {
                if (!ready) {
                    cause = null;
                    createPool(this);
                }
            } catch (SQLException e) {
                cause = e;
            } finally {
                lock.writeLock().unlock();
            }
        } else {
            try {
                if (!this.readLock.tryLock(maxWaitNanos, TimeUnit.NANOSECONDS))
                    throw new ConnectionGetTimeoutException("Connection get timeout at lock of pool creation");
            } catch (InterruptedException e) {
                throw new ConnectionGetInterruptedException("Connection get request interrupted at lock of pool creation");
            }
            readLock.unlock();
        }

        //read lock will reach
        if (cause != null) throw cause;
        return pool;
    }

    public Connection getConnection(String user, String password) throws SQLException {
        //throw new SQLFeatureNotSupportedException("Not support");
        CommonLog.warn("getConnection (user,password) ignores authentication - returning default connection");
        return getConnection();
    }

    public XAConnection getXAConnection(String user, String password) throws SQLException {
        //throw new SQLFeatureNotSupportedException("Not support");
        CommonLog.warn("getXAConnection (user,password) ignores authentication - returning default XAConnection");
        return getXAConnection();
    }

    //***************************************************************************************************************//
    //                                      Override methods from CommonDataSource                                   //
    //***************************************************************************************************************//
    public PrintWriter getLogWriter() throws SQLException {
        return subDs.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        subDs.setLogWriter(out);
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return subDs.getParentLogger();
    }

    public int getLoginTimeout() throws SQLException {
        return subDs.getLoginTimeout();
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        subDs.setLoginTimeout(seconds);
    }
    //******************************************************** Override End ******************************************//

    public boolean isWrapperFor(Class<?> clazz) {
        return clazz != null && clazz.isInstance(this);
    }

    public <T> T unwrap(Class<T> clazz) throws SQLException {
        if (clazz != null && clazz.isInstance(this))
            return clazz.cast(this);
        else
            throw new SQLException("Wrapped object was not an instance of " + clazz);
    }

    //***************************************************************************************************************//
    //                                     3: below are self-define methods(7)                                       //
    //***************************************************************************************************************//
    public boolean isClosed() {
        return this.pool == null || this.pool.isClosed();
    }

    public void close() {
        if (this.pool != null) {
            try {
                this.pool.close();
            } catch (Throwable e) {
                CommonLog.error("Error at closing connection pool", e);
            }
        }
    }

    //override method
    public void setMaxWait(long maxWait) {
        if (maxWait > 0) {
            super.setMaxWait(maxWait);
            this.maxWaitNanos = MILLISECONDS.toNanos(maxWait);
        }
    }

    public void setPrintRuntimeLog(boolean printRuntimeLog) {
        if (this.pool != null) this.pool.setPrintRuntimeLog(printRuntimeLog);
    }

    public BeeConnectionPoolMonitorVo getPoolMonitorVo() throws SQLException {
        if (this.pool == null) throw new PoolNotCreatedException("Connection pool not initialized");
        return this.pool.getPoolMonitorVo();
    }

    public void clear(boolean forceCloseUsing) throws SQLException {
        if (this.pool == null) throw new PoolNotCreatedException("Connection pool not initialized");
        this.pool.clear(forceCloseUsing);
    }

    public long getElapsedTimeSinceCreationLock() throws SQLException {
        if (this.pool == null) throw new PoolNotCreatedException("Connection pool not initialized");
        return this.pool.getElapsedTimeSinceCreationLock();
    }

    public void interruptThreadsOnCreationLock() throws SQLException {
        if (this.pool == null) throw new PoolNotCreatedException("Connection pool not initialized");
        this.pool.interruptThreadsOnCreationLock();
    }

    public void clear(boolean forceCloseUsing, BeeDataSourceConfig config) throws SQLException {
        if (this.pool == null) throw new PoolNotCreatedException("Connection pool not initialized");
        if (config == null) throw new BeeDataSourceConfigException("Connection pool config can't be null");
        this.pool.clear(forceCloseUsing, config);
        config.copyTo(this);
        this.maxWaitNanos = MILLISECONDS.toNanos(config.getMaxWait());
    }
}
