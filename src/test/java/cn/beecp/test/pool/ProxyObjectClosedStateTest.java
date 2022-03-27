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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class ProxyObjectClosedStateTest extends TestCase {
    private BeeDataSource ds;

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

    private void testStatement(Connection con) throws Exception {
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

    private void testPreparedStatement(Connection con) throws Exception {
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

    private void testCallableStatement(Connection con) throws Exception {
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

    public void test() throws Exception {
        Connection con = null;
        try {
            con = ds.getConnection();
            testStatement(con);
            testPreparedStatement(con);
            testCallableStatement(con);
            con.close();
            if (!con.isClosed())
                TestUtil.assertError("Connection is not closed");
            con = null;
        } finally {
            TestUtil.oclose(con);
        }
    }
}
