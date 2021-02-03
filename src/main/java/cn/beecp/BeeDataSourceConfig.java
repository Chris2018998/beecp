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

import cn.beecp.pool.DataSourceConnectionFactory;
import cn.beecp.pool.DriverConnectionFactory;
import cn.beecp.xa.XaConnectionFactory;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

import static cn.beecp.pool.PoolStaticCenter.isBlank;
import static cn.beecp.pool.PoolStaticCenter.setPropertiesValue;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Connection pool configuration under datasource
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class BeeDataSourceConfig implements BeeDataSourceConfigJMXBean {
    //Pool implementation default class name
    static final String DefaultImplementClassName = "cn.beecp.pool.FastConnectionPool";
    //indicator of configuration check result,true:check passed
    private boolean checked;
    //jdbc user name
    private String username;
    //jdbc password
    private String password;
    //jdbc url
    private String jdbcUrl;
    //jdbc driver class name
    private String driverClassName;
    //pool name
    private String poolName;
    //true:first come first take connection
    private boolean fairMode;
    //connection created size at pool initialization
    private int initialSize;
    //connection max size in pool
    private int maxActive = 10;
    //borrow Semaphore Size
    private int borrowSemaphoreSize;
    //default set value on raw connection after it created. <code>connection.setAutoCommit(boolean)</code>
    private boolean defaultAutoCommit = true;
    //default transaction isolation description,match isolation code can be set to <code>defaultTransactionIsolationCode</code>
    private String defaultTransactionIsolation;
    //default set value on raw connection after it created,<code>connection.setTransactionIsolation(int)</code>
    private int defaultTransactionIsolationCode;
    //default set value on raw connection after it created <code>connection.setAutoCommit(String)</code> .
    private String defaultCatalog;
    //default set value on raw connection after it created <code>connection.setSchema(String)</code> .
    private String defaultSchema;
    //default set value on raw connection after it created <code>connection.setReadOnly(boolean)</code> .
    private boolean defaultReadOnly;

    //milliseconds:borrower request timeout
    private long maxWait = SECONDS.toMillis(8);
    //milliseconds:idle timeout connection will be removed from pool
    private long idleTimeout = MINUTES.toMillis(3);
    //milliseconds:long time not active connection hold by borrower will closed by pool
    private long holdTimeout = MINUTES.toMillis(5);
    //a SQL to check connection active,recommend to use a simple query SQL,not contain procedure,function in SQL
    private String connectionTestSQL = "select 1 from dual";
    //seconds:wait time to get connection whether active result.
    private int connectionTestTimeout = 3;
    //milliseconds:connection test interval time from last active time
    private long connectionTestInterval = 500L;
    //indicator,when pool close or reset,need close using connection directly
    private boolean forceCloseConnection;
    //seconds:delay time to close using connection util them become idle.
    private long waitTimeToClearPool = 3;
    //milliseconds:interval time to run check task in scheduledThreadPoolExecutor
    private long idleCheckTimeInterval = MINUTES.toMillis(5);
    //milliseconds:delay time to run first task in scheduledThreadPoolExecutor
    private long idleCheckTimeInitDelay = SECONDS.toMillis(1);

    //pool implementation class name
    private String poolImplementClassName = DefaultImplementClassName;
    //physical JDBC Connection factory class name
    private String connectionFactoryClassName;
    //physical JDBC Connection factory
    private ConnectionFactory connectionFactory;
    //connection extra properties
    private Properties connectProperties = new Properties();
    //xaConnection Factory ClassName
    private String xaConnectionFactoryClassName;
    //xaConnectionFactory
    private XaConnectionFactory xaConnectionFactory;
    //indicator,whether register datasource to jmx
    private boolean enableJMX;

    public BeeDataSourceConfig() {
        this(null, null, null, null);
    }

    public BeeDataSourceConfig(String driver, String url, String user, String password) {
        this.jdbcUrl = url;
        this.username = user;
        this.password = password;
        this.driverClassName = driver;
        defaultTransactionIsolation = TransactionIsolationLevel.LEVEL_READ_COMMITTED;
        defaultTransactionIsolationCode = Connection.TRANSACTION_READ_COMMITTED;
        //fix issue:#19 Chris-2020-08-16 begin
        borrowSemaphoreSize = Math.min(maxActive / 2, Runtime.getRuntime().availableProcessors());
        //fix issue:#19 Chris-2020-08-16 end
    }

    void setAsChecked() {
        if (!this.checked)
            this.checked = true;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (!this.checked)
            this.username = username;
    }

    public void setPassword(String password) {
        if (!this.checked)
            this.password = password;
    }

    public String getUrl() {
        return jdbcUrl;
    }

    public void setUrl(String jdbcUrl) {
        if (!this.checked && !isBlank(jdbcUrl))
            this.jdbcUrl = jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        if (!this.checked && !isBlank(jdbcUrl))
            this.jdbcUrl = jdbcUrl;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        if (!this.checked && !isBlank(driverClassName))
            this.driverClassName = driverClassName;
    }

    public String getConnectionFactoryClassName() {
        return connectionFactoryClassName;
    }

    public void setConnectionFactoryClassName(String connectionFactoryClassName) {
        if (!this.checked && !isBlank(connectionFactoryClassName))
            this.connectionFactoryClassName = connectionFactoryClassName;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        if (!this.checked)
            this.connectionFactory = connectionFactory;
    }

    public String getXaConnectionFactoryClassName() {
        return xaConnectionFactoryClassName;
    }

    public void setXaConnectionFactoryClassName(String xaConnectionFactoryClassName) {
        if (!this.checked && !isBlank(xaConnectionFactoryClassName))
            this.xaConnectionFactoryClassName = xaConnectionFactoryClassName;
    }

    public XaConnectionFactory getXaConnectionFactory() {
        return xaConnectionFactory;
    }

    public void setXaConnectionFactory(XaConnectionFactory xaConnectionFactory) {
        if (!this.checked)
            this.xaConnectionFactory = xaConnectionFactory;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        if (!this.checked && !isBlank(poolName))
            this.poolName = poolName;
    }

    public boolean isFairMode() {
        return fairMode;
    }

    public void setFairMode(boolean fairMode) {
        if (!this.checked)
            this.fairMode = fairMode;
    }

    public int getInitialSize() {
        return initialSize;
    }

    public void setInitialSize(int initialSize) {
        if (!this.checked && initialSize > 0)
            this.initialSize = initialSize;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(int maxActive) {
        if (!this.checked && maxActive > 0) {
            this.maxActive = maxActive;
            //fix issue:#19 Chris-2020-08-16 begin
            if (maxActive > 1)
                this.borrowSemaphoreSize = Math.min(maxActive / 2, Runtime.getRuntime().availableProcessors());
            //fix issue:#19 Chris-2020-08-16 end
        }
    }

    public int getBorrowSemaphoreSize() {
        return borrowSemaphoreSize;
    }

    public void setBorrowSemaphoreSize(int borrowSemaphoreSize) {
        if (!this.checked && borrowSemaphoreSize > 0)
            this.borrowSemaphoreSize = borrowSemaphoreSize;
    }

    public boolean isDefaultAutoCommit() {
        return defaultAutoCommit;
    }

    public void setDefaultAutoCommit(boolean defaultAutoCommit) {
        if (!this.checked) this.defaultAutoCommit = defaultAutoCommit;
    }

    public String getDefaultTransactionIsolation() {
        return defaultTransactionIsolation;
    }

    public void setDefaultTransactionIsolation(String defaultTransactionIsolation) {
        if (!this.checked && !isBlank(defaultTransactionIsolation))
            this.defaultTransactionIsolation = defaultTransactionIsolation;
    }

    public int getDefaultTransactionIsolationCode() {
        return defaultTransactionIsolationCode;
    }

    public String getDefaultCatalog() {
        return defaultCatalog;
    }

    public void setDefaultCatalog(String catalog) {
        if (!isBlank(catalog))
            this.defaultCatalog = catalog;
    }

    public String getDefaultSchema() {
        return defaultSchema;
    }

    public void setDefaultSchema(String schema) {
        if (!isBlank(schema))
            this.defaultSchema = schema;
    }

    public boolean isDefaultReadOnly() {
        return defaultReadOnly;
    }

    public void setDefaultReadOnly(boolean readOnly) {
        if (!this.checked)
            this.defaultReadOnly = readOnly;
    }

    public long getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(long maxWait) {
        if (!this.checked && maxWait > 0)
            this.maxWait = maxWait;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        if (!this.checked && idleTimeout > 0)
            this.idleTimeout = idleTimeout;
    }

    public long getHoldTimeout() {
        return holdTimeout;
    }

    public void setHoldTimeout(long holdTimeout) {
        if (!this.checked && holdTimeout > 0)
            this.holdTimeout = holdTimeout;
    }

    public String getConnectionTestSQL() {
        return connectionTestSQL;
    }

    public void setConnectionTestSQL(String validationQuery) {
        if (!this.checked && !isBlank(validationQuery))
            this.connectionTestSQL = validationQuery;
    }

    public int getConnectionTestTimeout() {
        return connectionTestTimeout;
    }

    public void setConnectionTestTimeout(int connectionTestTimeout) {
        if (!this.checked && connectionTestTimeout > 0)
            this.connectionTestTimeout = connectionTestTimeout;
    }

    public long getConnectionTestInterval() {
        return connectionTestInterval;
    }

    public void setConnectionTestInterval(long connectionTestInterval) {
        if (!this.checked && connectionTestInterval > 0)
            this.connectionTestInterval = connectionTestInterval;
    }

    public boolean isForceCloseConnection() {
        return forceCloseConnection;
    }

    public void setForceCloseConnection(boolean forceCloseConnection) {
        if (!this.checked)
            this.forceCloseConnection = forceCloseConnection;
    }

    public long getWaitTimeToClearPool() {
        return waitTimeToClearPool;
    }

    public void setWaitTimeToClearPool(long waitTimeToClearPool) {
        if (!this.checked && waitTimeToClearPool >= 0)
            this.waitTimeToClearPool = waitTimeToClearPool;
    }

    public long getIdleCheckTimeInterval() {
        return idleCheckTimeInterval;
    }

    public void setIdleCheckTimeInterval(long idleCheckTimeInterval) {
        if (!this.checked && idleCheckTimeInterval >= 1000L)
            this.idleCheckTimeInterval = idleCheckTimeInterval;
    }

    public long getIdleCheckTimeInitDelay() {
        return idleCheckTimeInitDelay;
    }

    public void setIdleCheckTimeInitDelay(long idleCheckTimeInitDelay) {
        if (!this.checked && idleCheckTimeInitDelay >= 1000L)
            this.idleCheckTimeInitDelay = idleCheckTimeInitDelay;
    }

    public String getPoolImplementClassName() {
        return poolImplementClassName;
    }

    public void setPoolImplementClassName(String poolImplementClassName) {
        if (!this.checked && !isBlank(poolImplementClassName)) {
            this.poolImplementClassName = poolImplementClassName;
        }
    }

    public void removeConnectProperty(String key) {
        if (!this.checked) {
            connectProperties.remove(key);
        }
    }

    public void addConnectProperty(String key, String value) {
        if (!this.checked) {
            connectProperties.put(key, value);
        }
    }

    public boolean isEnableJMX() {
        return enableJMX;
    }

    public void setEnableJMX(boolean enableJMX) {
        if (!this.checked)
            this.enableJMX = enableJMX;
    }

    void copyTo(BeeDataSourceConfig config) throws SQLException {
        int modifiers;
        Field[] fields = BeeDataSourceConfig.class.getDeclaredFields();
        for (Field field : fields) {
            if ("checked".equals(field.getName())) continue;
            modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers))
                continue;
            try {
                field.set(config, field.get(this));
            } catch (Exception e) {
                throw new BeeDataSourceConfigException("Failed to copy field[" + field.getName() + "]", e);
            }
        }
    }

    private Driver loadJdbcDriver(String driverClassName) throws BeeDataSourceConfigException {
        try {
            Class<?> driverClass = Class.forName(driverClassName, true, this.getClass().getClassLoader());
            Driver driver = (Driver) driverClass.newInstance();
            if (!driver.acceptsURL(this.jdbcUrl)) throw new InstantiationException();
            return driver;
        } catch (ClassNotFoundException e) {
            throw new BeeDataSourceConfigException("Driver class[" + driverClassName + "]not found");
        } catch (InstantiationException e) {
            throw new BeeDataSourceConfigException("Driver class[" + driverClassName + "]can't be instantiated");
        } catch (IllegalAccessException e) {
            throw new BeeDataSourceConfigException("Driver class[" + driverClassName + "]can't be instantiated", e);
        } catch (SQLException e) {
            throw new BeeDataSourceConfigException("Driver class[" + driverClassName + "]can't be instantiated", e);
        }
    }

    //check pool configuration
    void check() throws SQLException {
        if (connectionFactory == null && isBlank(this.connectionFactoryClassName)) {
            Driver connectDriver = null;
            if (!isBlank(driverClassName)) {
                connectDriver = loadJdbcDriver(driverClassName);
            } else if (!isBlank(jdbcUrl)) {
                connectDriver = DriverManager.getDriver(this.jdbcUrl);
            }

            if (isBlank(jdbcUrl))
                throw new BeeDataSourceConfigException("jdbcUrl can't be null");
            if (connectDriver == null)
                throw new BeeDataSourceConfigException("Failed to load jdbc Driver");

            if (!isBlank(this.username))
                this.connectProperties.put("user", this.username);
            if (!isBlank(this.password))
                this.connectProperties.put("password", this.password);

            if (this.socketLoginTimeout > 0) DriverManager.setLoginTimeout(socketLoginTimeout);
            connectionFactory = new DriverConnectionFactory(jdbcUrl, connectDriver, connectProperties);
        } else if (connectionFactory == null && !isBlank(this.connectionFactoryClassName)) {
            try {
                Class<?> conFactClass = Class.forName(connectionFactoryClassName, true, BeeDataSourceConfig.class.getClassLoader());
                if (ConnectionFactory.class.isAssignableFrom(conFactClass)) {
                    connectionFactory = (ConnectionFactory) conFactClass.newInstance();
                } else if (DataSource.class.isAssignableFrom(conFactClass)) {
                    DataSource driverDataSource = (DataSource) conFactClass.newInstance();
                    if (connectProperties != null && !connectProperties.isEmpty()) {
                        Map<String, Object> setValueMap = new HashMap<String, Object>();
                        Iterator<Map.Entry<Object, Object>> itor = connectProperties.entrySet().iterator();
                        while (itor.hasNext()) {
                            Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) itor.next();
                            if (entry.getKey() instanceof String) {
                                setValueMap.put((String) entry.getKey(), entry.getValue());
                            }
                        }

                        try {
                            setPropertiesValue(driverDataSource, setValueMap);
                        } catch (Exception e) {
                            throw new BeeDataSourceConfigException("Failed to set config value", e);
                        }
                    }
                    connectionFactory = new DataSourceConnectionFactory(driverDataSource, username, password);
                } else {
                    throw new BeeDataSourceConfigException("Custom connection factory class must be implemented 'ConnectionFactory' interface");
                }
            } catch (ClassNotFoundException e) {
                throw new BeeDataSourceConfigException("Class(" + connectionFactoryClassName + ")not found ");
            } catch (InstantiationException e) {
                throw new BeeDataSourceConfigException("Failed to instantiate connection factory class:" + connectionFactoryClassName, e);
            } catch (IllegalAccessException e) {
                throw new BeeDataSourceConfigException("Failed to instantiate connection factory class:" + connectionFactoryClassName, e);
            }
        }

        if (!isBlank(xaConnectionFactoryClassName) && xaConnectionFactory == null) {
            try {
                Class<?> xaConnectionFactoryClass = Class.forName(xaConnectionFactoryClassName, true, BeeDataSourceConfig.class.getClassLoader());
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

        if (this.maxActive <= 0)
            throw new BeeDataSourceConfigException("Pool 'maxActive' must be greater than zero");
        if (this.initialSize < 0)
            throw new BeeDataSourceConfigException("Pool 'initialSize' must be greater than zero");
        if (this.initialSize > maxActive)
            throw new BeeDataSourceConfigException("Pool 'initialSize' must not be greater than 'maxActive'");
        if (this.borrowSemaphoreSize <= 0)
            throw new BeeDataSourceConfigException("Pool 'borrowSemaphoreSize' must be greater than zero");
        //fix issue:#19 Chris-2020-08-16 begin
        //if (this.borrowConcurrentSize > maxActive)
        //throw new BeeDataSourceConfigException("Pool 'borrowConcurrentSize' must not be greater than pool max size");
        //fix issue:#19 Chris-2020-08-16 end

        if (this.idleTimeout <= 0)
            throw new BeeDataSourceConfigException("Connection 'idleTimeout' must be greater than zero");
        if (this.holdTimeout <= 0)
            throw new BeeDataSourceConfigException("Connection 'holdTimeout' must be greater than zero");
        if (this.maxWait <= 0)
            throw new BeeDataSourceConfigException("Borrower 'maxWait' must be greater than zero");

        defaultTransactionIsolationCode = TransactionIsolationLevel.nameToCode(defaultTransactionIsolation);
        if (defaultTransactionIsolationCode == -999) {
            throw new BeeDataSourceConfigException("Valid transaction isolation level list:" + TransactionIsolationLevel.TRANS_LEVEL_LIST);
        }

        //fix issue:#1 The check of validationQuerySQL has logic problem. Chris-2019-05-01 begin
        //if (this.validationQuerySQL != null && validationQuerySQL.trim().length() == 0) {
        if (!isBlank(this.connectionTestSQL) && !this.connectionTestSQL.trim().toLowerCase(Locale.US).startsWith("select "))
            //fix issue:#1 The check of validationQuerySQL has logic problem. Chris-2019-05-01 end
            throw new BeeDataSourceConfigException("Connection 'connectionTestSQL' must start with 'select '");
        //}
    }

    public void loadPropertiesFile(String filename) throws IOException {
        loadPropertiesFile(new File(filename));
    }

    public void loadPropertiesFile(File file) throws IOException {
        if (file == null) throw new IOException("Properties file can't be null");
        if (!file.exists()) throw new FileNotFoundException(file.getAbsolutePath());
        if (!file.isFile()) throw new IOException("Target object is not a valid file");
        if (!file.getAbsolutePath().toLowerCase(Locale.US).endsWith(".properties"))
            throw new IOException("Target file is not a properties file");

        if (!checked) {
            InputStream stream = null;
            try {
                stream = Files.newInputStream(Paths.get(file.toURI()));
                connectProperties.clear();
                connectProperties.load(stream);
            } finally {
                if (stream != null) stream.close();
            }
        }
    }
}

