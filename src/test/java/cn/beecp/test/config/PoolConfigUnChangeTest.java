/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test.config;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import cn.beecp.pool.FastConnectionPool;
import cn.beecp.test.JdbcConfig;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;

public class PoolConfigUnChangeTest extends TestCase {
    BeeDataSourceConfig testConfig;
    private BeeDataSource ds;
    private int initSize = 5;
    private int maxSize = 20;

    public void setUp() throws Throwable {
        testConfig = new BeeDataSourceConfig();
        String url = JdbcConfig.JDBC_URL;
        testConfig.setJdbcUrl(url);
        testConfig.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        testConfig.setUsername(JdbcConfig.JDBC_USER);
        testConfig.setPassword(JdbcConfig.JDBC_PASSWORD);
        testConfig.setInitialSize(initSize);
        testConfig.setMaxActive(maxSize);
        testConfig.setValidTestSql("SELECT 1 from dual");
        testConfig.setIdleTimeout(3000);
        ds = new BeeDataSource(testConfig);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws InterruptedException, Exception {
        testConfig.setInitialSize(10);
        testConfig.setMaxActive(50);

        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        BeeDataSourceConfig tempConfig = (BeeDataSourceConfig) TestUtil.getFieldValue(pool, "poolConfig");
        if (tempConfig.getInitialSize() != initSize) TestUtil.assertError("initSize has changed,expected:" + initSize);
        if (tempConfig.getMaxActive() != maxSize) TestUtil.assertError("maxActive has changed,expected" + maxSize);
    }
}