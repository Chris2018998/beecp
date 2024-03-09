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
import org.stone.beecp.JdbcConfig;

import java.sql.Connection;
import java.sql.SQLException;

public class PoolDelayInitializeFailedTest extends TestCase {
    private final int initSize = 5;

    public void setUp() throws Throwable {
        //do nothing
    }

    public void tearDown() throws Throwable {
        //do nothing
    }

    public void testPoolInit() throws Exception {
        Connection con = null;
        BeeDataSource ds = null;
        try {
            ds = new BeeDataSource();
            ds.setJdbcUrl("jdbc:beecp://localhost/testdb2");//give valid URL
            ds.setDriverClassName(JdbcConfig.JDBC_DRIVER);
            ds.setUsername(JdbcConfig.JDBC_USER);
            ds.setPassword(JdbcConfig.JDBC_PASSWORD);
            ds.setInitialSize(initSize);
            con = ds.getConnection();
            TestUtil.assertError("A pool fail to init e need be thrown,but not");
        } catch (SQLException e) {
        } finally {
            if (con != null)
                TestUtil.oclose(con);
        }
    }
}
