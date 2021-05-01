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
package cn.beecp.test.config;

import cn.beecp.BeeDataSourceConfig;
import cn.beecp.BeeDataSourceConfigException;
import cn.beecp.test.TestCase;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Properties;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class PropertiesFileLoadTest extends TestCase {
    public void test() throws Exception {
        String filename = "PropertiesFileLoadTest.properties";
        URL url = PropertiesFileLoadTest.class.getClassLoader().getResource(filename);
        if (url == null) url = PropertiesFileLoadTest.class.getResource(filename);

        BeeDataSourceConfig testConfig = new BeeDataSourceConfig();
        testConfig.loadFromPropertiesFile(url.getFile());
        if (!"root".equals(testConfig.getUsername())) throw new BeeDataSourceConfigException("username error");
        if (!"root".equals(testConfig.getPassword())) throw new BeeDataSourceConfigException("password error");
        if (!"jdbc:mysql://localhost/test".equals(testConfig.getJdbcUrl()))
            throw new BeeDataSourceConfigException("jdbcUrl error");
        if (!"com.mysql.cj.jdbc.Driver".equals(testConfig.getDriverClassName()))
            throw new BeeDataSourceConfigException("driverClassName error");
        if (!"test1".equals(testConfig.getDefaultCatalog()))
            throw new BeeDataSourceConfigException("defaultCatalog error");
        if (!"test2".equals(testConfig.getDefaultSchema()))
            throw new BeeDataSourceConfigException("defaultSchema error");
        if (!testConfig.isDefaultReadOnly()) throw new BeeDataSourceConfigException("defaultReadOnly error");
        if (!testConfig.isDefaultAutoCommit()) throw new BeeDataSourceConfigException("defaultAutoCommit error");
        if (testConfig.getDefaultTransactionIsolationCode() != 1)
            throw new BeeDataSourceConfigException("defaultTransactionIsolationCode error");
        if (!"TRANSACTION_READ_UNCOMMITTED".equals(testConfig.getDefaultTransactionIsolation()))
            throw new BeeDataSourceConfigException("defaultTransactionIsolation error");
        if (!"SELECT 1".equals(testConfig.getConnectionTestSql()))
            throw new BeeDataSourceConfigException("connectionTestSQL error");
        if (!"Pool1".equals(testConfig.getPoolName())) throw new BeeDataSourceConfigException("poolName error");
        if (!testConfig.isFairMode()) throw new BeeDataSourceConfigException("fairMode error");
        if (testConfig.getInitialSize() != 1) throw new BeeDataSourceConfigException("initialSize error");
        if (testConfig.getMaxActive() != 10) throw new BeeDataSourceConfigException("maxActive error");
        if (testConfig.getBorrowSemaphoreSize() != 4)
            throw new BeeDataSourceConfigException("borrowSemaphoreSize error");
        if (testConfig.getMaxWait() != 8000) throw new BeeDataSourceConfigException("maxWait error");
        if (testConfig.getIdleTimeout() != 18000) throw new BeeDataSourceConfigException("idleTimeout error");
        if (testConfig.getHoldTimeout() != 30000) throw new BeeDataSourceConfigException("holdTimeout error");
        if (testConfig.getConnectionTestTimeout() != 3)
            throw new BeeDataSourceConfigException("connectionTestTimeout error");
        if (testConfig.getConnectionTestInterval() != 500)
            throw new BeeDataSourceConfigException("connectionTestInterval error");
        if (testConfig.getIdleCheckTimeInterval() != 30000)
            throw new BeeDataSourceConfigException("idleCheckTimeInterval error");
        if (!testConfig.isForceCloseUsingOnClear())
            throw new BeeDataSourceConfigException("forceCloseUsingOnClear error");
        if (testConfig.getDelayTimeForNextClear() != 3000)
            throw new BeeDataSourceConfigException("delayTimeForNextClear error");
        if (!"cn.beecp.pool.DriverConnectionFactory".equals(testConfig.getConnectionFactoryClassName()))
            throw new BeeDataSourceConfigException("connectionFactoryClassName error");
        if (!"cn.beecp.xa.Mysql8XaConnectionFactory".equals(testConfig.getXaConnectionFactory().getClass().getName()))
            throw new BeeDataSourceConfigException("xaConnectionFactory error");
        if (!"cn.beecp.xa.Mysql8XaConnectionFactory".equals(testConfig.getXaConnectionFactoryClassName()))
            throw new BeeDataSourceConfigException("xaConnectionFactoryClassName error");
        if (!"cn.beecp.pool.RawConnectionPool".equals(testConfig.getPoolImplementClassName()))
            throw new BeeDataSourceConfigException("poolImplementClassName error");
        if (!testConfig.isEnableJmx()) throw new BeeDataSourceConfigException("enableJmx error");

        Field connectPropertiesField = BeeDataSourceConfig.class.getDeclaredField("connectProperties");
        connectPropertiesField.setAccessible(true);
        Properties connectProperties = (Properties) connectPropertiesField.get(testConfig);
        if (!"true".equals(connectProperties.getProperty("cachePrepStmts")))
            throw new BeeDataSourceConfigException("connectProperties error");
        if (!"50".equals(connectProperties.getProperty("prepStmtCacheSize")))
            throw new BeeDataSourceConfigException("connectProperties error");
    }
}
