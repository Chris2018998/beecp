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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class ProxyObjectClosedStateTest extends TestCase {
    private BeeDataSource ds;

    private static void statementProxy(Connection con) throws Exception {
        Statement st = null;
        try {
            st = con.createStatement();
            st.close();
            if (!st.isClosed())
                TestUtil.assertError("Statement is not closed");
        } finally {
            TestUtil.oclose(st);
        }
    }

    private static void preparedStatementProxy(Connection con) throws Exception {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("select 1 from dual");
            ps.close();
            if (!ps.isClosed())
                TestUtil.assertError("PreparedStatement is not closed");
        } finally {
            TestUtil.oclose(ps);
        }
    }

    private static void callableStatementProxy(Connection con) throws Exception {
        CallableStatement cs = null;
        try {
            cs = con.prepareCall("?={call test(}");
            cs.close();
            if (!cs.isClosed())
                TestUtil.assertError("CallableStatement is not closed");
        } finally {
            TestUtil.oclose(cs);
        }
    }

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        config.setInitialSize(5);
        config.setValidTestSql("SELECT 1 from dual");
        config.setIdleTimeout(3000);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void testProxyObject() throws Exception {
        Connection con = null;
        try {
            con = ds.getConnection();
            statementProxy(con);
            preparedStatementProxy(con);
            callableStatementProxy(con);

            con.close();
            if (!con.isClosed())
                TestUtil.assertError("Connection is not closed");
        } finally {
            if (con != null) TestUtil.oclose(con);
        }
    }
}
