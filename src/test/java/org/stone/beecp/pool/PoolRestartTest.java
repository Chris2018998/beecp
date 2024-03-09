/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;

public class PoolRestartTest extends TestCase {
    private final int initSize = 5;
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        config.setInitialSize(initSize);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws Exception {
        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        TestUtil.assertError("Total connections expected:%s,current is:%s", initSize, pool.getTotalSize());
        TestUtil.assertError("connections expected:%s,current is:%s", initSize, pool.getIdleSize());

        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setJdbcUrl(JdbcConfig.JDBC_URL);
        config2.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config2.setUsername(JdbcConfig.JDBC_USER);
        config2.setPassword(JdbcConfig.JDBC_PASSWORD);
        config2.setInitialSize(10);
        pool.clear(true, config2);
        TestUtil.assertError("Total connections not as expected:%s,but current is:%s", 10, pool.getTotalSize());
        TestUtil.assertError("Idle connections not as expected:%s,but current is:%s", 10, pool.getIdleSize());


    }
}
