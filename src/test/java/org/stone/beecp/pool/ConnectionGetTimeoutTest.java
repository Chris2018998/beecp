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

public class ConnectionGetTimeoutTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        config.setMaxWait(1000);
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
            GetMockThread mockThread = new GetMockThread();
            mockThread.start();

            mockThread.join();
            if (mockThread.e == null)
                TestUtil.assertError("Connect timeout test failed");
            else
                System.out.println(mockThread.e);
        } finally {
            if (con != null)
                TestUtil.oclose(con);
        }
    }

    class GetMockThread extends Thread {
        SQLException e = null;

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
        }
    }
}
