/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp;

import cn.beecp.pool.ConnectionPool;
import cn.beecp.pool.ConnectionPoolMonitorVo;
import cn.beecp.pool.exception.PoolCreateFailedException;
import cn.beecp.pool.exception.PoolNotCreateException;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import static cn.beecp.pool.PoolStaticCenter.CommonLog;
import static cn.beecp.pool.PoolStaticCenter.createClassInstance;

/**
 * Email:  Chris2018998@tom.com
 * Project: https://github.com/Chris2018998/BeeCP
 *
 * @author Chris.Liao
 * @version 1.0
 */
//fix BeeCP-Starter-#6 Chris-2020-09-01 start
//public final class BeeDataSource extends BeeDataSourceConfig implements DataSource {
public class BeeDataSource extends BeeDataSourceConfig implements DataSource, XADataSource {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private ConnectionPool pool;
    private boolean ready;
    private SQLException failedCause;

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
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static ConnectionPool createPool(BeeDataSource ds) throws SQLException {
        try {
            Class<?> poolClass = Class.forName(ds.getPoolImplementClassName());
            ConnectionPool pool = (ConnectionPool) createClassInstance(poolClass, ConnectionPool.class, "pool");
            pool.init(ds);
            ds.pool = pool;
            ds.ready = true;
            return pool;
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
        if (this.ready) return this.pool.getConnection();
        return createPoolByLock().getConnection();
    }

    public final XAConnection getXAConnection() throws SQLException {
        if (this.ready) return this.pool.getXAConnection();
        return createPoolByLock().getXAConnection();
    }

    private ConnectionPool createPoolByLock() throws SQLException {
        if (writeLock.tryLock()) {
            try {
                if (!ready) {
                    failedCause = null;
                    createPool(this);
                }
            } catch (SQLException e) {
                failedCause = e;
            } finally {
                writeLock.unlock();
            }
        } else {
            try {
                readLock.lock();
            } finally {
                readLock.unlock();
            }
        }

        //read lock will reach
        if (failedCause != null) throw failedCause;
        return pool;
    }

    public Connection getConnection(String username, String password) throws SQLException {
        throw new SQLFeatureNotSupportedException("Not support");
    }

    public XAConnection getXAConnection(String user, String password) throws SQLException {
        throw new SQLFeatureNotSupportedException("Not support");
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

    //***************************************************************************************************************//
    //                                     3: below are self-define methods(6)                                       //
    //***************************************************************************************************************//
    public void clear() throws SQLException {
        clear(false);
    }

    public void clear(boolean force) throws SQLException {
        if (this.pool == null) throw new PoolNotCreateException("Connection pool not initialize");
        this.pool.clear(force);
    }


    public boolean isClosed() {
        return this.pool == null || this.pool.isClosed();
    }

    public void close() {
        if (this.pool != null) {
            try {
                this.pool.close();
            } catch (Throwable e) {
                CommonLog.error("Error at closing connection pool,cause:", e);
            }
        }
    }

    public void setPrintRuntimeLog(boolean printRuntimeLog) {
        if (this.pool != null) this.pool.setPrintRuntimeLog(printRuntimeLog);
    }

    public ConnectionPoolMonitorVo getPoolMonitorVo() throws SQLException {
        if (this.pool == null) throw new PoolNotCreateException("Connection pool not initialize");
        return this.pool.getPoolMonitorVo();
    }
}
