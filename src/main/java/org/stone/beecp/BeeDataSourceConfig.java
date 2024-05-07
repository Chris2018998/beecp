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

import org.stone.beecp.pool.*;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.stone.beecp.TransactionIsolation.TRANS_LEVEL_CODE_LIST;
import static org.stone.beecp.pool.ConnectionPoolStatics.*;
import static org.stone.tools.CommonUtil.*;

/**
 * Bee data source configuration object
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeDataSourceConfig implements BeeDataSourceConfigJmxBean {
    //pool name generation index which is an atomic integer start with 1
    private static final AtomicInteger PoolNameIndex = new AtomicInteger(1);
    //default exclusion list of config print(info level,still print them under debug mode)
    private static final List<String> DefaultExclusionList = Arrays.asList("username", "password", "jdbcUrl", "user", "url");

    //properties applied in a factory{@code RawConnectionFactory,RawXaConnectionFactory} to establish connections
    private final Map<String, Object> connectProperties = new HashMap<>();
    //username of a database,default is null
    private String username;
    //user password security link to database,default is null
    private String password;
    //database link url for jdbc,default is null
    private String jdbcUrl;
    //database link driver class name,default is null(if not set,pool try to search a matched driver from driver manager with valid configured url)
    private String driverClassName;
    //pool name for log trace,if null or empty,a generation name will be assigned to pool after configuration check passed
    private String poolName;
    //work mode of pool semaphore,default is false(unfair mode,I call it competition mode)
    private boolean fairMode;
    //creation size of initial connections,default is zero
    private int initialSize;
    //creation mode of initial connections;default is false(synchronization mode)
    private boolean asyncCreateInitConnection;
    //maximum of connections in pool,default is 10(default range: 10 =< number <=50)
    private int maxActive = Math.min(Math.max(10, NCPU), 50);
    //max permits size of pool semaphore
    private int borrowSemaphoreSize = Math.min(this.maxActive / 2, NCPU);
    //milliseconds: max wait time in pool to get connections,default is 8000 milliseconds(8 seconds)
    private long maxWait = SECONDS.toMillis(8);

    //seconds: max wait time effects inside a driver or a datasource to establish raw connections.
    //Two connection creation modes supported in bee datasource configuration
    //1: driver mode,driverClassName field has been set or a matched driver can be searched with url
    //2: factory mode,@see field connectionFactoryClass(supports four types)
    //* import tips: factory mode is priority for pool,if connection creation works under driver mode,this item value
    // is assigned to field loginTimeout of DriverManager on pool initialization,but the loginTimeout field is sharable,
    // in same JVM,other drivers maybe read its value,so need more careful on this item(connectTimeout),default is zero
    private int connectTimeout;
    //milliseconds: max idle time check on not borrowed connections,if timeout,then remove them from pool,default is 18000 milliseconds(3 minutes)
    private long idleTimeout = MINUTES.toMillis(3);
    //milliseconds: max inactive time check on borrowed connections,if timeout,pool recycled them by force to avoid connections leak,default is zero
    private long holdTimeout;

    //an alive test sql executed on borrowed connections,if dead,removed from pool
    private String aliveTestSql = "SELECT 1";
    //seconds: max wait time to get validation result on test connections,default is 3 seconds.
    private int aliveTestTimeout = 3;
    //milliseconds: a gap time value from last activity time to borrowed time point,needn't do test on connections,default is 500 milliseconds
    private long aliveAssumeTime = 500L;
    //milliseconds: an interval time to scan idle connections or long time hold connections,default is 18000 milliseconds(3 minutes)
    private long timerCheckInterval = MINUTES.toMillis(3);
    //indicator on direct closing borrowed connections while pool clears,default is false
    private boolean forceCloseUsingOnClear;
    //milliseconds: A wait time for borrowed connections return to pool in a loop,at end of wait,try to close returned connections,default is 3000 milliseconds
    private long delayTimeForNextClear = 3000L;
    //error code list check on vendorCode of thrown sql exceptions,if matched,connections evicted from pool,@see field vendorCode of SQLException.class
    private List<Integer> sqlExceptionCodeList;
    //error state list check on SQLState of thrown sql exceptions,if matched,connections evicted from pool,@see field SQLState of SQLException.class
    private List<String> sqlExceptionStateList;

    //an initial value assigned to property catalog of new connections,refer to method{@code Connection.setCatalog(String)}
    private String defaultCatalog;
    //an initial value assigned to property schema of new connections,refer to method{@code Connection.setSchema(String)}
    private String defaultSchema;
    //an initial value assigned to property readOnly of new connections,refer to method{@code Connection.setReadOnly(boolean)}
    private Boolean defaultReadOnly;
    //an initial value assigned to property autoCommit of new connections,refer to method{@code Connection.setAutoCommit(boolean)}
    private Boolean defaultAutoCommit;
    //an initial value assigned to property transactionIsolation of new connections,refer to method{@code Connection.setTransactionIsolation(int)}
    private Integer defaultTransactionIsolationCode;
    //transaction isolation name,which can get a mapping code as initial value of property transactionIsolation
    private String defaultTransactionIsolationName;

    //enable indicator to set default value on property catalog
    private boolean enableDefaultOnCatalog = true;
    //enable indicator to set default value on property schema
    private boolean enableDefaultOnSchema = true;
    //enable indicator to set default value on property readOnly
    private boolean enableDefaultOnReadOnly = true;
    //enable indicator to set default value on property autoCommit
    private boolean enableDefaultOnAutoCommit = true;
    //enable indicator to set default value on property transactionIsolation
    private boolean enableDefaultOnTransactionIsolation = true;

    //dirty force indicator on schema property(supports recover under transaction in PG database)
    private boolean forceDirtyOnSchemaAfterSet;
    //dirty force indicator on catalog property(supports recover under transaction in PG database)
    private boolean forceDirtyOnCatalogAfterSet;


    //thread factory instance(creation order-1)
    private BeeConnectionPoolThreadFactory threadFactory;
    //thread factory class(creation order-2)
    private Class<? extends BeeConnectionPoolThreadFactory> threadFactoryClass;
    //thread factory class name(creation order-3),if not set,default factory will be applied in pool
    private String threadFactoryClassName = ConnectionPoolThreadFactory.class.getName();

    /**
     * connection factory class,which must be implement one of the below four interfaces
     * 1: <class>RawConnectionFactory</class>
     * 2: <class>RawXaConnectionFactory</class>
     * 3: <class>DataSource</class>
     * 4: <class>XADataSource</class>
     */
    //connection factory instance
    private Object connectionFactory;
    //connection factory class
    private Class connectionFactoryClass;
    //connection factory class name
    private String connectionFactoryClassName;

    /**
     * connections eviction check on thrown sql exceptions by customization
     * eviction check priority logic
     * 1: if exists a predication,only check with predication
     * 2: if not exists,priority order: error code check,sql state check
     */
    //eviction predicate
    private BeeConnectionPredicate evictPredicate;
    //eviction predicate class
    private Class<? extends BeeConnectionPredicate> evictPredicateClass;
    //eviction predicate class name
    private String evictPredicateClassName;

    /**
     * A short lifecycle object and used to decode jdbc link info(url,username,password)in pool initialization check
     */
    //decoder
    private BeeJdbcLinkInfoDecoder jdbcLinkInfoDecoder;
    //decoder class
    private Class<? extends BeeJdbcLinkInfoDecoder> jdbcLinkInfoDecoderClass;
    //decoder class name
    private String jdbcLinkInfoDecoderClassName;

    //enable indicator to register configuration and pool to Jmx,default is false
    private boolean enableJmx;
    //enable indicator to print pool runtime log,default is false
    private boolean printRuntimeLog;
    //enable indicator to print configuration items on pool initialization,default is false
    private boolean printConfigInfo;
    //config items exclusion list on info-level print
    private List<String> configPrintExclusionList = new ArrayList<>(DefaultExclusionList);


    //pool implementation class name,if not be set,a default implementation applied in bee datasource
    private String poolImplementClassName = FastConnectionPool.class.getName();

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
            this.borrowSemaphoreSize = maxActive > 1 ? Math.min(maxActive / 2, NCPU) : 1;
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
        if (maxWait > 0L) this.maxWait = maxWait;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        if (connectTimeout >= 0) this.connectTimeout = connectTimeout;
    }

    public long getIdleTimeout() {
        return this.idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        if (idleTimeout > 0L) this.idleTimeout = idleTimeout;
    }

    public long getHoldTimeout() {
        return this.holdTimeout;
    }

    public void setHoldTimeout(long holdTimeout) {
        if (holdTimeout >= 0L) this.holdTimeout = holdTimeout;
    }

    public String getAliveTestSql() {
        return this.aliveTestSql;
    }

    public void setAliveTestSql(String aliveTestSql) {
        if (isNotBlank(aliveTestSql)) this.aliveTestSql = trimString(aliveTestSql);
    }

    public int getAliveTestTimeout() {
        return this.aliveTestTimeout;
    }

    public void setAliveTestTimeout(int aliveTestTimeout) {
        if (aliveTestTimeout >= 0) this.aliveTestTimeout = aliveTestTimeout;
    }

    public long getAliveAssumeTime() {
        return this.aliveAssumeTime;
    }

    public void setAliveAssumeTime(long aliveAssumeTime) {
        if (aliveAssumeTime >= 0L) this.aliveAssumeTime = aliveAssumeTime;
    }

    public long getTimerCheckInterval() {
        return this.timerCheckInterval;
    }

    public void setTimerCheckInterval(long timerCheckInterval) {
        if (timerCheckInterval > 0L) this.timerCheckInterval = timerCheckInterval;
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
        if (delayTimeForNextClear >= 0L) this.delayTimeForNextClear = delayTimeForNextClear;
    }

    public List<Integer> getSqlExceptionCodeList() {
        return sqlExceptionCodeList;
    }

    public void addSqlExceptionCode(int code) {
        if (sqlExceptionCodeList == null) sqlExceptionCodeList = new ArrayList<>(1);
        if (!this.sqlExceptionCodeList.contains(code)) this.sqlExceptionCodeList.add(code);
    }

    public void removeSqlExceptionCode(int code) {
        if (sqlExceptionCodeList != null) this.sqlExceptionCodeList.remove(Integer.valueOf(code));
    }

    public List<String> getSqlExceptionStateList() {
        return sqlExceptionStateList;
    }

    public void addSqlExceptionState(String state) {
        if (sqlExceptionStateList == null) sqlExceptionStateList = new ArrayList<>(1);
        if (!this.sqlExceptionStateList.contains(state)) this.sqlExceptionStateList.add(state);
    }

    public void removeSqlExceptionState(String state) {
        if (sqlExceptionStateList != null) this.sqlExceptionStateList.remove(state);
    }

    public String getPoolImplementClassName() {
        return this.poolImplementClassName;
    }

    public void setPoolImplementClassName(String poolImplementClassName) {
        if (isNotBlank(poolImplementClassName)) this.poolImplementClassName = trimString(poolImplementClassName);
    }

    public boolean isEnableJmx() {
        return this.enableJmx;
    }

    public void setEnableJmx(boolean enableJmx) {
        this.enableJmx = enableJmx;
    }

    public boolean isPrintRuntimeLog() {
        return this.printRuntimeLog;
    }

    public void setPrintRuntimeLog(boolean printRuntimeLog) {
        this.printRuntimeLog = printRuntimeLog;
    }

    public boolean isPrintConfigInfo() {
        return this.printConfigInfo;
    }

    public void setPrintConfigInfo(boolean printConfigInfo) {
        this.printConfigInfo = printConfigInfo;
    }

    public void clearAllConfigPrintExclusion() {
        this.configPrintExclusionList.clear();
    }

    public void addConfigPrintExclusion(String fieldName) {
        if (!configPrintExclusionList.contains(fieldName))
            this.configPrintExclusionList.add(fieldName);
    }

    public boolean removeConfigPrintExclusion(String fieldName) {
        return this.configPrintExclusionList.remove(fieldName);
    }

    public boolean existConfigPrintExclusion(String fieldName) {
        return this.configPrintExclusionList.contains(fieldName);
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

    public boolean isForceDirtyOnSchemaAfterSet() {
        return forceDirtyOnSchemaAfterSet;
    }

    public void setForceDirtyOnSchemaAfterSet(boolean forceDirtyOnSchemaAfterSet) {
        this.forceDirtyOnSchemaAfterSet = forceDirtyOnSchemaAfterSet;
    }

    public boolean isForceDirtyOnCatalogAfterSet() {
        return forceDirtyOnCatalogAfterSet;
    }

    public void setForceDirtyOnCatalogAfterSet(boolean forceDirtyOnCatalogAfterSet) {
        this.forceDirtyOnCatalogAfterSet = forceDirtyOnCatalogAfterSet;
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

    public Class<? extends BeeConnectionPoolThreadFactory> getThreadFactoryClass() {
        return threadFactoryClass;
    }

    public void setThreadFactoryClass(Class<? extends BeeConnectionPoolThreadFactory> factoryClass) {
        this.threadFactoryClass = factoryClass;
    }

    public String getThreadFactoryClassName() {
        return threadFactoryClassName;
    }

    public void setThreadFactoryClassName(String threadFactoryClassName) {
        if (isNotBlank(threadFactoryClassName)) this.threadFactoryClassName = trimString(threadFactoryClassName);
    }

    public BeeConnectionPoolThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public void setThreadFactory(BeeConnectionPoolThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
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

    public Class<? extends BeeConnectionPredicate> getEvictPredicateClass() {
        return evictPredicateClass;
    }

    public void setEvictPredicateClass(Class<? extends BeeConnectionPredicate> evictPredicateClass) {
        this.evictPredicateClass = evictPredicateClass;
    }

    public String getEvictPredicateClassName() {
        return evictPredicateClassName;
    }

    public void setEvictPredicateClassName(String evictPredicateClassName) {
        this.evictPredicateClassName = evictPredicateClassName;
    }

    public BeeConnectionPredicate getEvictPredicate() {
        return evictPredicate;
    }

    public void setEvictPredicate(BeeConnectionPredicate evictPredicate) {
        this.evictPredicate = evictPredicate;
    }

    public Class<? extends BeeJdbcLinkInfoDecoder> getJdbcLinkInfoDecoderClass() {
        return this.jdbcLinkInfoDecoderClass;
    }

    public void setJdbcLinkInfoDecoderClass(Class<? extends BeeJdbcLinkInfoDecoder> jdbcLinkInfoDecoderClass) {
        this.jdbcLinkInfoDecoderClass = jdbcLinkInfoDecoderClass;
    }

    public String getJdbcLinkInfoDecoderClassName() {
        return this.jdbcLinkInfoDecoderClassName;
    }

    public void setJdbcLinkInfoDecoderClassName(String jdbcLinkInfoDecoderClassName) {
        this.jdbcLinkInfoDecoderClassName = jdbcLinkInfoDecoderClassName;
    }

    public BeeJdbcLinkInfoDecoder getJdbcLinkInfoDecoder() {
        return jdbcLinkInfoDecoder;
    }

    public void setJdbcLinkInfoDecoder(BeeJdbcLinkInfoDecoder jdbcLinkInfoDecoder) {
        this.jdbcLinkInfoDecoder = jdbcLinkInfoDecoder;
    }

    public Object getConnectProperty(String key) {
        return this.connectProperties.get(key);
    }

    public Object removeConnectProperty(String key) {
        return this.connectProperties.remove(key);
    }

    public void addConnectProperty(String key, Object value) {
        if (isNotBlank(key) && value != null) this.connectProperties.put(key, value);
    }

    public void addConnectProperty(String connectPropertyText) {
        if (isNotBlank(connectPropertyText)) {
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
        if (isBlank(filename))
            throw new IllegalArgumentException("Configuration file name can't be null or empty");
        if (!filename.toLowerCase().endsWith(".properties"))
            throw new IllegalArgumentException("Configuration file name file must end with '.properties'");

        File file = new File(filename);
        if (file.exists()) {
            this.loadFromPropertiesFile(file);
        } else {//try to load file from classpath
            Class<BeeDataSourceConfig> selfClass = BeeDataSourceConfig.class;
            InputStream propertiesStream = selfClass.getResourceAsStream(filename);
            if (propertiesStream == null) propertiesStream = selfClass.getClassLoader().getResourceAsStream(filename);
            if (propertiesStream == null)
                throw new IllegalArgumentException("Not found configuration file:" + filename);

            Properties prop = new Properties();
            try {
                prop.load(propertiesStream);
                loadFromProperties(prop);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to load configuration properties file:" + filename, e);
            } finally {
                try {
                    propertiesStream.close();
                } catch (Throwable e) {
                    //do nothing
                }
            }
        }
    }

    public void loadFromPropertiesFile(File file) {
        if (file == null) throw new IllegalArgumentException("Configuration properties file can't be null");
        if (!file.exists()) throw new IllegalArgumentException("Configuration properties file not found:" + file);
        if (!file.isFile()) throw new IllegalArgumentException("Target object is not a valid file");
        if (!file.getAbsolutePath().toLowerCase(Locale.US).endsWith(".properties"))
            throw new IllegalArgumentException("Target file is not a properties file");

        InputStream stream = null;
        try {
            stream = Files.newInputStream(file.toPath());
            Properties configProperties = new Properties();
            configProperties.load(stream);

            this.loadFromProperties(configProperties);
        } catch (BeeDataSourceConfigException e) {
            throw e;
        } catch (Throwable e) {
            throw new BeeDataSourceConfigException("Failed to load configuration properties file:" + file, e);
        } finally {
            if (stream != null) try {
                stream.close();
            } catch (Throwable e) {
                CommonLog.warn("Failed to close inputStream of configuration properties file:{}", file, e);
            }
        }
    }

    public void loadFromProperties(Properties configProperties) {
        if (configProperties == null || configProperties.isEmpty())
            throw new IllegalArgumentException("Configuration properties can't be null or empty");

        //1:load configuration item values from outside properties
        synchronized (configProperties) {//synchronization mode
            Map<String, Object> setValueMap = new HashMap<>(configProperties.size());
            for (String propertyName : configProperties.stringPropertyNames()) {
                setValueMap.put(propertyName, configProperties.getProperty(propertyName));
            }

            //2: exclude some special keys in setValueMap
            setValueMap.remove(CONFIG_CONNECT_PROP);//remove item if exists in properties file before injection
            setValueMap.remove(CONFIG_SQL_EXCEPTION_CODE);//remove item if exists in properties file before injection
            setValueMap.remove(CONFIG_SQL_EXCEPTION_STATE);//remove item if exists in properties file before injection
            setValueMap.remove(CONFIG_CONFIG_PRINT_EXCLUSION_LIST);//remove item if exists in properties file before injection
            setPropertiesValue(this, setValueMap);

            //3:try to find 'connectProperties' config value and put to ds config object
            this.addConnectProperty(getPropertyValue(configProperties, CONFIG_CONNECT_PROP));
            String connectPropertiesSize = getPropertyValue(configProperties, CONFIG_CONNECT_PROP_SIZE);
            if (isNotBlank(connectPropertiesSize)) {
                int size = Integer.parseInt(connectPropertiesSize.trim());
                for (int i = 1; i <= size; i++)//properties index begin with 1
                    this.addConnectProperty(getPropertyValue(configProperties, CONFIG_CONNECT_PROP_KEY_PREFIX + i));
            }

            //4:try to load sql exception fatal code and fatal state
            String sqlExceptionCode = getPropertyValue(configProperties, CONFIG_SQL_EXCEPTION_CODE);
            String sqlExceptionState = getPropertyValue(configProperties, CONFIG_SQL_EXCEPTION_STATE);
            if (isNotBlank(sqlExceptionCode)) {
                for (String code : sqlExceptionCode.trim().split(",")) {
                    try {
                        this.addSqlExceptionCode(Integer.parseInt(code));
                    } catch (NumberFormatException e) {
                        throw new BeeDataSourceConfigException(code + " is not a valid SQLException error code");
                    }
                }
            }

            if (isNotBlank(sqlExceptionState)) {
                for (String state : sqlExceptionState.trim().split(",")) {
                    this.addSqlExceptionState(state);
                }
            }

            //5:try to load exclusion list on config print
            String exclusionListText = getPropertyValue(configProperties, CONFIG_CONFIG_PRINT_EXCLUSION_LIST);
            if (isNotBlank(exclusionListText)) {
                this.clearAllConfigPrintExclusion();//remove existed exclusion
                for (String exclusion : exclusionListText.trim().split(",")) {
                    this.addConfigPrintExclusion(exclusion);
                }
            }
        }//synchronized end
    }

    //****************************************************************************************************************//
    //                                    8: configuration check and connection factory create methods(4)             //
    //****************************************************************************************************************//

    /**
     * configuration items check
     *
     * @return a check passed config object
     * @throws RuntimeException if check failed on some configuration items
     * @throws SQLException     if specified driver not accept jdbc url or failed to get a matched driver from driverManager
     */
    public BeeDataSourceConfig check() throws SQLException {
        if (initialSize > maxActive)
            throw new BeeDataSourceConfigException("initialSize must not be greater than maxActive");
        if (!aliveTestSql.toUpperCase(Locale.US).startsWith("SELECT ")) {
            //fix issue:#1 The check of validationQuerySQL has logic problem. Chris-2019-05-01 end
            throw new BeeDataSourceConfigException("Alive test sql must be start with 'select '");
        }

        Object connectionFactory = createConnectionFactory();
        BeeConnectionPoolThreadFactory threadFactory = this.createThreadFactory();
        BeeConnectionPredicate predicate = this.createConnectionEvictPredicate();

        BeeDataSourceConfig checkedConfig = new BeeDataSourceConfig();
        copyTo(checkedConfig);

        //set some factories to config
        checkedConfig.threadFactory = threadFactory;
        this.connectionFactory = connectionFactory;
        checkedConfig.connectionFactory = connectionFactory;
        checkedConfig.evictPredicate = predicate;
        if (isBlank(checkedConfig.poolName)) checkedConfig.poolName = "FastPool-" + PoolNameIndex.getAndIncrement();
        if (checkedConfig.printConfigInfo) printConfiguration(checkedConfig);

        return checkedConfig;
    }

    //copy configuration info to other from local
    void copyTo(BeeDataSourceConfig config) {
        String fieldName = "";
        try {
            for (Field field : BeeDataSourceConfig.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;

                fieldName = field.getName();
                if ("configPrintExclusionList".equals(fieldName)) {//copy 'exclusionConfigPrintList'
                    config.configPrintExclusionList = new ArrayList<>(configPrintExclusionList);//support copy on an empty list

                } else if ("connectProperties".equals(fieldName)) {//copy 'connectProperties'
                    for (Map.Entry<String, Object> entry : this.connectProperties.entrySet())
                        config.addConnectProperty(entry.getKey(), entry.getValue());
                } else if ("sqlExceptionCodeList".equals(fieldName)) {//copy 'sqlExceptionCodeList'
                    if (this.sqlExceptionCodeList != null && !sqlExceptionCodeList.isEmpty())
                        config.sqlExceptionCodeList = new ArrayList<>(sqlExceptionCodeList);
                } else if ("sqlExceptionStateList".equals(fieldName)) {//copy 'sqlExceptionStateList'
                    if (this.sqlExceptionStateList != null && !sqlExceptionStateList.isEmpty())
                        config.sqlExceptionStateList = new ArrayList<>(sqlExceptionStateList);
                } else {//other config items
                    field.set(config, field.get(this));
                }
            }
        } catch (Throwable e) {
            throw new BeeDataSourceConfigException("Failed to copy field[" + fieldName + "]", e);
        }
    }

    //create BeeJdbcLinkInfoDecoder instance
    private BeeJdbcLinkInfoDecoder createJdbcLinkInfoDecoder() {
        if (jdbcLinkInfoDecoder != null) return this.jdbcLinkInfoDecoder;

        //step2: create link info decoder
        if (jdbcLinkInfoDecoderClass != null || isNotBlank(jdbcLinkInfoDecoderClassName)) {
            Class<?> decoderClass = null;
            try {
                decoderClass = jdbcLinkInfoDecoderClass != null ? jdbcLinkInfoDecoderClass : Class.forName(jdbcLinkInfoDecoderClassName);
                return (BeeJdbcLinkInfoDecoder) createClassInstance(decoderClass, BeeJdbcLinkInfoDecoder.class, "jdbc link info decoder");
            } catch (ClassNotFoundException e) {
                throw new BeeDataSourceConfigException("Failed to create jdbc link info decoder with class[" + jdbcLinkInfoDecoderClassName + "]", e);
            } catch (BeeDataSourceConfigException e) {
                throw e;
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to create sql exception predication with class[" + decoderClass + "]", e);
            }
        }
        return null;
    }

    //create Connection factory
    private Object createConnectionFactory() throws SQLException {
        //step1:if exists object factory,then return it
        if (this.connectionFactory != null) return this.connectionFactory;

        //step2:create connection factory with driver
        Properties jdbcLinkInfoProperties = getJdbcLinkInfoProperties();
        BeeJdbcLinkInfoDecoder jdbcLinkInfoDecoder = this.createJdbcLinkInfoDecoder();
        if (this.connectionFactoryClass == null && isBlank(this.connectionFactoryClassName)) {
            //step2.1: prepare jdbc url
            String url = jdbcLinkInfoProperties.getProperty("url");
            if (isBlank(url)) throw new BeeDataSourceConfigException("jdbcUrl can't be null");
            if (jdbcLinkInfoDecoder != null) url = jdbcLinkInfoDecoder.decodeUrl(url);//decode url

            //step2.2: find a matched driver
            Driver driver;
            if (isNotBlank(this.driverClassName)) {
                driver = loadDriver(this.driverClassName);
                if (!driver.acceptsURL(url))
                    throw new BeeDataSourceConfigException("jdbcUrl(" + url + ")can not match configured driver[" + driverClassName + "]");
            } else {
                driver = DriverManager.getDriver(url);//try to get a matched driver with url,if not found,a SQLException will be thrown from driverManager
            }

            //step2.3: get username and password
            String username = jdbcLinkInfoProperties.getProperty("user");
            String password = jdbcLinkInfoProperties.getProperty("password");

            //step2.4: decode username and password
            if (jdbcLinkInfoDecoder != null) {
                if (isNotBlank(username))
                    username = jdbcLinkInfoDecoder.decodeUsername(username);
                if (isNotBlank(password))
                    password = jdbcLinkInfoDecoder.decodePassword(password);
            }

            //step2.5: make a copy from connect properties
            Properties localConnectProperties = new Properties();
            localConnectProperties.putAll(this.connectProperties);

            //2.6: set username and password to local connectProperties
            if (isNotBlank(username)) {
                localConnectProperties.setProperty("user", username);
                if (isNotBlank(password)) localConnectProperties.setProperty("password", password);
            }

            //step2.7: create a new connection factory with a driver and jdbc link info properties
            return new ConnectionFactoryByDriver(url, driver, localConnectProperties);
        } else {//step3:create connection factory with connection factory class
            Class<?> conFactClass = null;
            try {
                //3.1: load connection factory class with class name
                conFactClass = this.connectionFactoryClass != null ? this.connectionFactoryClass : Class.forName(this.connectionFactoryClassName);

                //3.2: check connection factory class
                Class[] parentClasses = {RawConnectionFactory.class, RawXaConnectionFactory.class, DataSource.class, XADataSource.class};

                //3.3: create connection factory instance
                Object factory = createClassInstance(conFactClass, parentClasses, "connection factory");

                //3.4: create a copy on local connectProperties
                Map<String, Object> localConnectProperties = new HashMap<>(this.connectProperties);//copy

                //3.5: set jdbc link info
                String url = jdbcLinkInfoProperties.getProperty("url");
                String username = jdbcLinkInfoProperties.getProperty("user");
                String password = jdbcLinkInfoProperties.getProperty("password");

                //3.6: decode jdbc link info
                if (jdbcLinkInfoDecoder != null) {//then execute the decoder
                    if (isNotBlank(url))
                        url = jdbcLinkInfoDecoder.decodeUrl(url);
                    if (isNotBlank(username))
                        username = jdbcLinkInfoDecoder.decodeUsername(username);
                    if (isNotBlank(password))
                        password = jdbcLinkInfoDecoder.decodePassword(password);
                }

                //3.7: reset jdbc url to the target map
                if (isNotBlank(url)) {//reset url to properties map with three key names
                    localConnectProperties.put("url", url);
                    localConnectProperties.put("URL", url);
                    localConnectProperties.put("jdbcUrl", url);
                }

                //3.8: set username and password to local connectProperties
                if (isNotBlank(username)) {
                    localConnectProperties.put("user", username);
                    if (isNotBlank(password))
                        localConnectProperties.put("password", password);
                }

                //3.9: inject properties to connection factory or dataSource
                setPropertiesValue(factory, localConnectProperties);

                //3.10: return RawConnectionFactory or RawXaConnectionFactory
                if (factory instanceof RawConnectionFactory || factory instanceof RawXaConnectionFactory) {
                    return factory;
                } else if (factory instanceof XADataSource) {
                    return new XaConnectionFactoryByDriverDs((XADataSource) factory, username, password);
                } else {//here,factory must be a datasource(only support 4 types,because that factory class type check before creation)
                    return new ConnectionFactoryByDriverDs((DataSource) factory, username, password);
                }
            } catch (ClassNotFoundException e) {
                throw new BeeDataSourceConfigException("Not found connection factory class[" + conFactClass + "]", e);
            } catch (BeeDataSourceConfigException e) {
                throw e;
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to create connection factory with class[" + conFactClass + "]", e);
            }
        }
    }

    //look up jdbc link info
    private Properties getJdbcLinkInfoProperties() {
        String url = this.jdbcUrl;//note:jdbc url is a base info
        String username = this.username;
        String password = this.password;

        if (isBlank(url)) {
            url = (String) this.connectProperties.get("url");
            if (isBlank(url)) url = (String) connectProperties.get("URL");
            if (isBlank(url)) url = (String) connectProperties.get("jdbcUrl");
            if (isNotBlank(url)) {//url found from connectProperties
                username = (String) connectProperties.get("user");
                password = (String) connectProperties.get("password");
            } else {
                url = System.getProperty("beecp.url");
                if (isBlank(url)) url = System.getProperty("beecp.URL");
                if (isBlank(url)) url = System.getProperty("beecp.jdbcUrl");
                if (isNotBlank(url)) {//url found from system properties
                    username = System.getProperty("beecp.user");
                    password = System.getProperty("beecp.password");
                }
            }
        }

        Properties jdbcLinkInfoProperties = new Properties();
        if (isNotBlank(url)) jdbcLinkInfoProperties.put("url", url);
        if (isNotBlank(username)) jdbcLinkInfoProperties.put("user", username);
        if (isNotBlank(password)) jdbcLinkInfoProperties.put("password", password);
        return jdbcLinkInfoProperties;
    }

    //create Thread factory
    private BeeConnectionPredicate createConnectionEvictPredicate() throws BeeDataSourceConfigException {
        //step1:if exists predication,then return it
        if (this.evictPredicate != null) return this.evictPredicate;

        //step2: create SQLExceptionPredication
        if (evictPredicateClass != null || isNotBlank(evictPredicateClassName)) {
            Class<?> predicationClass = null;
            try {
                predicationClass = evictPredicateClass != null ? evictPredicateClass : Class.forName(evictPredicateClassName);
                return (BeeConnectionPredicate) createClassInstance(predicationClass, BeeConnectionPredicate.class, "sql exception predicate");
            } catch (ClassNotFoundException e) {
                throw new BeeDataSourceConfigException("Not found sql exception predicate class[" + threadFactoryClassName + "]", e);
            } catch (BeeDataSourceConfigException e) {
                throw e;
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to create sql exception predicate with class[" + predicationClass + "]", e);
            }
        }

        return null;
    }

    //create Thread factory
    private BeeConnectionPoolThreadFactory createThreadFactory() throws BeeDataSourceConfigException {
        //step1:if exists thread factory,then return it
        if (this.threadFactory != null) return this.threadFactory;

//        //step2: configuration of thread factory
//        if (this.threadFactoryClass == null && isBlank(this.threadFactoryClassName))
//            throw new BeeDataSourceConfigException("Must provide one of config items[threadFactory,threadFactoryClass,threadFactoryClassName]");

        //step3: create thread factory with class or class name
        Class<?> threadFactClass = null;
        try {
            threadFactClass = threadFactoryClass != null ? threadFactoryClass : Class.forName(threadFactoryClassName);
            return (BeeConnectionPoolThreadFactory) createClassInstance(threadFactClass, BeeConnectionPoolThreadFactory.class, "pool thread factory");
        } catch (ClassNotFoundException e) {
            throw new BeeDataSourceConfigException("Not found thread factory class[" + threadFactoryClassName + "]", e);
        } catch (BeeDataSourceConfigException e) {
            throw e;
        } catch (Throwable e) {
            throw new BeeDataSourceConfigException("Failed to create pool thread factory with class[" + threadFactClass + "]", e);
        }
    }

    //print check passed configuration
    private void printConfiguration(BeeDataSourceConfig config) {
        String poolName = config.poolName;
        List<String> exclusionList = config.configPrintExclusionList;
        CommonLog.info("................................................BeeCP({})configuration[start]................................................", poolName);
        try {
            for (Field field : BeeDataSourceConfig.class.getDeclaredFields()) {
                String fieldName = field.getName();
                if (Modifier.isStatic(field.getModifiers())) continue;

                if (exclusionList.contains(fieldName)) {//debug print
                    if ("connectProperties".equals(fieldName)) {
                        for (Map.Entry<String, Object> entry : config.connectProperties.entrySet())
                            CommonLog.debug("BeeCP({}).connectProperties.{}={}", poolName, entry.getKey(), entry.getValue());
                    } else {//other config items
                        CommonLog.debug("BeeCP({}).{}={}", poolName, fieldName, field.get(config));
                    }
                } else {//info print
                    if ("connectProperties".equals(fieldName)) {
                        for (Map.Entry<String, Object> entry : config.connectProperties.entrySet()) {
                            if (!exclusionList.contains(entry.getKey()))
                                CommonLog.info("BeeCP({}).connectProperties.{}={}", poolName, entry.getKey(), entry.getValue());
                        }
                    } else {//other config items
                        CommonLog.info("BeeCP({}).{}={}", poolName, fieldName, field.get(config));
                    }
                }
            }
        } catch (Throwable e) {
            CommonLog.warn("BeeCP({})failed to print configuration", poolName, e);
        }
        CommonLog.info("................................................BeeCP({})configuration[end]................................................", poolName);
    }
}

