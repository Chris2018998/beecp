/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.beecp;

import cn.beecp.pool.ConnectionPool;
import cn.beecp.pool.ProxyConnectionBase;
import cn.beecp.xa.XaConnectionFactory;
import cn.beecp.xa.XaConnectionWrapper;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import static cn.beecp.util.BeeJdbcUtil.isBlank;

/**
 * Bee DataSource,there are two pool implementation for it.
 * <p>
 * 1) cn.beecp.pool.FastConnectionPool:base implementation with semaphore
 * 2) cn.beecp.pool.RawConnectionPool:return raw connections to borrowers directly(maybe used for BeeNode)
 * <p>
 * Email:  Chris2018998@tom.com
 * Project: https://github.com/Chris2018998/BeeCP
 *
 * @author Chris.Liao
 * @version 1.0
 */
//fix BeeCP-Starter-#6 Chris-2020-09-01 start
//public final class BeeDataSource extends BeeDataSourceConfig implements DataSource {
public class BeeDataSource extends BeeDataSourceConfig implements DataSource, XADataSource {
//fix BeeCP-Starter-#6 Chris-2020-09-01 end
    private final static HashMap<String, String> XaConnectionFactoryMap = new HashMap();
    private static final SQLException XaConnectionFactoryNotFound = new SQLException("Can't found matched XaConnectionFactory for driver,please config it");

    static {
        XaConnectionFactoryMap.put("oracle", "cn.beecp.xa.OracleXaConnectionFactory");
        XaConnectionFactoryMap.put("mariadb", "cn.beecp.xa.MariadbXaConnectionFactory");
        XaConnectionFactoryMap.put("mysql5", "cn.beecp.xa.Mysql5XaConnectionFactory");
        XaConnectionFactoryMap.put("mysql8", "cn.beecp.xa.Mysql8XaConnectionFactory");
        XaConnectionFactoryMap.put("postgresql", "cn.beecp.xa.PostgresXaConnectionFactory");
    }

    /**
     * logger
     */
    private final org.slf4j.Logger log = LoggerFactory.getLogger(this.getClass());
    /**
     * pool initialized
     */
    private boolean inited;
    /**
     * connection pool
     */
    private ConnectionPool pool;
    /**
     * failed cause to creating pool
     */
    private SQLException failedCause;
    /**
     * read Write Locker
     */
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    /**
     * read Locker
     */
    private ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    /**
     * write Locker
     */
    private ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    /**
     * @return a XAConnection
     * @throws SQLException
     */
    private XaConnectionFactory xaConnectionFactory;

    /**
     * constructor
     */
    public BeeDataSource() { }

    /**
     * constructor
     *
     * @param config data source configuration
     */
    public BeeDataSource(final BeeDataSourceConfig config) {
        try {
            config.copyTo(this);
            pool = createPool(this);
            inited = true;
        } catch (SQLException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * constructor
     *
     * @param driver   driver class name
     * @param url      JDBC url
     * @param user     JDBC user name
     * @param password JDBC password
     */
    public BeeDataSource(String driver, String url, String user, String password) {
        super(driver, url, user, password);
    }

    /**
     * borrow a connection from pool
     *
     * @return If exists idle connection in pool,then return one;if not, waiting
     * until other borrower release
     * @throws SQLException if pool is closed or waiting timeout,then throw exception
     */
    public Connection getConnection() throws SQLException {
        if (inited) return pool.getConnection();

        if (writeLock.tryLock()) {
            try {
                if (!inited) {
                    failedCause = null;
                    pool = createPool(this);
                    inited = true;
                }
            } catch (SQLException e) {
                failedCause = e;
                throw e;
            } finally {
                writeLock.unlock();
            }
        } else {
            try {
                readLock.lock();
                if (failedCause != null) throw failedCause;
            } finally {
                readLock.unlock();
            }
        }

        return pool.getConnection();
    }

    public XAConnection getXAConnection() throws SQLException {
        if (xaConnectionFactory == null) throw XaConnectionFactoryNotFound;
        ProxyConnectionBase proxyCon = (ProxyConnectionBase) this.getConnection();
        return new XaConnectionWrapper(xaConnectionFactory.create(proxyCon.getDelegate()), proxyCon);
    }

    public Connection getConnection(String username, String password) throws SQLException {
        throw new SQLException("Not support");
    }

    public XAConnection getXAConnection(String user, String password) throws SQLException {
        throw new SQLException("Not support");
    }

    public void close() {
        if (pool != null) {
            try {
                pool.close();
            } catch (SQLException e) {
                log.error("Error on closing connection pool,cause:", e);
            }
        }
    }

    public boolean isClosed() {
        return (pool != null) ? pool.isClosed() : false;
    }

    public PrintWriter getLogWriter() throws SQLException {
        throw new SQLException("Not supported");
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new SQLException("Not supported");
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("Not supported");
    }

    public int getLoginTimeout() throws SQLException {
        throw new SQLException("Not supported");
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        throw new SQLException("Not supported");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this))
            return (T) this;
        else
            throw new SQLException("Wrapped object is not an instance of " + iface);
    }

    /**
     * create a pool instance by specified class name in configuration,
     * and initialize the pool with configuration
     *
     * @param config pool configuration
     * @return a initialized pool for data source
     */
    private final ConnectionPool createPool(BeeDataSourceConfig config) throws SQLException {
        config.check();
        String poolImplementClassName = config.getPoolImplementClassName();
        if (isBlank(poolImplementClassName))
            poolImplementClassName = BeeDataSourceConfig.DefaultImplementClassName;

        try {
            Class<?> poolClass = Class.forName(poolImplementClassName, true, getClass().getClassLoader());
            if (!ConnectionPool.class.isAssignableFrom(poolClass))
                throw new IllegalArgumentException("Connection pool class must be implemented 'ConnectionPool' interface");

            config.setAsChecked();
            ConnectionPool pool = (ConnectionPool) poolClass.newInstance();
            pool.init(config);

            //Create XAConnection Factory begin
            xaConnectionFactory = config.getXaConnectionFactory();
            if (xaConnectionFactory == null && !isBlank(config.getUrl())) {
                String driverType = getDriverType(config.getUrl());
                if (!isBlank(driverType)) {
                    String xaConnectionFactoryClassName=XaConnectionFactoryMap.get(driverType);
                    if (!isBlank(xaConnectionFactoryClassName)) {
                        try {
                            Class<?> xaConnectionFactoryClass = Class.forName(xaConnectionFactoryClassName,true, getClass().getClassLoader());
                            if (XaConnectionFactory.class.isAssignableFrom(xaConnectionFactoryClass)) {
                                xaConnectionFactory = (XaConnectionFactory) xaConnectionFactoryClass.newInstance();
                            }
                        } catch (ClassNotFoundException e) {
                            throw new BeeDataSourceConfigException("Class(" + xaConnectionFactoryClassName + ")not found ");
                        } catch (InstantiationException e) {
                            throw new BeeDataSourceConfigException("Failed to instantiate XAConnection factory class:" + xaConnectionFactoryClassName, e);
                        } catch (IllegalAccessException e) {
                            throw new BeeDataSourceConfigException("Failed to instantiate XAConnection factory class:" + xaConnectionFactoryClassName, e);
                        }
                    }
                }
            }
            //Create XAConnection Factory end

            return pool;
        } catch (ClassNotFoundException e) {
            throw new SQLException("Not found connection pool implementation class:" + poolImplementClassName);
        } catch (Throwable e) {
            throw new SQLException(e);
        }
    }

    private String getDriverType(String url) {
        try {
            Driver driver = DriverManager.getDriver(url);
            if (url.indexOf("oracle") > 1) {
                return "oracle";
            } else if (url.indexOf("mysql") > 1) {
                return "mysql" + driver.getMajorVersion();
            } else if (url.indexOf("mariadb") > 1) {
                return "mariadb";
            } else if (url.indexOf("postgresql") > 1) {
                return "postgresql";
            } else {
                return null;
            }
        } catch (SQLException e) {
            log.warn("Can't get driver by url from driverManager", e);
            return null;
        }
    }
}
