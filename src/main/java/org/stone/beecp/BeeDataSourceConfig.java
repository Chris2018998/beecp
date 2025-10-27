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

import org.stone.beecp.pool.ConnectionFactoryByDriver;
import org.stone.beecp.pool.ConnectionFactoryByDriverDs;
import org.stone.beecp.pool.XaConnectionFactoryByDriverDs;
import org.stone.tools.exception.BeanException;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.security.InvalidParameterException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.stone.beecp.BeeTransactionIsolationNames.TRANS_ISOLATION_CODE_LIST;
import static org.stone.beecp.pool.ConnectionPoolStatics.*;
import static org.stone.tools.BeanUtil.*;
import static org.stone.tools.CommonUtil.*;
import static org.stone.tools.logger.LogPrinterFactory.CommonLogPrinter;

/**
 * Bee data source configuration object,which is not thread-safe.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeDataSourceConfig implements BeeDataSourceConfigMBean {
    //An atomic integer to generate sequence value append to pool name as suffix,its value starts with 1
    private static final AtomicInteger PoolNameIndex = new AtomicInteger(1);
    //A list of field name,not be log print during pool initialization, default that five field names in list
    private static final List<String> DefaultExclusionList = Arrays.asList("username", "password", "jdbcUrl", "user", "url");
    //23: An exclusion list of configuration print,default is copies from {@code DefaultExclusionList}
    private final List<String> exclusionListOfPrint = new ArrayList<>(DefaultExclusionList);
    //24: A map stores some properties of connection provider,these properties are injected to provider during pool initialization
    private final Map<String, Object> connectionFactoryProperties = new HashMap<>(1);

    //1: Username link to database,default is none
    private String username;
    //2: Password link to database,default is none
    private String password;
    //3: Url link to database,default is none
    private String jdbcUrl;
    //4: Jdbc driver class name,default is none; if not set, pool attempt to search a match driver with the set jdbc url.
    private String driverClassName;

    //5: Pool name,default is none; if not set,a name generated with {@code PoolNameIndex} for it
    private String poolName;
    //6: Pool mode,default is false,unfair mode
    private boolean fairMode;
    //7: Initialization size of pooled connections
    private int initialSize;
    //8: Creation mode of initialization connections;if it is true,pool use a thread to create them;default is false
    private boolean asyncCreateInitConnection;
    //9: Maximum of connections in pool,default value is calculated by formula
    private int maxActive = Math.min(Math.max(10, NCPU), 50);
    //10: Maximum of semaphore permits to control concurrency on connections get,default value is calculated by formula
    private int semaphoreSize = Math.min(this.maxActive / 2, NCPU);
    //11: A flag to enable or disable pool thread local to cache last borrowed connection(false can be used to support virtual threads)
    private boolean useThreadLocal = true;
    //12: Milliseconds,max time to get a connection from pool for a borrower;default is 8000 milliseconds(8 seconds)
    private long maxWait = 8000L;
    //13: Milliseconds,Max time of connections idle in pool;default is 180000 milliseconds(3 minutes)
    private long idleTimeout = 180000L;
    //14: Milliseconds,max time of connections not used by borrowers;default is zero
    private long holdTimeout;
    //15: Milliseconds: interval time of pool timer to clear timeout connections(idle timeout and hold timeout),default is 180000 milliseconds(3 minutes)
    private long intervalOfClearTimeout = 180000L;
    //16: A flag to recycle borrowed connections and remove them from pool when pool shutdown,default is false.
    private boolean forceRecycleBorrowedOnClose;
    //17: Milliseconds,A spin park time to wait borrowed connections self return to pool during when pool shutdown,default is 3000 milliseconds
    private long parkTimeForRetry = 3000L;
    //18: A flag to register configuration and pool to JMX server
    private boolean registerMbeans;
    //19: A flag to print pool working logs,default is false
    private boolean printRuntimeLogs;
    //20: A flag to print configured items by logs after configuration check passed
    private boolean printConfiguration;
    //21: Class name of pool implementation,default is {@code FastConnectionPool}
    private String poolImplementClassName;

    //25: Test sql on borrowed connections to check them whether alive,default is "SELECT 1"
    private String aliveTestSql = "SELECT 1";
    //26: Seconds,max wait time of pool to get alive test result from borrowed connections,default is 3 seconds.
    private int aliveTestTimeout = 3;
    //27: Milliseconds: A threshold time for borrowed connections,if gap time of them is less than it,not need do alive test on them,default is 500 milliseconds;(Gap time = (time at borrowed) - (last used))
    private long aliveAssumeTime = 500L;

    //28: Default value to {@code java.sql.Connection.setCatalog(String)} on created connections and released connections
    private String defaultCatalog;
    //29: Default value to {@code java.sql.Connection.setSchema(String)} on created connections and released connections
    private String defaultSchema;
    //30: Default value to {@code java.sql.Connection.Connection.setReadOnly(boolean)} on created connections and released connections
    private Boolean defaultReadOnly;
    //31: Default value to {@code java.sql.Connection.Connection.setAutoCommit(boolean)} on created connections and released connections
    private Boolean defaultAutoCommit;
    //32: Default value to {@code java.sql.Connection.setTransactionIsolation(int)} on created connections and released connections
    private Integer defaultTransactionIsolation;
    //33: Name of transactionIsolation,a mapping value of{@code defaultTransactionIsolation} retrieved by it when pool initialization
    private String defaultTransactionIsolationName;
    //34: A flag to enable catalog default setting on new connections,default is true
    private boolean useDefaultCatalog = true;
    //35: A flag to enable schema default setting on new connections,default is true
    private boolean useDefaultSchema = true;
    //36: A flag to enable readonly default setting on new connections,default is true
    private boolean useDefaultReadOnly = true;
    //37: A flag to enable autoCommit default setting on new connections,default is true
    private boolean useDefaultAutoCommit = true;
    //38: A flag to enable transactionIsolation default setting on new connections,default is true
    private boolean useDefaultTransactionIsolation = true;
    //39: A flag to set dirty on schema property to support to be reset under transaction,for example:PG driver
    private boolean forceDirtyWhenSetSchema;
    //40: A flag to set dirty on catalog property to support to be reset under transaction,for example:PG driver
    private boolean forceDirtyWhenSetCatalog;

    /**
     * connection factory class,which must be implement one of the below four interfaces
     * 1: {@code RawConnectionFactory}
     * 2: {@code RawXaConnectionFactory}
     * 3: {@code DataSource}
     * 4: {@code XADataSource}
     */
    //41: Connection factory,priority order: instance > class > class name
    private Object connectionFactory;
    //42: Class of Connection factory
    private Class<?> connectionFactoryClass;
    //43: Class name of Connection factory
    private String connectionFactoryClassName;

    //44: A {@code SQLException.vendorCode} list to check sql-exceptions thrown from connections, if code matched in list,then evicts connections from pool
    private List<Integer> sqlExceptionCodeList;
    //45: A {@code SQLException.SQLState} list to check sql-exceptions thrown from connections, if code matched in list,then evicts connections from pool
    private List<String> sqlExceptionStateList;
    //46: Connections predicate,priority order: instance > class > class name
    private BeeConnectionPredicate predicate;
    //47: Class of predicate
    private Class<? extends BeeConnectionPredicate> predicateClass;
    //48: Class name of predicate
    private String predicateClassName;

    //49: Jdbc info decoder(url,username password),default is none;priority order: instance > class > class name
    private BeeJdbcLinkInfoDecoder linkInfoDecoder;
    //50 Class of Jdbc info decoder(url,username password),default is none
    private Class<? extends BeeJdbcLinkInfoDecoder> linkInfoDecoderClass;
    //51: Class name of Jdbc info decoder(url,username password),default is none
    private String linkInfoDecoderClassName;

    //********************************************** method Execution logs ************************************************//
    //52: A flag to enable method execution log cache
    private boolean enableMethodExecutionLogCache;
    //53: Capacity of logs cache size,default is 1000
    private int methodExecutionLogCacheSize = 1000;
    //54: Logs timeout,default is 3 minutes
    private long methodExecutionLogTimeout = 180000L;
    //55: interval time to clear timeout logs,default is 3 minutes
    private long intervalOfClearTimeoutExecutionLogs = methodExecutionLogTimeout;

    //56: Slow threshold for connection acquisition,default is 30 seconds,time unit:milliseconds
    private long slowConnectionThreshold = 30000L;
    //57: Slow threshold for sql execution,default is 30 seconds,time unit:milliseconds
    private long slowSQLThreshold = 30000L;

    //58: method execution listener: instance > class > class name
    private BeeMethodExecutionListener methodExecutionListener;
    //59: Class of method execution listener,default is none
    private Class<? extends BeeMethodExecutionListener> methodExecutionListenerClass;
    //60: Class name of method execution listener,default is none
    private String methodExecutionListenerClassName;

    //61: method execution listener factory: instance > class > class name
    private BeeMethodExecutionListenerFactory methodExecutionListenerFactory;
    //62: Class of method execution listener factory ,default is none
    private Class<? extends BeeMethodExecutionListenerFactory> methodExecutionListenerFactoryClass;
    //63: Class name of method execution listener factory,default is none
    private String methodExecutionListenerFactoryClassName;

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
    //                                     2: JDBC link info(10)[1 --- 4]                                             //
    //****************************************************************************************************************//
    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getJdbcUrl() {
        return this.jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = trimString(jdbcUrl);
    }

    public String getUrl() {
        return this.jdbcUrl;
    }

    public void setUrl(String jdbcUrl) {
        this.jdbcUrl = trimString(jdbcUrl);
    }

    public String getDriverClassName() {
        return this.driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = trimString(driverClassName);
    }

    //****************************************************************************************************************//
    //                                     3: Pool control setting(42)[5 --- 24]                                      //
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
        if (initialSize < 0)
            throw new InvalidParameterException("The given value for the configuration item 'initial-size' cannot be less than zero");
        this.initialSize = initialSize;
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
        if (maxActive <= 0)
            throw new InvalidParameterException("The given value for configuration item 'max-active' must be greater than zero");
        this.maxActive = maxActive;
        //fix issue:#19 Chris-2020-08-16 begin
        this.semaphoreSize = maxActive > 1 ? Math.min(maxActive / 2, NCPU) : 1;
        //fix issue:#19 Chris-2020-08-16 end
    }

    public int getSemaphoreSize() {
        return this.semaphoreSize;
    }

    public void setSemaphoreSize(int semaphoreSize) {
        if (semaphoreSize <= 0)
            throw new InvalidParameterException("The given value for configuration item 'semaphore-size' must be greater than zero");
        this.semaphoreSize = semaphoreSize;
    }

    public boolean isUseThreadLocal() {
        return useThreadLocal;
    }

    public void setUseThreadLocal(boolean useThreadLocal) {
        this.useThreadLocal = useThreadLocal;
    }

    public long getMaxWait() {
        return this.maxWait;
    }

    public void setMaxWait(long maxWait) {
        if (maxWait <= 0L)
            throw new InvalidParameterException("The given value for configuration item 'max-wait' must be greater than zero");
        this.maxWait = maxWait;
    }

    public long getIdleTimeout() {
        return this.idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        if (idleTimeout <= 0L)
            throw new InvalidParameterException("The given value for configuration item 'idle-timeout' must be greater than zero");
        this.idleTimeout = idleTimeout;
    }

    public long getHoldTimeout() {
        return this.holdTimeout;
    }

    public void setHoldTimeout(long holdTimeout) {
        if (holdTimeout < 0L)
            throw new InvalidParameterException("The given value for configuration item 'hold-timeout' cannot be less than zero");
        this.holdTimeout = holdTimeout;
    }

    public long getIntervalOfClearTimeout() {
        return this.intervalOfClearTimeout;
    }

    public void setIntervalOfClearTimeout(long intervalOfClearTimeout) {
        if (intervalOfClearTimeout <= 0L)
            throw new InvalidParameterException("The given value for configuration item 'interval-of-clear-timeout' must be greater than zero");
        this.intervalOfClearTimeout = intervalOfClearTimeout;
    }

    public boolean isForceRecycleBorrowedOnClose() {
        return this.forceRecycleBorrowedOnClose;
    }

    public void setForceRecycleBorrowedOnClose(boolean forceRecycleBorrowedOnClose) {
        this.forceRecycleBorrowedOnClose = forceRecycleBorrowedOnClose;
    }

    public long getParkTimeForRetry() {
        return this.parkTimeForRetry;
    }

    public void setParkTimeForRetry(long parkTimeForRetry) {
        if (parkTimeForRetry < 0L)
            throw new InvalidParameterException("The given value for configuration item 'park-time-for-retry' cannot be less than zero");
        this.parkTimeForRetry = parkTimeForRetry;
    }

    public boolean isRegisterMbeans() {
        return this.registerMbeans;
    }

    public void setRegisterMbeans(boolean registerMbeans) {
        this.registerMbeans = registerMbeans;
    }

    public boolean isPrintRuntimeLogs() {
        return this.printRuntimeLogs;
    }

    public void setPrintRuntimeLogs(boolean printRuntimeLogs) {
        this.printRuntimeLogs = printRuntimeLogs;
    }

    public boolean isPrintConfiguration() {
        return this.printConfiguration;
    }

    public void setPrintConfiguration(boolean printConfiguration) {
        this.printConfiguration = printConfiguration;
    }

    public String getPoolImplementClassName() {
        return this.poolImplementClassName;
    }

    public void setPoolImplementClassName(String poolImplementClassName) {
        this.poolImplementClassName = trimString(poolImplementClassName);
    }

    public void clearExclusionListOfPrint() {
        this.exclusionListOfPrint.clear();
    }

    public void addExclusionNameOfPrint(String fieldName) {
        if (!exclusionListOfPrint.contains(fieldName)) this.exclusionListOfPrint.add(fieldName);
    }

    public boolean removeExclusionNameOfPrint(String fieldName) {
        return this.exclusionListOfPrint.remove(fieldName);
    }

    public boolean existExclusionNameOfPrint(String fieldName) {
        return this.exclusionListOfPrint.contains(fieldName);
    }

    public Object getConnectionFactoryProperty(String key) {
        return this.connectionFactoryProperties.get(key);
    }

    public Object removeConnectionFactoryProperty(String key) {
        return this.connectionFactoryProperties.remove(key);
    }

    public void addConnectionFactoryProperty(String key, Object value) {
        if (isBlank(key)) throw new InvalidParameterException("The given key cannot be null or blank");
        this.connectionFactoryProperties.put(key, value);
    }

    public void addConnectionFactoryProperty(String connectPropertyText) {
        if (isNotBlank(connectPropertyText)) {
            for (String attribute : connectPropertyText.split("&")) {
                String[] pair = attribute.split("=");
                if (pair.length == 2) {
                    this.addConnectionFactoryProperty(pair[0].trim(), pair[1].trim());
                } else {
                    pair = attribute.split(":");
                    if (pair.length == 2) {
                        this.addConnectionFactoryProperty(pair[0].trim(), pair[1].trim());
                    }
                }
            }
        }
    }

    //****************************************************************************************************************//
    //                                     4: Connection alive test(6)[25 --- 27]                                     //
    //****************************************************************************************************************//
    public String getAliveTestSql() {
        return this.aliveTestSql;
    }

    public void setAliveTestSql(String aliveTestSql) {
        if (isBlank(aliveTestSql))
            throw new InvalidParameterException("The given value for configuration item 'alive-test-sql' cannot be null or empty");

        aliveTestSql = trimString(aliveTestSql);
        if (!aliveTestSql.toUpperCase(Locale.US).startsWith("SELECT "))
            throw new InvalidParameterException("The given value for configuration item 'alive-test-sql' must start with 'select '");

        this.aliveTestSql = aliveTestSql;
    }

    public int getAliveTestTimeout() {
        return this.aliveTestTimeout;
    }

    public void setAliveTestTimeout(int aliveTestTimeout) {
        if (aliveTestTimeout < 0L)
            throw new InvalidParameterException("The given value for configuration item 'alive-test-timeout' cannot  be less than zero");
        this.aliveTestTimeout = aliveTestTimeout;
    }

    public long getAliveAssumeTime() {
        return this.aliveAssumeTime;
    }

    public void setAliveAssumeTime(long aliveAssumeTime) {
        if (aliveAssumeTime < 0L)
            throw new InvalidParameterException("The given value for configuration item 'alive-assume-time' cannot be less than zero");
        this.aliveAssumeTime = aliveAssumeTime;
    }

    //****************************************************************************************************************//
    //                                     5: connection default value Setting (26)[28 --- 40]                        //
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

    public Integer getDefaultTransactionIsolation() {
        return this.defaultTransactionIsolation;
    }

    public void setDefaultTransactionIsolation(Integer transactionIsolationCode) {
        this.defaultTransactionIsolation = transactionIsolationCode;//support Informix jdbc
    }

    public String getDefaultTransactionIsolationName() {
        return this.defaultTransactionIsolationName;
    }

    public void setDefaultTransactionIsolationName(String transactionIsolationName) {
        String transactionIsolationNameTemp = trimString(transactionIsolationName);
        if (isBlank(transactionIsolationNameTemp))
            throw new InvalidParameterException("The given value for configuration item 'default-transaction-isolation-name' cannot be null or empty");

        this.defaultTransactionIsolation = BeeTransactionIsolationNames.getTransactionIsolationCode(transactionIsolationNameTemp);
        if (this.defaultTransactionIsolation != null) {
            defaultTransactionIsolationName = transactionIsolationNameTemp;
        } else {
            throw new BeeDataSourceConfigException("Invalid transaction isolation name:" + transactionIsolationNameTemp + ", value is one of[" + TRANS_ISOLATION_CODE_LIST + "]");
        }
    }

    public boolean isUseDefaultCatalog() {
        return useDefaultCatalog;
    }

    public void setUseDefaultCatalog(boolean useDefaultCatalog) {
        this.useDefaultCatalog = useDefaultCatalog;
    }

    public boolean isUseDefaultSchema() {
        return useDefaultSchema;
    }

    public void setUseDefaultSchema(boolean useDefaultSchema) {
        this.useDefaultSchema = useDefaultSchema;
    }

    public boolean isUseDefaultReadOnly() {
        return useDefaultReadOnly;
    }

    public void setUseDefaultReadOnly(boolean useDefaultReadOnly) {
        this.useDefaultReadOnly = useDefaultReadOnly;
    }

    public boolean isUseDefaultAutoCommit() {
        return useDefaultAutoCommit;
    }

    public void setUseDefaultAutoCommit(boolean useDefaultAutoCommit) {
        this.useDefaultAutoCommit = useDefaultAutoCommit;
    }

    public boolean isUseDefaultTransactionIsolation() {
        return useDefaultTransactionIsolation;
    }

    public void setUseDefaultTransactionIsolation(boolean useDefaultTransactionIsolation) {
        this.useDefaultTransactionIsolation = useDefaultTransactionIsolation;
    }

    public boolean isForceDirtyWhenSetSchema() {
        return forceDirtyWhenSetSchema;
    }

    public void setForceDirtyWhenSetSchema(boolean forceDirtyWhenSetSchema) {
        this.forceDirtyWhenSetSchema = forceDirtyWhenSetSchema;
    }

    public boolean isForceDirtyWhenSetCatalog() {
        return forceDirtyWhenSetCatalog;
    }

    public void setForceDirtyWhenSetCatalog(boolean forceDirtyWhenSetCatalog) {
        this.forceDirtyWhenSetCatalog = forceDirtyWhenSetCatalog;
    }


    //****************************************************************************************************************//
    //                                     6: Connection factory Setting (7)[41 --- 43]                               //
    //****************************************************************************************************************//
    public Object getConnectionFactory() {
        return this.connectionFactory;
    }

    public void setConnectionFactory(BeeConnectionFactory factory) {
        this.connectionFactory = factory;
    }

    public void setXaConnectionFactory(BeeXaConnectionFactory factory) {
        this.connectionFactory = factory;
    }

    public Class<?> getConnectionFactoryClass() {
        return this.connectionFactoryClass;
    }

    public void setConnectionFactoryClass(Class<?> connectionFactoryClass) {
        this.connectionFactoryClass = connectionFactoryClass;
    }

    public String getConnectionFactoryClassName() {
        return this.connectionFactoryClassName;
    }

    public void setConnectionFactoryClassName(String connectionFactoryClassName) {
        this.connectionFactoryClassName = trimString(connectionFactoryClassName);
    }

    //****************************************************************************************************************//
    //                                     7: Connection predicate(12)[44 --- 48]                                     //
    //****************************************************************************************************************//
    public BeeConnectionPredicate getPredicate() {
        return predicate;
    }

    public void setPredicate(BeeConnectionPredicate predicate) {
        this.predicate = predicate;
    }

    public Class<? extends BeeConnectionPredicate> getPredicateClass() {
        return predicateClass;
    }

    public void setPredicateClass(Class<? extends BeeConnectionPredicate> predicateClass) {
        this.predicateClass = predicateClass;
    }

    public String getPredicateClassName() {
        return predicateClassName;
    }

    public void setPredicateClassName(String predicateClassName) {
        this.predicateClassName = predicateClassName;
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

    //****************************************************************************************************************//
    //                                    8: Link Info Decoder(6)[49 --- 51]                                          //
    //****************************************************************************************************************//
    public BeeJdbcLinkInfoDecoder getLinkInfoDecoder() {
        return linkInfoDecoder;
    }

    public void setLinkInfoDecoder(BeeJdbcLinkInfoDecoder linkInfoDecoder) {
        this.linkInfoDecoder = linkInfoDecoder;
    }

    public Class<? extends BeeJdbcLinkInfoDecoder> getLinkInfoDecoderClass() {
        return this.linkInfoDecoderClass;
    }

    public void setLinkInfoDecoderClass(Class<? extends BeeJdbcLinkInfoDecoder> linkInfoDecoderClass) {
        this.linkInfoDecoderClass = linkInfoDecoderClass;
    }

    public String getLinkInfoDecoderClassName() {
        return this.linkInfoDecoderClassName;
    }

    public void setLinkInfoDecoderClassName(String linkInfoDecoderClassName) {
        this.linkInfoDecoderClassName = linkInfoDecoderClassName;
    }

    //****************************************************************************************************************//
    //                                    9: Log Manager(24)[52 --- 63]                                               //
    //****************************************************************************************************************//
    public boolean isEnableMethodExecutionLogCache() {
        return enableMethodExecutionLogCache;
    }

    public void setEnableMethodExecutionLogCache(boolean enableMethodExecutionLogCache) {
        this.enableMethodExecutionLogCache = enableMethodExecutionLogCache;
    }

    public int getMethodExecutionLogCacheSize() {
        return methodExecutionLogCacheSize;
    }

    public void setMethodExecutionLogCacheSize(int methodExecutionLogCacheSize) {
        if (methodExecutionLogCacheSize <= 0)
            throw new InvalidParameterException("The given value for configuration item 'method-execution-log-cache-size' must be greater than zero");
        this.methodExecutionLogCacheSize = methodExecutionLogCacheSize;
    }

    public long getMethodExecutionLogTimeout() {
        return methodExecutionLogTimeout;
    }

    public void setMethodExecutionLogTimeout(long methodExecutionLogTimeout) {
        if (methodExecutionLogTimeout <= 0L)
            throw new InvalidParameterException("The given value for configuration item 'method-execution-log-timeout' must be greater than zero");
        this.methodExecutionLogTimeout = methodExecutionLogTimeout;
    }

    public long getIntervalOfClearTimeoutExecutionLogs() {
        return intervalOfClearTimeoutExecutionLogs;
    }

    public void setIntervalOfClearTimeoutExecutionLogs(long intervalOfClearTimeoutExecutionLogs) {
        if (intervalOfClearTimeoutExecutionLogs <= 0L)
            throw new InvalidParameterException("The given value for configuration item 'interval-of-clear-timeout-execution-logs' must be greater than zero");
        this.intervalOfClearTimeoutExecutionLogs = intervalOfClearTimeoutExecutionLogs;
    }

    public long getSlowConnectionThreshold() {
        return slowConnectionThreshold;
    }

    public void setSlowConnectionThreshold(long slowConnectionThreshold) {
        if (slowConnectionThreshold < 0L)
            throw new InvalidParameterException("The given value for configuration item 'slow-connection-threshold' must be greater than zero");
        this.slowConnectionThreshold = slowConnectionThreshold;
    }

    public long getSlowSQLThreshold() {
        return slowSQLThreshold;
    }

    public void setSlowSQLThreshold(long slowSQLThreshold) {
        if (slowSQLThreshold < 0L)
            throw new InvalidParameterException("The given value for configuration item 'slow-SQL-threshold' must be greater than zero");
        this.slowSQLThreshold = slowSQLThreshold;
    }

    public BeeMethodExecutionListener getMethodExecutionListener() {
        return methodExecutionListener;
    }

    public void setMethodExecutionListener(BeeMethodExecutionListener methodExecutionListener) {
        this.methodExecutionListener = methodExecutionListener;
    }

    public Class<? extends BeeMethodExecutionListener> getMethodExecutionListenerClass() {
        return methodExecutionListenerClass;
    }

    public void setMethodExecutionListenerClass(Class<? extends BeeMethodExecutionListener> methodExecutionListenerClass) {
        this.methodExecutionListenerClass = methodExecutionListenerClass;
    }

    public String getMethodExecutionListenerClassName() {
        return methodExecutionListenerClassName;
    }

    public void setMethodExecutionListenerClassName(String methodExecutionListenerClassName) {
        this.methodExecutionListenerClassName = methodExecutionListenerClassName;
    }


    public Class<? extends BeeMethodExecutionListenerFactory> getMethodExecutionListenerFactoryClass() {
        return methodExecutionListenerFactoryClass;
    }

    public void setMethodExecutionListenerFactoryClass(Class<? extends BeeMethodExecutionListenerFactory> methodExecutionListenerFactoryClass) {
        this.methodExecutionListenerFactoryClass = methodExecutionListenerFactoryClass;
    }

    public BeeMethodExecutionListenerFactory getMethodExecutionListenerFactory() {
        return methodExecutionListenerFactory;
    }

    public void setMethodExecutionListenerFactory(BeeMethodExecutionListenerFactory methodExecutionListenerFactory) {
        this.methodExecutionListenerFactory = methodExecutionListenerFactory;
    }

    public String getMethodExecutionListenerFactoryClassName() {
        return methodExecutionListenerFactoryClassName;
    }

    public void setMethodExecutionListenerFactoryClassName(String methodExecutionListenerFactoryClassName) {
        this.methodExecutionListenerFactoryClassName = methodExecutionListenerFactoryClassName;
    }

    //****************************************************************************************************************//
    //                                     10: properties configuration(3)                                             //
    //****************************************************************************************************************//
    public void loadFromPropertiesFile(String filename) {
        loadFromPropertiesFile(filename, null);
    }

    public void loadFromPropertiesFile(File file) {
        loadFromPropertiesFile(file, null);
    }

    public void loadFromProperties(Properties configProperties) {
        loadFromProperties(configProperties, null);
    }

    public void loadFromPropertiesFile(String filename, String keyPrefix) {
        if (isBlank(filename))
            throw new IllegalArgumentException("Configuration file name can't be null or empty");
        String fileLowerCaseName = filename.toLowerCase(Locale.US);
        if (!fileLowerCaseName.endsWith(".properties"))
            throw new IllegalArgumentException("Configuration file name file must be end with '.properties'");

        if (fileLowerCaseName.startsWith("cp:")) {//1:'cp:' prefix
            String cpFileName = fileLowerCaseName.substring("cp:".length());
            Properties fileProperties = loadPropertiesFromClassPathFile(cpFileName);
            loadFromProperties(fileProperties, keyPrefix);
        } else if (fileLowerCaseName.startsWith("classpath:")) {//2:'classpath:' prefix
            String cpFileName = fileLowerCaseName.substring("classpath:".length());
            Properties fileProperties = loadPropertiesFromClassPathFile(cpFileName);
            loadFromProperties(fileProperties, keyPrefix);
        } else {//load a real path
            File file = new File(filename);
            if (!file.exists()) throw new IllegalArgumentException("Not found configuration file:" + filename);
            if (!file.isFile())
                throw new IllegalArgumentException("Target object is a valid configuration file:" + filename);
            loadFromPropertiesFile(file, keyPrefix);
        }
    }

    public void loadFromPropertiesFile(File file, String keyPrefix) {
        if (file == null) throw new IllegalArgumentException("Configuration properties file can't be null");
        if (!file.exists()) throw new IllegalArgumentException("Configuration properties file not found:" + file);
        if (!file.isFile()) throw new IllegalArgumentException("Target object is not a valid file");
        if (!file.getAbsolutePath().toLowerCase(Locale.US).endsWith(".properties"))
            throw new IllegalArgumentException("Target file is not a properties file");

        try (InputStream stream = Files.newInputStream(file.toPath())) {
            Properties configProperties = new Properties();
            configProperties.load(stream);

            this.loadFromProperties(configProperties, keyPrefix);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load configuration file:" + file, e);
        }
    }

    public void loadFromProperties(Properties configProperties, String keyPrefix) {
        if (configProperties == null || configProperties.isEmpty())
            throw new IllegalArgumentException("Configuration properties must not be null or empty");

        //1:load configuration item values from outside properties
        HashMap<String, String> setValueMap;
        if (isNotBlank(keyPrefix)) {
            if (keyPrefix.charAt(keyPrefix.length() - 1) != '.') keyPrefix = keyPrefix + ".";
            final int keyPrefixLen = keyPrefix.length();
            setValueMap = new HashMap<>(configProperties.size());
            for (Map.Entry<Object, Object> entry : configProperties.entrySet()) {
                String key = (String) entry.getKey();
                if (key.startsWith(keyPrefix)) {
                    setValueMap.put(key.substring(keyPrefixLen), (String) entry.getValue());
                }
            }
        } else {
            setValueMap = new HashMap(configProperties);
        }

        //2: exclude some special keys in setValueMap
        String connectPropertiesText = setValueMap.remove(CONFIG_FACTORY_PROP);//remove item if exists in properties file before injection
        String connectPropertiesSize = setValueMap.remove(CONFIG_FACTORY_PROP_SIZE);//remove item if exists in properties file before injection
        String sqlExceptionCode = setValueMap.remove(CONFIG_SQL_EXCEPTION_CODE);//remove item if exists in properties file before injection
        String sqlExceptionState = setValueMap.remove(CONFIG_SQL_EXCEPTION_STATE);//remove item if exists in properties file before injection
        String exclusionListText = setValueMap.remove(CONFIG_EXCLUSION_LIST_OF_PRINT);

        try {
            setPropertiesValue(this, setValueMap);
        } catch (BeanException e) {
            throw new BeeDataSourceConfigException(e.getMessage(), e);
        }

        //3:try to find 'connectProperties' config value and put to ds config object
        this.addConnectionFactoryProperty(connectPropertiesText);
        if (isNotBlank(connectPropertiesSize)) {
            int size = Integer.parseInt(connectPropertiesSize.trim());
            for (int i = 1; i <= size; i++)//properties index begin with 1
                this.addConnectionFactoryProperty(getPropertyValue(setValueMap, CONFIG_FACTORY_PROP_KEY_PREFIX + i));
        }

        //4: add error codes if not null and not empty
        if (isNotBlank(sqlExceptionCode)) {
            for (String code : sqlExceptionCode.trim().split(",")) {
                try {
                    this.addSqlExceptionCode(Integer.parseInt(code));
                } catch (NumberFormatException e) {
                    throw new BeeDataSourceConfigException(code + " is not a valid SQLException error code");
                }
            }
        }

        //5: add sql states if not null and not empty
        if (isNotBlank(sqlExceptionState)) {
            for (String state : sqlExceptionState.trim().split(",")) {
                this.addSqlExceptionState(state);
            }
        }

        //6:try to load exclusion list on config print
        if (isNotBlank(exclusionListText)) {
            this.clearExclusionListOfPrint();//remove existed exclusion
            for (String exclusion : exclusionListText.trim().split(",")) {
                this.addExclusionNameOfPrint(exclusion);
            }
        }
    }

    //****************************************************************************************************************//
    //                                   11: configuration check and connection factory create methods(8)             //
    //****************************************************************************************************************//

    /**
     * Check on this configuration,return its copy if success
     *
     * @return a copy of current configuration
     * @throws BeeDataSourceConfigException when check configuration failed
     * @throws SQLException                 when failed to load a driver with a configured class name or other check on a driver
     */
    public BeeDataSourceConfig check() throws SQLException {
        if (initialSize > maxActive)
            throw new BeeDataSourceConfigException("The configured value of item 'initial-size' cannot be greater than the configured value of item 'max-active'");

        Object connectionFactory = createConnectionFactory();
        BeeConnectionPredicate predicate = this.createConnectionEvictPredicate();
        BeeMethodExecutionListener methodExecutionListener = createMethodExecutionListener();

        BeeDataSourceConfig checkedConfig = new BeeDataSourceConfig();
        copyTo(checkedConfig);
        if (this.connectionFactory != null || connectionFactoryClass != null || isNotBlank(connectionFactoryClassName)) {
            if (isNotBlank(this.username) || isNotBlank(this.password) || isNotBlank(this.jdbcUrl) || isNotBlank(driverClassName)) {
                CommonLogPrinter.info("BeeCP({})configured jdbc link info abandoned according that a connection factory has been existed", "...");
                checkedConfig.username = null;
                checkedConfig.password = null;
                checkedConfig.jdbcUrl = null;
                checkedConfig.driverClassName = null;
            }
        }

        //set some factories to config
        this.connectionFactory = connectionFactory;
        checkedConfig.connectionFactory = connectionFactory;
        checkedConfig.predicate = predicate;
        checkedConfig.methodExecutionListener = methodExecutionListener;
        if (isBlank(checkedConfig.poolName)) checkedConfig.poolName = "FastPool-" + PoolNameIndex.getAndIncrement();
        if (checkedConfig.printConfiguration) printConfiguration(checkedConfig);

        return checkedConfig;
    }

    //copy configuration info to other from local
    void copyTo(BeeDataSourceConfig config) {
        String fieldName = null;
        try {
            for (Field field : BeeDataSourceConfig.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;

                fieldName = field.getName();
                switch (fieldName) {
                    case CONFIG_FACTORY_PROP: //copy 'connectProperties'
                        config.connectionFactoryProperties.putAll(connectionFactoryProperties);
                        break;
                    case CONFIG_EXCLUSION_LIST_OF_PRINT: //copy 'exclusionListOfPrint'
                        if (exclusionListOfPrint.isEmpty())
                            config.exclusionListOfPrint.clear();
                        else
                            config.exclusionListOfPrint.addAll(exclusionListOfPrint);
                        break;
                    case CONFIG_SQL_EXCEPTION_CODE: //copy 'sqlExceptionCodeList'
                        if (sqlExceptionCodeList == null)
                            config.sqlExceptionCodeList = null;
                        else if (!sqlExceptionCodeList.isEmpty()) {
                            if (config.sqlExceptionCodeList == null) {
                                config.sqlExceptionCodeList = new ArrayList<>(sqlExceptionCodeList);
                            } else {
                                config.sqlExceptionCodeList.clear();
                                config.sqlExceptionCodeList.addAll(sqlExceptionCodeList);
                            }
                        }
                        break;
                    case CONFIG_SQL_EXCEPTION_STATE: //copy 'sqlExceptionStateList'
                        if (sqlExceptionStateList == null)
                            config.sqlExceptionStateList = null;
                        else if (!sqlExceptionStateList.isEmpty()) {
                            if (config.sqlExceptionStateList == null) {
                                config.sqlExceptionStateList = new ArrayList<>(sqlExceptionStateList);
                            } else {
                                config.sqlExceptionStateList.clear();
                                config.sqlExceptionStateList.addAll(sqlExceptionStateList);
                            }
                        }
                        break;
                    default: //other config items
                        field.set(config, field.get(this));
                }
            }
        } catch (Exception e) {
            throw new BeeDataSourceConfigException("Failed to copy field[" + fieldName + "]", e);
        }
    }

    //create BeeJdbcLinkInfoDecoder instance
    private BeeJdbcLinkInfoDecoder createJdbcLinkInfoDecoder() {
        //step1:if exists link info decoder,then return it
        if (linkInfoDecoder != null) return this.linkInfoDecoder;

        //step2: create link info decoder
        if (linkInfoDecoderClass != null || isNotBlank(linkInfoDecoderClassName)) {
            Class<?> decoderClass = null;
            try {
                decoderClass = linkInfoDecoderClass != null ? linkInfoDecoderClass : loadClass(linkInfoDecoderClassName);
                return (BeeJdbcLinkInfoDecoder) createClassInstance(decoderClass, BeeJdbcLinkInfoDecoder.class, "jdbc link info decoder");
            } catch (ClassNotFoundException e) {
                throw new BeeDataSourceConfigException("Failed to create jdbc link info decoder with class[" + linkInfoDecoderClassName + "]", e);
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to create jdbc link info decoder with class[" + decoderClass + "]", e);
            }
        }
        return null;
    }

    //create method log handler
    private BeeMethodExecutionListener createMethodExecutionListener() {
        //step1:if exists listener,then return it
        if (this.methodExecutionListener != null) return this.methodExecutionListener;

        //step2:if exists listener factory,then use it to create one
        if (this.methodExecutionListenerFactory != null) {
            try {
                return methodExecutionListenerFactory.create();
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to create method execution listener by listener factory", e);
            }
        }

        //step3: create listener factory and let it create a listener
        if (this.methodExecutionListenerFactoryClass != null || isNotBlank(this.methodExecutionListenerFactoryClassName)) {
            Class<?> listenerFactoryClass = null;
            BeeMethodExecutionListenerFactory factory;
            try {
                listenerFactoryClass = methodExecutionListenerFactoryClass != null ? methodExecutionListenerFactoryClass : loadClass(methodExecutionListenerFactoryClassName);
                factory = ((BeeMethodExecutionListenerFactory) createClassInstance(listenerFactoryClass, BeeMethodExecutionListenerFactory.class, "method execution listener factory"));
            } catch (ClassNotFoundException e) {
                throw new BeeDataSourceConfigException("Failed to create method execution listener factory with class[" + methodExecutionListenerFactoryClassName + "]", e);
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to create method execution listener factory with class[" + listenerFactoryClass + "]", e);
            }

            try {
                return factory.create();
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to create method execution listener by listener factory", e);
            }
        }

        //step4: create a listener
        if (this.methodExecutionListenerClass != null || isNotBlank(this.methodExecutionListenerClassName)) {
            Class<?> listenerClass = null;
            try {
                listenerClass = methodExecutionListenerClass != null ? methodExecutionListenerClass : loadClass(methodExecutionListenerClassName);
                return (BeeMethodExecutionListener) createClassInstance(listenerClass, BeeMethodExecutionListener.class, "method execution listener");
            } catch (ClassNotFoundException e) {
                throw new BeeDataSourceConfigException("Failed to create method execution listener with class[" + methodExecutionListenerClassName + "]", e);
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to create method execution listener with class[" + listenerClass + "]", e);
            }
        }
        return null;
    }

    //create Connection factory
    private Object createConnectionFactory() throws SQLException {
        //step1:create jdbc info Decoder
        Properties jdbcLinkInfoProperties = getJdbcLinkInfoProperties();
        BeeJdbcLinkInfoDecoder jdbcLinkInfoDecoder = this.createJdbcLinkInfoDecoder();
        if (this.connectionFactory == null && this.connectionFactoryClass == null && isBlank(this.connectionFactoryClassName)) {
            //step2.1: prepare jdbc url
            String url = jdbcLinkInfoProperties.getProperty("url");
            if (isBlank(url)) throw new BeeDataSourceConfigException("jdbcUrl must not be null or blank");
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
            localConnectProperties.putAll(this.connectionFactoryProperties);

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
                Object factory = this.connectionFactory;
                if (factory == null) {
                    //3.1: load connection factory class with class name
                    conFactClass = this.connectionFactoryClass != null ? this.connectionFactoryClass : loadClass(this.connectionFactoryClassName);

                    //3.2: check connection factory class
                    Class<?>[] parentClasses = {BeeConnectionFactory.class, BeeXaConnectionFactory.class, DataSource.class, XADataSource.class};

                    //3.3: create connection factory instance
                    factory = createClassInstance(conFactClass, parentClasses, "connection factory");
                }

                //3.4: create a copy on local connectProperties
                Map<String, Object> localConnectProperties = new HashMap<>(this.connectionFactoryProperties);//copy

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
                    localConnectProperties.put("username", username);
                    if (isNotBlank(password))
                        localConnectProperties.put("password", password);
                }

                //3.9: inject properties to connection factory or dataSource
                setPropertiesValue(factory, localConnectProperties);

                //3.10: return RawConnectionFactory or RawXaConnectionFactory
                if (factory instanceof BeeConnectionFactory || factory instanceof BeeXaConnectionFactory) {
                    return factory;
                } else if (factory instanceof XADataSource) {
                    return new XaConnectionFactoryByDriverDs((XADataSource) factory, username, password);
                } else {//here,factory must be a datasource(only support 4 types,because that factory class type check before creation)
                    return new ConnectionFactoryByDriverDs((DataSource) factory, username, password);
                }
            } catch (ClassNotFoundException e) {
                throw new BeeDataSourceConfigException("Not found connection factory class[" + conFactClass + "]", e);
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
            url = (String) this.connectionFactoryProperties.get("url");
            if (isBlank(url)) url = (String) connectionFactoryProperties.get("URL");
            if (isBlank(url)) url = (String) connectionFactoryProperties.get("jdbcUrl");
            if (isNotBlank(url)) {//url found from connectProperties
                username = (String) connectionFactoryProperties.get("user");
                password = (String) connectionFactoryProperties.get("password");
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
        if (this.predicate != null) return this.predicate;

        //step2: create SQLExceptionPredication
        if (predicateClass != null || isNotBlank(predicateClassName)) {
            Class<?> predicationClass = null;
            try {
                predicationClass = predicateClass != null ? predicateClass : loadClass(predicateClassName);
                return (BeeConnectionPredicate) createClassInstance(predicationClass, BeeConnectionPredicate.class, "sql exception predicate");
            } catch (ClassNotFoundException e) {
                throw new BeeDataSourceConfigException("Not found sql exception predicate class[" + predicateClassName + "]", e);
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to create sql exception predicate with class[" + predicationClass + "]", e);
            }
        }

        return null;
    }

    //print check passed configuration
    private void printConfiguration(BeeDataSourceConfig checkedConfig) {
        String poolName = checkedConfig.poolName;
        CommonLogPrinter.info("................................................BeeCP({})configuration[start]................................................", poolName);
        try {
            for (Field field : BeeDataSourceConfig.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;

                String fieldName = field.getName();
                boolean infoPrint = !checkedConfig.exclusionListOfPrint.contains(fieldName);
                switch (fieldName) {
                    case CONFIG_EXCLUSION_LIST_OF_PRINT: //copy 'exclusionConfigPrintList'
                        break;
                    case CONFIG_FACTORY_PROP: //copy 'connectionFactoryProperties'
                        if (!connectionFactoryProperties.isEmpty()) {
                            if (infoPrint) {
                                for (Map.Entry<String, Object> entry : checkedConfig.connectionFactoryProperties.entrySet())
                                    CommonLogPrinter.info("BeeCP({}).connectionFactoryProperties.{}={}", poolName, entry.getKey(), entry.getValue());
                            } else {
                                for (Map.Entry<String, Object> entry : checkedConfig.connectionFactoryProperties.entrySet())
                                    CommonLogPrinter.debug("BeeCP({}).connectionFactoryProperties.{}={}", poolName, entry.getKey(), entry.getValue());
                            }
                        }
                        break;
                    default:
                        if (infoPrint)
                            CommonLogPrinter.info("BeeCP({}).{}={}", poolName, fieldName, field.get(checkedConfig));
                        else
                            CommonLogPrinter.debug("BeeCP({}).{}={}", poolName, fieldName, field.get(checkedConfig));
                }
            }
        } catch (Throwable e) {
            CommonLogPrinter.warn("BeeCP({})failed to print configuration", poolName, e);
        }
        CommonLogPrinter.info("................................................BeeCP({})configuration[end]................................................", poolName);
    }
}