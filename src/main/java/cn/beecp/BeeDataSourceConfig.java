/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package cn.beecp;

import cn.beecp.pool.ConnectionFactoryByDriver;
import cn.beecp.pool.ConnectionFactoryByDriverDs;
import cn.beecp.pool.FastConnectionPool;
import cn.beecp.pool.XaConnectionFactoryByDriverDs;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.beecp.TransactionIsolation.TRANS_LEVEL_CODE_LIST;
import static cn.beecp.pool.PoolStaticCenter.*;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Connection pool configuration under dataSource
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeDataSourceConfig implements BeeDataSourceConfigJmxBean {
    //index on pool name generation
    private static final AtomicInteger PoolNameIndex = new AtomicInteger(1);

    //extra properties for jdbc driver to connect db
    private final Map<String, Object> connectProperties = new HashMap<String, Object>(2);
    //jdbc user name
    private String username;
    //jdbc password
    private String password;
    //jdbc link url
    private String jdbcUrl;
    //jdbc driver class name
    private String driverClassName;
    //pool name. if not set,then generate with<code>BeeDataSourceConfig.PoolNameIndex</code>
    private String poolName;
    //boolean indicator,false:pool use unfair semaphore and compete transfer policy,default value:false
    private boolean fairMode;
    //connections create size on pool starting
    private int initialSize;
    //create connection on init by asynchronization
    private boolean asyncCreateInitConnection;
    //connections max reachable size in pool
    private int maxActive = Math.min(Math.max(10, NCPUS), 50);
    //max permit size of pool semaphore
    private int borrowSemaphoreSize = Math.min(this.maxActive / 2, NCPUS);
    //milliseconds:max wait parkTime to get one connection from pool<code>ConnectionPool.getConnection()</code>
    private long maxWait = SECONDS.toMillis(8);
    //milliseconds:max idle parkTime of connections in pool,when reach,then close them and remove from pool
    private long idleTimeout = MINUTES.toMillis(3);
    //milliseconds:max no-use parkTime hold by borrowers,when reach,then return them to pool by forced close
    private long holdTimeout = MINUTES.toMillis(3);
    //connection valid test sql on borrowed
    private String validTestSql = "SELECT 1";
    //seconds:max parkTime to get valid test result
    private int validTestTimeout = 3;
    //milliseconds:max gap parkTime between last activity and borrowed,if less this value,assume connection valid,otherwise test them
    private long validAssumeTime = 500L;
    //milliseconds:interval parkTime of pool idle-scan timer task
    private long timerCheckInterval = MINUTES.toMillis(3);
    //close indicator of connections in using on pool clean
    private boolean forceCloseUsingOnClear;
    //milliseconds:delay parkTime for next clear using connections util them return to pool,when<config>forceCloseUsingOnClear</config> is false
    private long delayTimeForNextClear = 3000L;

    //connection default value:catalog <code>Connection.setAutoCommit(String)</code>
    private String defaultCatalog;
    //connection default value:schema <code>Connection.setSchema(String)</code>
    private String defaultSchema;
    //connection default value:readOnly <code>Connection.setReadOnly(boolean)</code>
    private Boolean defaultReadOnly;
    //connection default value:autoCommit <code>Connection.setAutoCommit(boolean)</code>
    private Boolean defaultAutoCommit;
    //connection default value:transactionIsolation <code>Connection.setTransactionIsolation(int)</code>
    private Integer defaultTransactionIsolationCode;
    //connection default value:description of transactionIsolation <code>defaultTransactionIsolationCode</code>
    private String defaultTransactionIsolationName;

    //default value set indicator on catalog(connection property)
    private boolean enableDefaultOnCatalog = true;
    //default value set indicator on schema(connection property)
    private boolean enableDefaultOnSchema = true;
    //default value set indicator on readOnly(connection property)
    private boolean enableDefaultOnReadOnly = true;
    //default value set indicator on readOnly(connection property)
    private boolean enableDefaultOnAutoCommit = true;
    //default value set indicator on transactionIsolation(connection property)
    private boolean enableDefaultOnTransactionIsolation = true;


    /**
     * connection factory class,which is one implementation class of
     * 1:<class>RawConnectionFactory</class>
     * 2:<class>RawXaConnectionFactory</class>
     * 3:<class>DataSource</class>
     * 4:<class>XADataSource</class>
     */
    private Class connectionFactoryClass;
    //connection factory class name
    private String connectionFactoryClassName;
    //connection factory
    private Object connectionFactory;
    //password decoder
    private Class passwordDecoderClass;
    //password decoder class name
    private String passwordDecoderClassName;
    //pool implementation class name
    private String poolImplementClassName = FastConnectionPool.class.getName();

    //jmx register indicator
    private boolean enableJmx;
    //config print indicator on pool starting
    private boolean printConfigInfo;
    //runtime log print indicator on pool activity
    private boolean printRuntimeLog;

    //****************************************************************************************************************//
    //                                     1: constructors(5)                                                         //
    //****************************************************************************************************************//
    public BeeDataSourceConfig() {
    }

    //read configuration from properties file
    public BeeDataSourceConfig(File propertiesFile) {
        loadFromPropertiesFile(propertiesFile);
    }

    //read configuration from properties file
    public BeeDataSourceConfig(String propertiesFileName) {
        loadFromPropertiesFile(propertiesFileName);
    }

    //read configuration from properties
    public BeeDataSourceConfig(Properties configProperties) {
        loadFromProperties(configProperties);
    }

    public BeeDataSourceConfig(String driver, String url, String user, String password) {
        this.jdbcUrl = trimString(url);
        this.username = trimString(user);
        this.password = trimString(password);
        this.driverClassName = trimString(driver);
    }

    //****************************************************************************************************************//
    //                                     2: JDBC link configuration methods(10)                                     //
    //****************************************************************************************************************//
    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = trimString(username);
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = trimString(password);
    }

    public String getUrl() {
        return this.jdbcUrl;
    }

    public void setUrl(String jdbcUrl) {
        this.jdbcUrl = trimString(jdbcUrl);
    }

    public String getJdbcUrl() {
        return this.jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = trimString(jdbcUrl);
    }

    public String getDriverClassName() {
        return this.driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = trimString(driverClassName);
    }


    //****************************************************************************************************************//
    //                                3: configuration about pool inner control(30)                                   //
    //****************************************************************************************************************//
    public String getPoolName() {
        return this.poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = trimString(poolName);
    }

    public boolean isFairMode() {
        return this.fairMode;
    }

    public void setFairMode(boolean fairMode) {
        this.fairMode = fairMode;
    }

    public int getInitialSize() {
        return this.initialSize;
    }

    public void setInitialSize(int initialSize) {
        if (initialSize >= 0) this.initialSize = initialSize;
    }

    public boolean isAsyncCreateInitConnection() {
        return asyncCreateInitConnection;
    }

    public void setAsyncCreateInitConnection(boolean asyncCreateInitConnection) {
        this.asyncCreateInitConnection = asyncCreateInitConnection;
    }

    public int getMaxActive() {
        return this.maxActive;
    }

    public void setMaxActive(int maxActive) {
        if (maxActive > 0) {
            this.maxActive = maxActive;
            //fix issue:#19 Chris-2020-08-16 begin
            this.borrowSemaphoreSize = maxActive > 1 ? Math.min(maxActive / 2, NCPUS) : 1;
            //fix issue:#19 Chris-2020-08-16 end
        }
    }

    public int getBorrowSemaphoreSize() {
        return this.borrowSemaphoreSize;
    }

    public void setBorrowSemaphoreSize(int borrowSemaphoreSize) {
        if (borrowSemaphoreSize > 0) this.borrowSemaphoreSize = borrowSemaphoreSize;
    }

    public long getMaxWait() {
        return this.maxWait;
    }

    public void setMaxWait(long maxWait) {
        if (maxWait > 0) this.maxWait = maxWait;
    }

    public long getIdleTimeout() {
        return this.idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        if (idleTimeout > 0) this.idleTimeout = idleTimeout;
    }

    public long getHoldTimeout() {
        return this.holdTimeout;
    }

    public void setHoldTimeout(long holdTimeout) {
        if (holdTimeout > 0) this.holdTimeout = holdTimeout;
    }

    public String getValidTestSql() {
        return this.validTestSql;
    }

    public void setValidTestSql(String validTestSql) {
        if (!isBlank(validTestSql)) this.validTestSql = trimString(validTestSql);
    }

    public int getValidTestTimeout() {
        return this.validTestTimeout;
    }

    public void setValidTestTimeout(int validTestTimeout) {
        if (validTestTimeout >= 0) this.validTestTimeout = validTestTimeout;
    }

    public long getValidAssumeTime() {
        return this.validAssumeTime;
    }

    public void setValidAssumeTime(long validAssumeTime) {
        if (validAssumeTime >= 0) this.validAssumeTime = validAssumeTime;
    }

    public long getTimerCheckInterval() {
        return this.timerCheckInterval;
    }

    public void setTimerCheckInterval(long timerCheckInterval) {
        if (timerCheckInterval > 0) this.timerCheckInterval = timerCheckInterval;
    }

    public boolean isForceCloseUsingOnClear() {
        return this.forceCloseUsingOnClear;
    }

    public void setForceCloseUsingOnClear(boolean forceCloseUsingOnClear) {
        this.forceCloseUsingOnClear = forceCloseUsingOnClear;
    }

    public long getDelayTimeForNextClear() {
        return this.delayTimeForNextClear;
    }

    public void setDelayTimeForNextClear(long delayTimeForNextClear) {
        if (delayTimeForNextClear >= 0) this.delayTimeForNextClear = delayTimeForNextClear;
    }

    public String getPoolImplementClassName() {
        return this.poolImplementClassName;
    }

    public void setPoolImplementClassName(String poolImplementClassName) {
        if (!isBlank(poolImplementClassName)) this.poolImplementClassName = trimString(poolImplementClassName);
    }

    public boolean isEnableJmx() {
        return this.enableJmx;
    }

    public void setEnableJmx(boolean enableJmx) {
        this.enableJmx = enableJmx;
    }

    public boolean isPrintConfigInfo() {
        return this.printConfigInfo;
    }

    public void setPrintConfigInfo(boolean printConfigInfo) {
        this.printConfigInfo = printConfigInfo;
    }

    public boolean isPrintRuntimeLog() {
        return this.printRuntimeLog;
    }

    public void setPrintRuntimeLog(boolean printRuntimeLog) {
        this.printRuntimeLog = printRuntimeLog;
    }

    //****************************************************************************************************************//
    //                                     4: connection default value set methods(12)                                //
    //****************************************************************************************************************//
    public String getDefaultCatalog() {
        return this.defaultCatalog;
    }

    public void setDefaultCatalog(String defaultCatalog) {
        this.defaultCatalog = trimString(defaultCatalog);
    }

    public String getDefaultSchema() {
        return this.defaultSchema;
    }

    public void setDefaultSchema(String defaultSchema) {
        this.defaultSchema = trimString(defaultSchema);
    }

    public Boolean isDefaultReadOnly() {
        return this.defaultReadOnly;
    }

    public void setDefaultReadOnly(Boolean defaultReadOnly) {
        this.defaultReadOnly = defaultReadOnly;
    }

    public Boolean isDefaultAutoCommit() {
        return this.defaultAutoCommit;
    }

    public void setDefaultAutoCommit(Boolean defaultAutoCommit) {
        this.defaultAutoCommit = defaultAutoCommit;
    }

    public Integer getDefaultTransactionIsolationCode() {
        return this.defaultTransactionIsolationCode;
    }

    public void setDefaultTransactionIsolationCode(Integer transactionIsolationCode) {
        this.defaultTransactionIsolationCode = transactionIsolationCode;//support Informix jdbc
    }

    public String getDefaultTransactionIsolationName() {
        return this.defaultTransactionIsolationName;
    }

    public void setDefaultTransactionIsolationName(String transactionIsolationName) {
        String transactionIsolationNameTemp = trimString(transactionIsolationName);
        this.defaultTransactionIsolationCode = TransactionIsolation.getTransactionIsolationCode(transactionIsolationNameTemp);
        if (this.defaultTransactionIsolationCode != null) {
            defaultTransactionIsolationName = transactionIsolationNameTemp;
        } else {
            throw new BeeDataSourceConfigException("Invalid transaction isolation name:" + transactionIsolationNameTemp + ", value is one of[" + TRANS_LEVEL_CODE_LIST + "]");
        }
    }

    //****************************************************************************************************************//
    //                                     5: connection default value set Indicator methods(10)                      //
    //****************************************************************************************************************//
    public boolean isEnableDefaultOnCatalog() {
        return enableDefaultOnCatalog;
    }

    public void setEnableDefaultOnCatalog(boolean enableDefaultOnCatalog) {
        this.enableDefaultOnCatalog = enableDefaultOnCatalog;
    }

    public boolean isEnableDefaultOnSchema() {
        return enableDefaultOnSchema;
    }

    public void setEnableDefaultOnSchema(boolean enableDefaultOnSchema) {
        this.enableDefaultOnSchema = enableDefaultOnSchema;
    }

    public boolean isEnableDefaultOnReadOnly() {
        return enableDefaultOnReadOnly;
    }

    public void setEnableDefaultOnReadOnly(boolean enableDefaultOnReadOnly) {
        this.enableDefaultOnReadOnly = enableDefaultOnReadOnly;
    }

    public boolean isEnableDefaultOnAutoCommit() {
        return enableDefaultOnAutoCommit;
    }

    public void setEnableDefaultOnAutoCommit(boolean enableDefaultOnAutoCommit) {
        this.enableDefaultOnAutoCommit = enableDefaultOnAutoCommit;
    }

    public boolean isEnableDefaultOnTransactionIsolation() {
        return enableDefaultOnTransactionIsolation;
    }

    public void setEnableDefaultOnTransactionIsolation(boolean enableDefaultOnTransactionIsolation) {
        this.enableDefaultOnTransactionIsolation = enableDefaultOnTransactionIsolation;
    }

    //****************************************************************************************************************//
    //                                    6: connection factory class set methods(12)                                 //
    //****************************************************************************************************************//
    public Object getConnectionFactory() {
        return this.connectionFactory;
    }

    //connection factory
    public void setRawConnectionFactory(RawConnectionFactory factory) {
        this.connectionFactory = factory;
    }

    public void setRawXaConnectionFactory(RawXaConnectionFactory factory) {
        this.connectionFactory = factory;
    }

    public Class getConnectionFactoryClass() {
        return this.connectionFactoryClass;
    }

    public void setConnectionFactoryClass(Class connectionFactoryClass) {
        this.connectionFactoryClass = connectionFactoryClass;
    }

    public String getConnectionFactoryClassName() {
        return this.connectionFactoryClassName;
    }

    public void setConnectionFactoryClassName(String connectionFactoryClassName) {
        this.connectionFactoryClassName = trimString(connectionFactoryClassName);
    }

    public Class getPasswordDecoderClass() {
        return this.passwordDecoderClass;
    }

    public void setPasswordDecoderClass(Class passwordDecoderClass) {
        this.passwordDecoderClass = passwordDecoderClass;
    }

    public String getPasswordDecoderClassName() {
        return this.passwordDecoderClassName;
    }

    public void setPasswordDecoderClassName(String passwordDecoderClassName) {
        this.passwordDecoderClassName = passwordDecoderClassName;
    }

    public void removeConnectProperty(String key) {
        if (!isBlank(key)) this.connectProperties.remove(key);
    }

    public void addConnectProperty(String key, Object value) {
        if (!isBlank(key) && value != null) this.connectProperties.put(key, value);
    }

    public void addConnectProperty(String connectPropertyText) {
        if (!isBlank(connectPropertyText)) {
            for (String attribute : connectPropertyText.split("&")) {
                String[] pair = attribute.split("=");
                if (pair.length == 2) {
                    this.addConnectProperty(pair[0].trim(), pair[1].trim());
                } else {
                    pair = attribute.split(":");
                    if (pair.length == 2) {
                        this.addConnectProperty(pair[0].trim(), pair[1].trim());
                    }
                }
            }
        }
    }

    //****************************************************************************************************************//
    //                                     7: properties configuration(3)                                             //
    //****************************************************************************************************************//
    public void loadFromPropertiesFile(String filename) {
        if (isBlank(filename)) throw new IllegalArgumentException("Properties file can't be null");
        this.loadFromPropertiesFile(new File(filename));
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

            this.loadFromProperties(configProperties);
        } catch (BeeDataSourceConfigException e) {
            throw e;
        } catch (Throwable e) {
            throw new BeeDataSourceConfigException("Failed to load properties file", e);
        } finally {
            if (stream != null) try {
                stream.close();
            } catch (Throwable e) {
                CommonLog.warn("Failed to close properties file inputStream,cause:", e);
            }
        }
    }

    public void loadFromProperties(Properties configProperties) {
        if (configProperties == null || configProperties.isEmpty())
            throw new BeeDataSourceConfigException("Properties can't be null or empty");

        //1:load configuration item values from outside properties
        Map<String, Object> setValueMap = new HashMap<String, Object>(configProperties.size());
        for (String propertyName : configProperties.stringPropertyNames()) {
            setValueMap.put(propertyName, configProperties.getProperty(propertyName));
        }

        //2:inject item value from map to this dataSource config object
        setPropertiesValue(this, setValueMap);

        //3:try to find 'connectProperties' config value and put to ds config object
        this.addConnectProperty(getPropertyValue(configProperties, CONFIG_CONNECT_PROP));
        String connectPropertiesSize = getPropertyValue(configProperties, CONFIG_CONNECT_PROP_SIZE);
        if (!isBlank(connectPropertiesSize)) {
            int size = Integer.parseInt(connectPropertiesSize.trim());
            for (int i = 1; i <= size; i++)//properties index begin with 1
                this.addConnectProperty(getPropertyValue(configProperties, CONFIG_CONNECT_PROP_KEY_PREFIX + i));
        }
    }

    //****************************************************************************************************************//
    //                                    8: configuration check and connection factory create methods(4)             //
    //****************************************************************************************************************//
    //check pool configuration
    public BeeDataSourceConfig check() throws SQLException {
        if (maxActive <= 0)
            throw new BeeDataSourceConfigException("maxActive must be greater than zero");
        if (initialSize < 0)
            throw new BeeDataSourceConfigException("initialSize must not be less than zero");
        if (initialSize > this.maxActive)
            throw new BeeDataSourceConfigException("initialSize must not be greater than maxActive");
        if (borrowSemaphoreSize <= 0)
            throw new BeeDataSourceConfigException("borrowSemaphoreSize must be greater than zero");
        //fix issue:#19 Chris-2020-08-16 begin
        //if (this.borrowConcurrentSize > maxActive)
        //throw new BeeDataSourceConfigException("Pool 'borrowConcurrentSize' must not be greater than pool max size");
        //fix issue:#19 Chris-2020-08-16 end
        if (idleTimeout <= 0)
            throw new BeeDataSourceConfigException("idleTimeout must be greater than zero");
        if (holdTimeout <= 0)
            throw new BeeDataSourceConfigException("holdTimeout must be greater than zero");
        if (maxWait <= 0)
            throw new BeeDataSourceConfigException("maxWait must be greater than zero");
        //fix issue:#1 The check of validationQuerySQL has logic problem. Chris-2019-05-01 begin
        //if (this.validationQuerySQL != null && validationQuerySQL.trim().length() == 0) {
        if (isBlank(validTestSql))
            throw new BeeDataSourceConfigException("validTestSql cant be null or empty");
        if (!validTestSql.toUpperCase(Locale.US).startsWith("SELECT ")) {
            //fix issue:#1 The check of validationQuerySQL has logic problem. Chris-2019-05-01 end
            throw new BeeDataSourceConfigException("validTestSql must be start with 'select '");
        }

        Object tempConnectionFactory = null;
        if (this.connectionFactory == null) tempConnectionFactory = createConnectionFactory();

        BeeDataSourceConfig checkedConfig = new BeeDataSourceConfig();
        copyTo(checkedConfig);

        //set temp to config
        if (tempConnectionFactory != null) checkedConfig.connectionFactory = tempConnectionFactory;
        if (isBlank(checkedConfig.poolName)) checkedConfig.poolName = "FastPool-" + PoolNameIndex.getAndIncrement();
        return checkedConfig;
    }

    //copy configuration to other object
    void copyTo(BeeDataSourceConfig config) {
        //1:primitive type copy
        String fieldName = "";
        try {
            for (Field field : BeeDataSourceConfig.class.getDeclaredFields()) {
                fieldName = field.getName();
                if (!Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()) && !"connectProperties".equals(fieldName)) {
                    Object fieldValue = field.get(this);
                    if (this.printConfigInfo) CommonLog.info("{}.{}={}", this.poolName, fieldName, fieldValue);
                    field.set(config, fieldValue);
                }
            }
        } catch (Throwable e) {
            throw new BeeDataSourceConfigException("Failed to copy field[" + fieldName + "]", e);
        }

        //2:copy  'connectProperties'
        for (Map.Entry<String, Object> entry : this.connectProperties.entrySet()) {
            if (this.printConfigInfo)
                CommonLog.info("{}.connectProperties.{}={}", this.poolName, entry.getKey(), entry.getValue());
            config.addConnectProperty(entry.getKey(), entry.getValue());
        }
    }

    //create PasswordDecoder instance
    private PasswordDecoder createPasswordDecoder() {
        PasswordDecoder passwordDecoder = null;
        Class<?> passwordDecoderClass = this.passwordDecoderClass;
        if (passwordDecoderClass == null && !isBlank(this.passwordDecoderClassName)) {
            try {
                passwordDecoderClass = Class.forName(this.passwordDecoderClassName);
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to create password decoder by class:" + this.passwordDecoderClassName, e);
            }
        }

        if (passwordDecoderClass != null) {
            try {
                passwordDecoder = (PasswordDecoder) createClassInstance(passwordDecoderClass, PasswordDecoder.class, "password decoder");
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to instantiate password decoder class:" + passwordDecoderClass.getName(), e);
            }
        }
        return passwordDecoder;
    }

    //create Connection factory
    private Object createConnectionFactory() throws SQLException {
        //step1:if exists object factory,then return it
        if (this.connectionFactory != null) return this.connectionFactory;

        //step2:create connection factory by driver
        PasswordDecoder passwordDecoder = this.createPasswordDecoder();
        if (this.connectionFactoryClass == null && isBlank(this.connectionFactoryClassName)) {
            if (isBlank(this.jdbcUrl)) throw new BeeDataSourceConfigException("jdbcUrl can't be null");

            Driver connectDriver = null;
            if (!isBlank(this.driverClassName))
                connectDriver = loadDriver(this.driverClassName);
            else if (!isBlank(this.jdbcUrl))
                connectDriver = DriverManager.getDriver(this.jdbcUrl);
            if (connectDriver == null)
                throw new BeeDataSourceConfigException("Failed to load driver:" + this.driverClassName);
            if (!connectDriver.acceptsURL(this.jdbcUrl))
                throw new BeeDataSourceConfigException("jdbcUrl(" + this.jdbcUrl + ")can not match driver:" + connectDriver.getClass().getName());

            Properties configProperties = new Properties();
            configProperties.putAll(connectProperties);//copy
            if (!isBlank(this.username) && !configProperties.containsKey("user"))//set username
                configProperties.setProperty("user", this.username);

            String tempPassword = configProperties.getProperty("password");
            if (isBlank(tempPassword)) tempPassword = this.password;
            if (!isBlank(tempPassword)) {//set password
                if (passwordDecoder != null) tempPassword = passwordDecoder.decode(tempPassword);
                configProperties.setProperty("password", tempPassword);
            }
            return new ConnectionFactoryByDriver(this.jdbcUrl, connectDriver, configProperties);
        } else {//step3:create connection factory by connection factory class
            try {
                //1:load connection factory by class name
                Class<?> conFactClass = this.connectionFactoryClass != null ? this.connectionFactoryClass : Class.forName(this.connectionFactoryClassName);

                //2: check connection factory class
                Class[] parentClasses = {RawConnectionFactory.class, RawXaConnectionFactory.class, DataSource.class, XADataSource.class};

                //3:create connection factory instance
                Object factory = createClassInstance(conFactClass, parentClasses, "connection factory");

                //4:copy properties to value map(inject to dataSource or factory)
                Map<String, Object> propertyValueMap = new HashMap<String, Object>(this.connectProperties);//copy

                //5:set set username,password,jdbc-url to value map
                if (!isBlank(this.username) && !propertyValueMap.containsKey("user"))//set username
                    propertyValueMap.put("user", this.username);

                Object passwordPropVal = propertyValueMap.get("password");
                String tempPassword = passwordPropVal instanceof String ? (String) passwordPropVal : null;
                if (isBlank(tempPassword)) tempPassword = this.password;
                if (!isBlank(tempPassword)) {//set password
                    if (passwordDecoder != null) tempPassword = passwordDecoder.decode(tempPassword);
                    propertyValueMap.put("password", tempPassword);
                }

                if (!isBlank(this.jdbcUrl)) {//set jdbc url
                    if (!propertyValueMap.containsKey("url")) propertyValueMap.put("url", this.jdbcUrl);
                    if (!propertyValueMap.containsKey("URL")) propertyValueMap.put("URL", this.jdbcUrl);
                    if (!propertyValueMap.containsKey("jdbcUrl")) propertyValueMap.put("jdbcUrl", this.jdbcUrl);
                }

                //6:inject properties to connection factory or dataSource
                setPropertiesValue(factory, propertyValueMap);

                //7:return RawConnectionFactory or RawXaConnectionFactory
                if (factory instanceof RawConnectionFactory || factory instanceof RawXaConnectionFactory) {
                    return factory;
                } else if (factory instanceof XADataSource) {
                    return new XaConnectionFactoryByDriverDs((XADataSource) factory, this.username, this.password);
                } else if (factory instanceof DataSource) {
                    return new ConnectionFactoryByDriverDs((DataSource) factory, this.username, this.password);
                } else {
                    throw new BeeDataSourceConfigException("Error connection factory type:" + this.connectionFactoryClassName);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new BeeDataSourceConfigException("Failed to create connection factory by class:" + this.connectionFactoryClassName, e);
            }
        }
    }
}

