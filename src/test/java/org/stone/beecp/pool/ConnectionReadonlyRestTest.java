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

    public void test() throws Exception {
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
