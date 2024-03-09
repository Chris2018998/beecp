/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.config;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;
import org.stone.beecp.pool.FastConnectionPool;

public class PoolConfigUnChangeTest extends TestCase {
    private final int initSize = 5;
    private final int maxSize = 20;
    BeeDataSourceConfig testConfig;
    private BeeDataSource ds;

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

    public void test() throws Exception {
        testConfig.setInitialSize(10);
        testConfig.setMaxActive(50);

        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        BeeDataSourceConfig tempConfig = (BeeDataSourceConfig) TestUtil.getFieldValue(pool, "poolConfig");
        if (tempConfig.getInitialSize() != initSize) TestUtil.assertError("initSize has changed,expected:" + initSize);
        if (tempConfig.getMaxActive() != maxSize) TestUtil.assertError("maxActive has changed,expected" + maxSize);
    }
}
