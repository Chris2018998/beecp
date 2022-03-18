/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test.config;

import cn.beecp.BeeDataSourceConfig;
import cn.beecp.BeeDataSourceConfigException;
import cn.beecp.test.TestCase;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class PropertiesFileLoadTest extends TestCase {
    //private final String ConfigedUrl="jdbc:mysql://localhost/test";
    //private final String ConfigedDriver="com.mysql.cj.jdbc.Driver";
    private final String ConfigUrl = "jdbc:beecp://localhost/testdb";
    private final String ConfigDriver = "cn.beecp.test.mock.MockDriver";

    public void test() throws Exception {
        String filename = "beecp/config2.properties";
        URL url = PropertiesFileLoadTest.class.getClassLoader().getResource(filename);
        if (url == null) url = PropertiesFileLoadTest.class.getResource(filename);

        BeeDataSourceConfig testConfig = new BeeDataSourceConfig();
        testConfig.loadFromPropertiesFile(url.getFile());

        if (!"root".equals(testConfig.getUsername())) throw new BeeDataSourceConfigException("username error");
        if (!"root".equals(testConfig.getPassword())) throw new BeeDataSourceConfigException("password error");
        if (!ConfigUrl.equals(testConfig.getJdbcUrl()))
            throw new BeeDataSourceConfigException("jdbcUrl error");
        if (!ConfigDriver.equals(testConfig.getDriverClassName()))
            throw new BeeDataSourceConfigException("driverClassName error");
        if (!"test1".equals(testConfig.getDefaultCatalog()))
            throw new BeeDataSourceConfigException("defaultCatalog error");
        if (!"test2".equals(testConfig.getDefaultSchema()))
            throw new BeeDataSourceConfigException("defaultSchema error");
        if (!testConfig.isDefaultReadOnly()) throw new BeeDataSourceConfigException("defaultReadOnly error");
        if (!testConfig.isDefaultAutoCommit()) throw new BeeDataSourceConfigException("defaultAutoCommit error");
        if (testConfig.getDefaultTransactionIsolationCode() != 1)
            throw new BeeDataSourceConfigException("defaultTransactionIsolationCode error");
        if (!"READ_UNCOMMITTED".equals(testConfig.getDefaultTransactionIsolationName()))
            throw new BeeDataSourceConfigException("defaultTransactionIsolation error");
        if (!"SELECT 1".equals(testConfig.getValidTestSql()))
            throw new BeeDataSourceConfigException("connectionTestSQL error");
        if (!"Pool1".equals(testConfig.getPoolName())) throw new BeeDataSourceConfigException("poolName error");
        if (!testConfig.isFairMode()) throw new BeeDataSourceConfigException("fairMode error");
        if (testConfig.getInitialSize() != 1) throw new BeeDataSourceConfigException("initialSize error");
        if (testConfig.getMaxActive() != 10) throw new BeeDataSourceConfigException("maxActive error");
//        if (testConfig.getBorrowSemaphoreSize() != 4)throw new BeeDataSourceConfigException("borrowSemaphoreSize error");
        if (testConfig.getMaxWait() != 8000) throw new BeeDataSourceConfigException("maxWait error");
        if (testConfig.getIdleTimeout() != 18000) throw new BeeDataSourceConfigException("idleTimeout error");
        if (testConfig.getHoldTimeout() != 30000) throw new BeeDataSourceConfigException("holdTimeout error");
        if (testConfig.getValidTestTimeout() != 3)
            throw new BeeDataSourceConfigException("connectionTestTimeout error");
        if (testConfig.getValidAssumeTime() != 500)
            throw new BeeDataSourceConfigException("connectionTestInterval error");
        if (testConfig.getTimerCheckInterval() != 30000)
            throw new BeeDataSourceConfigException("idleCheckTimeInterval error");
        if (!testConfig.isForceCloseUsingOnClear())
            throw new BeeDataSourceConfigException("forceCloseUsingOnClear error");
        if (testConfig.getDelayTimeForNextClear() != 3000)
            throw new BeeDataSourceConfigException("delayTimeForNextClear error");
        if (!"cn.beecp.pool.ConnectionFactoryByDriver".equals(testConfig.getConnectionFactoryClassName()))
            throw new BeeDataSourceConfigException("connectionFactoryClassName error");
        if (!"cn.beecp.pool.RawConnectionPool".equals(testConfig.getPoolImplementClassName()))
            throw new BeeDataSourceConfigException("poolImplementClassName error");
        if (!"cn.beecp.pool.RawConnectionPool".equals(testConfig.getPoolImplementClassName()))
            throw new BeeDataSourceConfigException("poolImplementClassName error");
        if (!testConfig.isEnableJmx()) throw new BeeDataSourceConfigException("enableJmx error");

        Field connectPropertiesField = BeeDataSourceConfig.class.getDeclaredField("connectProperties");
        connectPropertiesField.setAccessible(true);
        Map<String, Object> connectProperties = (Map) connectPropertiesField.get(testConfig);
        if (!"true".equals(connectProperties.get("cachePrepStmts")))
            throw new BeeDataSourceConfigException("connectProperties error");
        if (!"50".equals(connectProperties.get("prepStmtCacheSize")))
            throw new BeeDataSourceConfigException("connectProperties error");
        if (!"2048".equals(connectProperties.get("prepStmtCacheSqlLimit")))
            throw new BeeDataSourceConfigException("connectProperties error");
        if (!"true".equals(connectProperties.get("useServerPrepStmts")))
            throw new BeeDataSourceConfigException("connectProperties error");
    }
}
