/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp;

import cn.beecp.pool.DataSourceConnectionFactory;
import cn.beecp.pool.DriverConnectionFactory;
import cn.beecp.xa.XaConnectionFactory;

import javax.sql.DataSource;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.beecp.TransactionIsolationLevel.TRANS_LEVEL_CODE_LIST;
import static cn.beecp.TransactionIsolationLevel.isValidTransactionIsolationCode;
import static cn.beecp.pool.PoolStaticCenter.*;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Connection pool configuration under datasource
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class BeeDataSourceConfig implements BeeDataSourceConfigJmxBean {
    //poolName index
    private static final AtomicInteger poolNameIndex = new AtomicInteger(1);
    //jdbc user name
    private String username;
    //jdbc password
    private String password;
    //jdbc url
    private String jdbcUrl;
    //jdbc driver class name
    private String driverClassName;

    //connection default value:catalog <code>Connection.setAutoCommit(String)</code>
    private String defaultCatalog;
    //connection default value:schema <code>Connection.setSchema(String)</code>
    private String defaultSchema;
    //connection default value:readOnly <code>Connection.setReadOnly(boolean)</code>
    private boolean defaultReadOnly;
    //connection default value:autoCommit <code>Connection.setAutoCommit(boolean)</code>
    private boolean defaultAutoCommit = true;
    //connection default value:transactionIsolation <code>Connection.setTransactionIsolation(int)</code>
    private int defaultTransactionIsolationCode = -999;
    //connection default value:description of transactionIsolation <code>defaultTransactionIsolationCode</code>
    private String defaultTransactionIsolationName;

    //pool name
    private String poolName;
    //pool mode:fair,compete
    private boolean fairMode;
    //connection size on pool initialize
    private int initialSize;
    //connection can reach max size in pool
    private int maxActive = 10;
    //borrow semaphore size
    private int borrowSemaphoreSize = Math.min(maxActive / 2, Runtime.getRuntime().availableProcessors());
    //milliseconds:max time to get one connection from pool
    private long maxWait = SECONDS.toMillis(8);
    //milliseconds:connection max idle time in pool,if reach,then remove from pool
    private long idleTimeout = MINUTES.toMillis(3);
    //milliseconds:connection not active time hold in borrower
    private long holdTimeout = MINUTES.toMillis(3);
    //connection test sql
    private String connectionTestSql = "SELECT 1";
    //seconds:wait for connection test result
    private int connectionTestTimeout = 3;
    //milliseconds:connection test interval time to last active time
    private long connectionTestInterval = 500L;
    //milliseconds:interval time to run check task
    private long idleCheckTimeInterval = MINUTES.toMillis(1);
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
    private String poolImplementClassName;
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

    public BeeDataSourceConfig(File propertiesFile) {
        this.loadFromPropertiesFile(propertiesFile);
    }

    public BeeDataSourceConfig(String propertiesFileName) {
        this.loadFromPropertiesFile(propertiesFileName);
    }

    public BeeDataSourceConfig(Properties configProperties) {
        this.loadFromProperties(configProperties);
    }

    private final static String getConfigValue(Properties configProperties, String propertyName) {
        String value = readConfig(configProperties, propertyName);
        if (isBlank(value))
            value = readConfig(configProperties, propertyNameToFieldId(propertyName, DS_Config_Prop_Separator_MiddleLine));
        if (isBlank(value))
            value = readConfig(configProperties, propertyNameToFieldId(propertyName, DS_Config_Prop_Separator_UnderLine));
        return value;
    }

    private final static String readConfig(Properties configProperties, String propertyName) {
        String value = configProperties.getProperty(propertyName);
        if (!isBlank(value)) {
            commonLog.info("beecp.{}={}", propertyName, value);
            return value.trim();
        } else {
            return null;
        }
    }

    private final static String trimString(String value) {
        return (value == null) ? null : value.trim();
    }

    private final static Driver loadJdbcDriver(String driverClassName) throws BeeDataSourceConfigException {
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
        this.defaultSchema = trimString(defaultSchema);
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
    public String getDefaultTransactionIsolationName() {
        return defaultTransactionIsolationName;
    }

    public void setDefaultTransactionIsolationName(String defaultTransactionIsolationName) {
        this.defaultTransactionIsolationName = trimString(defaultTransactionIsolationName);
    }

    @Override
    public String getConnectionTestSql() {
        return connectionTestSql;
    }

    public void setConnectionTestSql(String connectionTestSql) {
        if (!isBlank(connectionTestSql))
            this.connectionTestSql = trimString(connectionTestSql);
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
            this.borrowSemaphoreSize = (maxActive > 1) ? Math.min(maxActive / 2, Runtime.getRuntime().availableProcessors()) : 1;
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
        if (!isBlank(key))
            connectProperties.remove(key);
    }

    public void addConnectProperty(String key, Object value) {
        if (!isBlank(key) && value != null)
            connectProperties.put(key, value);
    }

    public void addConnectProperty(String connectPropertyText) {
        if (!isBlank(connectPropertyText)) {
            String[] attributeArray = connectPropertyText.split("&");
            for (String attribute : attributeArray) {
                String[] pair = attribute.split("=");
                if (pair.length == 2) {
                    addConnectProperty(pair[0].trim(), pair[1].trim());
                } else {
                    pair = attribute.split(":");
                    if (pair.length == 2) {
                        addConnectProperty(pair[0].trim(), pair[1].trim());
                    }
                }
            }
        }
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

    void copyTo(BeeDataSourceConfig config) {
        //1:primitive type copy
        String fieldName = "";
        try {
            for (Field field : BeeDataSourceConfig.class.getDeclaredFields()) {
                if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers()) || "connectProperties".equals(field.getName()))
                    continue;
                Object fieldValue = field.get(this);
                fieldName = field.getName();
                commonLog.debug("BeeDataSourceConfig.{}={}", fieldName, fieldValue);
                field.set(config, fieldValue);
            }
        } catch (Exception e) {
            throw new BeeDataSourceConfigException("Failed to copy field[" + fieldName + "]", e);
        }

        //2:copy 'connectProperties'
        Iterator<Map.Entry<Object, Object>> iterator = connectProperties.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Object, Object> entry = iterator.next();
            commonLog.debug("BeeDataSourceConfig.connectProperties.{}={}", entry.getKey(), entry.getValue());
            config.addConnectProperty((String) entry.getKey(), entry.getValue());
        }
    }

    //check pool configuration
    public BeeDataSourceConfig check() throws SQLException {
        if (this.maxActive <= 0)
            throw new BeeDataSourceConfigException("maxActive must be greater than zero");
        if (this.initialSize < 0)
            throw new BeeDataSourceConfigException("initialSize must not be less than zero");
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
        if (isBlank(this.connectionTestSql))
            throw new BeeDataSourceConfigException("connectionTestSql cant be null or empty");
        if (!this.connectionTestSql.toLowerCase(Locale.US).startsWith("select ")) {
            //fix issue:#1 The check of validationQuerySQL has logic problem. Chris-2019-05-01 end
            throw new BeeDataSourceConfigException("connectionTestSql must be start with 'select '");
        }

        if (isBlank(this.poolName)) this.poolName = "FastPool-" + poolNameIndex.getAndIncrement();

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

    public void loadFromPropertiesFile(String filename) {
        if (isBlank(filename)) throw new IllegalArgumentException("Properties file can't be null");
        loadFromPropertiesFile(new File(filename));
    }

    public void loadFromPropertiesFile(File file) {
        if (file == null) throw new IllegalArgumentException("Properties file can't be null");
        if (!file.exists()) throw new IllegalArgumentException("File not found:" + file.getAbsolutePath());
        if (!file.isFile()) throw new IllegalArgumentException("Target object is not a valid file");
        if (!file.getAbsolutePath().toLowerCase(Locale.US).endsWith(".properties"))
            throw new IllegalArgumentException("Target file is not a properties file");

        InputStream stream = null;
        try {
            stream = Files.newInputStream(Paths.get(file.toURI()));
            Properties configProperties = new Properties();
            configProperties.load(stream);
            loadFromProperties(configProperties);
        } catch (BeeDataSourceConfigException e) {
            throw (BeeDataSourceConfigException) e;
        } catch (Throwable e) {
            throw new IllegalArgumentException("Failed to load properties file:", e);
        } finally {
            if (stream != null) try {
                stream.close();
            } catch (Throwable e) {
            }
        }
    }

    public void loadFromProperties(Properties configProperties) {
        if (configProperties == null || configProperties.isEmpty())
            throw new IllegalArgumentException("Properties can't be null or empty");

        //1:get all properties set methods
        Map<String, Method> setMethodMap = getSetMethodMap(BeeDataSourceConfig.class);
        //2:create properties to collect config value
        Map<String, Object> setValueMap = new HashMap<String, Object>(setMethodMap.size());
        //3:loop to find out properties config value by set methods
        Iterator<String> iterator = setMethodMap.keySet().iterator();
        while (iterator.hasNext()) {
            String propertyName = iterator.next();
            String configVal = getConfigValue(configProperties, propertyName);
            if (isBlank(configVal)) continue;
            setValueMap.put(propertyName, configVal);
        }
        //4:inject found config value to ds config object
        setPropertiesValue(this, setMethodMap, setValueMap);

        //5:try to find 'connectProperties' config value and put to ds config object
        addConnectProperty(getConfigValue(configProperties, "connectProperties"));
        String connectPropertiesCount = getConfigValue(configProperties, "connectProperties.count");
        if (!isBlank(connectPropertiesCount)) {
            int count = 0;
            try {
                count = Integer.parseInt(connectPropertiesCount.trim());
            } catch (Throwable e) {
            }
            for (int i = 1; i <= count; i++)
                addConnectProperty(getConfigValue(configProperties, "connectProperties." + i));
        }
    }

    private final int getTransactionIsolationCode() throws BeeDataSourceConfigException {
        if (!isBlank(defaultTransactionIsolationName)) {
            return TransactionIsolationLevel.getTransactionIsolationCode(defaultTransactionIsolationName);
        } else {
            if (defaultTransactionIsolationCode != -999 && !isValidTransactionIsolationCode(defaultTransactionIsolationCode))
                throw new BeeDataSourceConfigException("defaultTransactionIsolationCode error,valid value is one of[" + TRANS_LEVEL_CODE_LIST + "]");

            return defaultTransactionIsolationCode;
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

