/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test.pool;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import cn.beecp.test.JdbcConfig;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;

public class ConnectionGetTimeoutTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        config.setMaxWait(3000);
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(1);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws Exception {
        Connection con = null;
        try {
            con = ds.getConnection();
            CountDownLatch lacth = new CountDownLatch(1);
            TestThread testTh = new TestThread(lacth);
            testTh.start();

            lacth.await();
            if (testTh.e == null)
                TestUtil.assertError("Connect timeout test failed");
            else
                System.out.println(testTh.e);
        } finally {
            if (con != null)
                TestUtil.oclose(con);
        }
    }

    class TestThread extends Thread {
        SQLException e = null;
        CountDownLatch lacth;

        TestThread(CountDownLatch lacth) {
            this.lacth = lacth;
        }

        public void run() {
            Connection con2 = null;
            try {
                con2 = ds.getConnection();
            } catch (SQLException e) {
                this.e = e;
            } finally {
                if (con2 != null)
                    TestUtil.oclose(con2);
            }
            lacth.countDown();
        }
    }
}
