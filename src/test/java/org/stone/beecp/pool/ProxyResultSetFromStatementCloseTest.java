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
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ProxyResultSetFromStatementCloseTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        config.setInitialSize(0);
        config.setValidTestSql("SELECT 1 from dual");
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws Exception {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = ds.getConnection();
            ps = con.prepareStatement("select * from BEECP_TEST");
            rs = ps.executeQuery();
            if (ps.getResultSet() != rs) TestUtil.assertError("ps.getResultSet() != ps.executeQuery()");
            rs.close();
            if (ps.getResultSet() != null) TestUtil.assertError("ps.getResultSet() != null after ResultSet.close");
        } finally {
            if (rs != null) TestUtil.oclose(rs);
            if (ps != null) TestUtil.oclose(ps);
            if (con != null) TestUtil.oclose(con);
        }
    }
}
