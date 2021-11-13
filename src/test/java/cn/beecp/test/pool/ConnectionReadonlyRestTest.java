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

public class ConnectionReadonlyRestTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(1);
        config.setValidTestSql("SELECT 1 from dual");
        config.setIdleTimeout(3000);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws InterruptedException, Exception {
        Connection con1 = null;
        try {
            con1 = ds.getConnection();
            con1.setReadOnly(true);
            if (!con1.isReadOnly()) TestUtil.assertError("Connection Readonly set error");
//            con1.setReadOnly(false);
//            if (con1.isReadOnly()) TestUtil.assertError("Connection Readonly set error");
        } finally {
            if (con1 != null) con1.close();
        }

        Connection con2 = null;
        try {
            con2 = ds.getConnection();
            if (con2.isReadOnly()) TestUtil.assertError("Connection Readonly reset error");
        } finally {
            if (con2 != null) con2.close();
        }
    }
}
