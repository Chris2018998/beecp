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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class ConnectionIdleTimeoutTest extends TestCase {
    private final int initSize = 5;
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        config.setInitialSize(initSize);
        config.setMaxActive(initSize);
        config.setValidTestSql("SELECT 1 from dual");
        config.setIdleTimeout(1000);
        config.setTimerCheckInterval(1000);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws Exception {
        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        //CountDownLatch poolThreadLatch = (CountDownLatch) TestUtil.getFieldValue(pool, "poolThreadLatch");
        //if (poolThreadLatch.getCount() > 0) poolThreadLatch.await();

        if (pool.getTotalSize() != initSize) TestUtil.assertError("Total connections not as expected:" + initSize);
        if (pool.getIdleSize() != initSize) TestUtil.assertError("Idle connections not as expected:" + initSize);

        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));

        if (pool.getTotalSize() != 0) TestUtil.assertError("Total connections not as expected:" + 0);
        if (pool.getIdleSize() != 0) TestUtil.assertError("Idle connections not a sexpected:" + 0);
    }
}
