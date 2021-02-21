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

import static cn.beecp.TransactionIsolationLevel.*;
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
public class BeeDataSourceConfig implements BeeDataSourceConfigJmxBean {
    //Default pool implementation class name
    static final String DefaultImplementClassName = "cn.beecp.pool.FastConnectionPool";

    //jdbc user name
    private String username;
    //jdbc password
    private String password;
    //jdbc url
    private String jdbcUrl;
    //jdbc driver class name
    private String driverClassName;

    //default set value on raw connection after it created <code>connection.setAutoCommit(String)</code>
    private String defaultCatalog;
    //default set value on raw connection after it created <code>connection.setSchema(String)</code>
    private String defaultSchema;
    //default set value on raw connection after it created <code>connection.setReadOnly(boolean)</code>
    private boolean defaultReadOnly;
    //default set value on raw connection after it created. <code>connection.setAutoCommit(boolean)</code>
    private boolean defaultAutoCommit = true;
    //default set value on raw connection after it created,<code>connection.setTransactionIsolation(int)</code>
    private int defaultTransactionIsolationCode = Connection.TRANSACTION_READ_COMMITTED;
    //default transaction isolation description,match isolation code can be set to <code>defaultTransactionIsolationCode</code>
    private String defaultTransactionIsolation = TransactionIsolationLevel.LEVEL_READ_COMMITTED;
    //a SQL to check connection active,recommend to use a simple query SQL,not contain procedure,function in SQL
    private String connectionTestSQL = "select 1 from dual";

    //pool name
    private String poolName;
    //true:fair,first arrive first take
    private boolean fairMode;
    //connection created size at pool initialization
    private int initialSize;
    //connection max size in pool
    private int maxActive = 10;
    //borrow Semaphore Size
    private int borrowSemaphoreSize = Math.min(maxActive / 2, Runtime.getRuntime().availableProcessors());
    //milliseconds:borrower request timeout
    private long maxWait = SECONDS.toMillis(8);
    //milliseconds:idle timeout connection will be removed from pool
    private long idleTimeout = MINUTES.toMillis(3);
    //milliseconds:long time not active connection hold by borrower will closed by pool
    private long holdTimeout = MINUTES.toMillis(5);
    //seconds:max time to get check active result from connection(socket readout time)
    private int connectionTestTimeout = 3;
    //milliseconds:connection test interval time from last active time
    private long connectionTestInterval = 500L;
    //milliseconds:interval time to run check task in scheduledThreadPoolExecutor
    private long idleCheckTimeInterval = MINUTES.toMillis(5);
    //using connection close indicator,true,close directly;false,delay close util them becoming idle or hold timeout
    private boolean forceCloseUsingOnClear;
    //milliseconds:delay time for next clear pooled connections when exists using connections and 'forceCloseUsingOnClear' is false
    private long delayTimeForNextClear = 3000L;

    //physical JDBC Connection factory
    private ConnectionFactory connectionFactory;
    //physical JDBC Connection factory class name
    private String connectionFactoryClassName;
    //xaConnectionFactory
    private XaConnectionFactory xaConnectionFactory;
    //xaConnection Factory ClassName
    private String xaConnectionFactoryClassName;
    //connection extra properties
    private Properties connectProperties = new Properties();
    //pool implementation class name
    private String poolImplementClassName = DefaultImplementClassName;
    //indicator,whether register datasource to jmx
    private boolean enableJmx;

    public BeeDataSourceConfig() {
    }

    public BeeDataSourceConfig(String driver, String url, String user, String password) {
        this.jdbcUrl = trimString(url);
        this.username = trimString(user);
        this.password = trimString(password);
        this.driverClassName = trimString(driver);
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = trimString(username);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = trimString(password);
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = trimString(jdbcUrl);
    }

    @Override
    public String getUrl() {
        return jdbcUrl;
    }

    public void setUrl(String jdbcUrl) {
        this.jdbcUrl = trimString(jdbcUrl);
    }

    @Override
    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = trimString(driverClassName);
    }

    @Override
    public String getDefaultCatalog() {
        return defaultCatalog;
    }

    public void setDefaultCatalog(String defaultCatalog) {
        this.defaultCatalog = trimString(defaultCatalog);
    }

    public String getDefaultSchema() {
        return defaultSchema;
    }

    public void setDefaultSchema(String defaultSchema) {
        this.defaultCatalog = trimString(defaultSchema);
    }

    @Override
    public boolean isDefaultReadOnly() {
        return defaultReadOnly;
    }

    public void setDefaultReadOnly(boolean defaultReadOnly) {
        this.defaultReadOnly = defaultReadOnly;
    }

    @Override
    public boolean isDefaultAutoCommit() {
        return defaultAutoCommit;
    }

    public void setDefaultAutoCommit(boolean defaultAutoCommit) {
        this.defaultAutoCommit = defaultAutoCommit;
    }

    @Override
    public int getDefaultTransactionIsolationCode() {
        return defaultTransactionIsolationCode;
    }

    public void setDefaultTransactionIsolationCode(int defaultTransactionIsolationCode) {
        this.defaultTransactionIsolationCode = defaultTransactionIsolationCode;
    }

    @Override
    public String getDefaultTransactionIsolation() {
        return defaultTransactionIsolation;
    }

    public void setDefaultTransactionIsolation(String defaultTransactionIsolation) {
        this.defaultTransactionIsolation = trimString(defaultTransactionIsolation);
    }

    @Override
    public String getConnectionTestSQL() {
        return connectionTestSQL;
    }

    public void setConnectionTestSQL(String connectionTestSQL) {
        this.connectionTestSQL = trimString(connectionTestSQL);
    }

    @Override
    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = trimString(poolName);
    }

    @Override
    public boolean isFairMode() {
        return fairMode;
    }

    public void setFairMode(boolean fairMode) {
        this.fairMode = fairMode;
    }

    @Override
    public int getInitialSize() {
        return initialSize;
    }

    public void setInitialSize(int initialSize) {
        if (initialSize >= 0)
            this.initialSize = initialSize;
    }

    @Override
    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(int maxActive) {
        if (maxActive > 0) {
            this.maxActive = maxActive;
            //fix issue:#19 Chris-2020-08-16 begin
            if (maxActive > 1)
                this.borrowSemaphoreSize = Math.min(maxActive / 2, Runtime.getRuntime().availableProcessors());
            //fix issue:#19 Chris-2020-08-16 end
        }
    }

    @Override
    public int getBorrowSemaphoreSize() {
        return borrowSemaphoreSize;
    }

    public void setBorrowSemaphoreSize(int borrowSemaphoreSize) {
        if (borrowSemaphoreSize > 0)
            this.borrowSemaphoreSize = borrowSemaphoreSize;
    }

    @Override
    public long getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(long maxWait) {
        if (maxWait > 0)
            this.maxWait = maxWait;
    }

    @Override
    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        if (idleTimeout > 0)
            this.idleTimeout = idleTimeout;
    }

    @Override
    public long getHoldTimeout() {
        return holdTimeout;
    }

    public void setHoldTimeout(long holdTimeout) {
        if (holdTimeout > 0)
            this.holdTimeout = holdTimeout;
    }

    @Override
    public int getConnectionTestTimeout() {
        return connectionTestTimeout;
    }

    public void setConnectionTestTimeout(int connectionTestTimeout) {
        if (connectionTestTimeout >= 0)
            this.connectionTestTimeout = connectionTestTimeout;
    }

    @Override
    public long getConnectionTestInterval() {
        return connectionTestInterval;
    }

    public void setConnectionTestInterval(long connectionTestInterval) {
        if (connectionTestInterval >= 0)
            this.connectionTestInterval = connectionTestInterval;
    }

    @Override
    public long getIdleCheckTimeInterval() {
        return idleCheckTimeInterval;
    }

    public void setIdleCheckTimeInterval(long idleCheckTimeInterval) {
        if (idleCheckTimeInterval > 0)
            this.idleCheckTimeInterval = idleCheckTimeInterval;
    }

    @Override
    public boolean isForceCloseUsingOnClear() {
        return forceCloseUsingOnClear;
    }

    public void setForceCloseUsingOnClear(boolean forceCloseUsingOnClear) {
        this.forceCloseUsingOnClear = forceCloseUsingOnClear;
    }

    @Override
    public long getDelayTimeForNextClear() {
        return delayTimeForNextClear;
    }

    public void setDelayTimeForNextClear(long delayTimeForNextClear) {
        if (delayTimeForNextClear > 0)
            this.delayTimeForNextClear = delayTimeForNextClear;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public String getConnectionFactoryClassName() {
        return connectionFactoryClassName;
    }

    public void setConnectionFactoryClassName(String connectionFactoryClassName) {
        this.connectionFactoryClassName = trimString(connectionFactoryClassName);
    }

    public XaConnectionFactory getXaConnectionFactory() {
        return xaConnectionFactory;
    }

    public void setXaConnectionFactory(XaConnectionFactory xaConnectionFactory) {
        this.xaConnectionFactory = xaConnectionFactory;
    }

    public String getXaConnectionFactoryClassName() {
        return xaConnectionFactoryClassName;
    }

    public void setXaConnectionFactoryClassName(String xaConnectionFactoryClassName) {
        this.xaConnectionFactoryClassName = trimString(xaConnectionFactoryClassName);
    }

    public void removeConnectProperty(String key) {
        connectProperties.remove(key);
    }

    public void addConnectProperty(String key, Object value) {
        connectProperties.put(key, value);
    }

    @Override
    public String getPoolImplementClassName() {
        return poolImplementClassName;
    }

    public void setPoolImplementClassName(String poolImplementClassName) {
        this.poolImplementClassName = trimString(poolImplementClassName);
    }

    @Override
    public boolean isEnableJmx() {
        return enableJmx;
    }

    public void setEnableJmx(boolean enableJmx) {
        this.enableJmx = enableJmx;
    }


    void copyTo(BeeDataSourceConfig config) throws SQLException {
        int modifiers;
        Field[] fields = BeeDataSourceConfig.class.getDeclaredFields();
        for (Field field : fields) {
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

    //check pool configuration
    public BeeDataSourceConfig check() throws SQLException {
        if (this.maxActive <= 0)
            throw new BeeDataSourceConfigException("maxActive must be greater than zero");
        if (this.initialSize < 0)
            throw new BeeDataSourceConfigException("initialSize must be greater than zero");
        if (this.initialSize > maxActive)
            throw new BeeDataSourceConfigException("initialSize must not be greater than maxActive");
        if (this.borrowSemaphoreSize <= 0)
            throw new BeeDataSourceConfigException("borrowSemaphoreSize must be greater than zero");
        //fix issue:#19 Chris-2020-08-16 begin
        //if (this.borrowConcurrentSize > maxActive)
        //throw new BeeDataSourceConfigException("Pool 'borrowConcurrentSize' must not be greater than pool max size");
        //fix issue:#19 Chris-2020-08-16 end
        if (this.idleTimeout <= 0)
            throw new BeeDataSourceConfigException("idleTimeout must be greater than zero");
        if (this.holdTimeout <= 0)
            throw new BeeDataSourceConfigException("holdTimeout must be greater than zero");
        if (this.maxWait <= 0)
            throw new BeeDataSourceConfigException("maxWait must be greater than zero");
        //fix issue:#1 The check of validationQuerySQL has logic problem. Chris-2019-05-01 begin
        //if (this.validationQuerySQL != null && validationQuerySQL.trim().length() == 0) {
        if (!isBlank(this.connectionTestSQL) && !this.connectionTestSQL.toLowerCase(Locale.US).startsWith("select "))
            //fix issue:#1 The check of validationQuerySQL has logic problem. Chris-2019-05-01 end
            throw new BeeDataSourceConfigException("connectionTestSQL must be start with 'select '");
        //}

        //get transaction Isolation Code
        int transactionIsolationCode = getTransactionIsolationCode();

        //try to create connection factory
        ConnectionFactory connectionFactory = tryCreateConnectionFactory();

        BeeDataSourceConfig configCopy = new BeeDataSourceConfig();
        this.copyTo(configCopy);

        configCopy.setConnectionFactory(connectionFactory);
        configCopy.setDefaultTransactionIsolationCode(transactionIsolationCode);
        return configCopy;
    }

    public void loadPropertiesFile(String filename) throws IOException {
        if (isBlank(filename)) throw new IOException("Properties file can't be null");
        loadPropertiesFile(new File(filename));
    }

    public void loadPropertiesFile(File file) throws IOException {
        if (file == null) throw new IOException("Properties file can't be null");
        if (!file.exists()) throw new FileNotFoundException(file.getAbsolutePath());
        if (!file.isFile()) throw new IOException("Target object is not a valid file");
        if (!file.getAbsolutePath().toLowerCase(Locale.US).endsWith(".properties"))
            throw new IOException("Target file is not a properties file");

        InputStream stream = null;
        try {
            stream = Files.newInputStream(Paths.get(file.toURI()));
            connectProperties.clear();
            connectProperties.load(stream);
        } finally {
            if (stream != null) stream.close();
        }
    }

    private String trimString(String value) {
        return (value == null) ? null : value.trim();
    }

    private final int getTransactionIsolationCode() throws BeeDataSourceConfigException {
        if (!isBlank(defaultTransactionIsolation)) {
            int transactionIsolationCode = TransactionIsolationLevel.getTransactionIsolationCode(defaultTransactionIsolation);
            if (transactionIsolationCode == -999)
                throw new BeeDataSourceConfigException("defaultTransactionIsolation error,valid value is one of[" + TRANS_LEVEL_DESC_LIST + "]");
            return transactionIsolationCode;
        } else {
            if (!isValidTransactionIsolationCode(defaultTransactionIsolationCode))
                throw new BeeDataSourceConfigException("defaultTransactionIsolationCode error,valid value is one of[" + TRANS_LEVEL_CODE_LIST + "]");

            return defaultTransactionIsolationCode;
        }
    }

    private final Driver loadJdbcDriver(String driverClassName) throws BeeDataSourceConfigException {
        try {
            Class<?> driverClass = Class.forName(driverClassName, true, BeeDataSourceConfig.class.getClassLoader());
            return (Driver) driverClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new BeeDataSourceConfigException("Not found driver class:" + driverClassName);
        } catch (InstantiationException e) {
            throw new BeeDataSourceConfigException("Failed to instantiate driver class:" + driverClassName, e);
        } catch (IllegalAccessException e) {
            throw new BeeDataSourceConfigException("Failed to instantiate driver class:" + driverClassName, e);
        }
    }

    private final ConnectionFactory tryCreateConnectionFactory() throws BeeDataSourceConfigException, SQLException {
        if (connectionFactory != null) return connectionFactory;

        if (isBlank(connectionFactoryClassName)) {
            if (isBlank(jdbcUrl))
                throw new BeeDataSourceConfigException("jdbcUrl can't be null");

            Driver connectDriver = null;
            if (!isBlank(driverClassName))
                connectDriver = loadJdbcDriver(driverClassName);
            else if (!isBlank(jdbcUrl))
                connectDriver = DriverManager.getDriver(jdbcUrl);
            if (connectDriver == null)
                throw new BeeDataSourceConfigException("Failed to load jdbc Driver:" + driverClassName);
            if (!connectDriver.acceptsURL(jdbcUrl))
                throw new BeeDataSourceConfigException("jdbcUrl(" + jdbcUrl + ")can not match driver:" + connectDriver.getClass().getName());

            Properties connectProperties = new Properties();
            connectProperties.putAll(this.connectProperties);
            if (!isBlank(username))
                connectProperties.put("user", username);
            if (!isBlank(password))
                connectProperties.put("password", password);
            return new DriverConnectionFactory(jdbcUrl, connectDriver, connectProperties);
        } else {
            try {
                Class<?> conFactClass = Class.forName(connectionFactoryClassName, true, BeeDataSourceConfig.class.getClassLoader());
                if (ConnectionFactory.class.isAssignableFrom(conFactClass)) {
                    return (ConnectionFactory) conFactClass.newInstance();
                } else if (DataSource.class.isAssignableFrom(conFactClass)) {
                    DataSource dataSource = (DataSource) conFactClass.newInstance();
                    Properties connectProperties = this.connectProperties;
                    Map<String, Object> setValueMap = new HashMap<String, Object>(connectProperties.size());
                    Iterator<Map.Entry<Object, Object>> iterator = connectProperties.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) iterator.next();
                        if (entry.getKey() instanceof String) {
                            setValueMap.put((String) entry.getKey(), entry.getValue());
                        }
                    }

                    try {
                        setPropertiesValue(dataSource, setValueMap);
                    } catch (Exception e) {
                        throw new BeeDataSourceConfigException("Failed to set config value to connection dataSource", e);
                    }

                    return new DataSourceConnectionFactory(dataSource, username, password);
                } else {
                    throw new BeeDataSourceConfigException("Error connection factory class,must implement '" + ConnectionFactory.class.getName() + "' interface");
                }
            } catch (ClassNotFoundException e) {
                throw new BeeDataSourceConfigException("Not found connection factory class:" + connectionFactoryClassName);
            } catch (InstantiationException e) {
                throw new BeeDataSourceConfigException("Failed to instantiate connection factory class:" + connectionFactoryClassName, e);
            } catch (IllegalAccessException e) {
                throw new BeeDataSourceConfigException("Failed to instantiate connection factory class:" + connectionFactoryClassName, e);
            }
        }
    }
}

