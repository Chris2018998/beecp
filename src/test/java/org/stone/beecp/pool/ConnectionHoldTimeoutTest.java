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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class ConnectionHoldTimeoutTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        config.setInitialSize(0);
        config.setValidTestSql("SELECT 1 from dual");

        config.setHoldTimeout(500L);// hold and not using connection;
        config.setTimerCheckInterval(1000L);// two seconds interval
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws Exception {
        Connection con = null;
        try {
            FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
            //CountDownLatch poolThreadLatch = (CountDownLatch) TestUtil.getFieldValue(pool, "poolThreadLatch");
            //if (poolThreadLatch.getCount() > 0) poolThreadLatch.await();

            con = ds.getConnection();
            if (pool.getTotalSize() != 1)
                TestUtil.assertError("Total connections not as expected 1");
            if (pool.getUsingSize() != 1)
                TestUtil.assertError("Using connections not as expected 1");

            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
            if (pool.getUsingSize() != 0)
                TestUtil.assertError("Using connections not as expected 0 after hold timeout");

            try {
                con.getCatalog();
                TestUtil.assertError("must throw closed exception");
            } catch (SQLException e) {
                System.out.println(e);
            }

            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
        } finally {
            if (con != null)
                TestUtil.oclose(con);
        }
    }
}
