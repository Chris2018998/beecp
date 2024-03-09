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

import java.sql.*;

public class ConnectionWrapperTest extends TestCase {
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

    public void test() throws Exception {
        Connection con = null;
        Statement st = null;
        CallableStatement cs = null;
        PreparedStatement ps = null;

        try {
            con = ds.getConnection();
            st = con.createStatement();
            cs = con.prepareCall("?={call test(}");
            ps = con.prepareStatement("select 1 from dual");
            DatabaseMetaData dbs = con.getMetaData();

            if (con.unwrap(Connection.class) != con)
                TestUtil.assertError("Raw conneciton exposed(unwrap)");
            if (st.getConnection() != con)
                TestUtil.assertError("Raw conneciton exposed(st)");
            if (cs.getConnection() != con)
                TestUtil.assertError("Raw conneciton exposed(cs)");
            if (ps.getConnection() != con)
                TestUtil.assertError("Raw conneciton exposed(ps)");
            if (dbs.getConnection() != con)
                TestUtil.assertError("Raw conneciton exposed(dbs)");

            ResultSet re1 = st.executeQuery("select 1 from dual");
            if (re1 != null && re1.getStatement() != st)
                TestUtil.assertError("Raw Statement exposed(st)");
            ResultSet re2 = ps.executeQuery();
            if (re2 != null && re2.getStatement() != ps)
                TestUtil.assertError("Raw Statement exposed(ps)");
            ResultSet re3 = cs.getResultSet();
            if (re3 != null && re3.getStatement() != cs)
                TestUtil.assertError("Raw Statement exposed(cs)");
            ResultSet re4 = dbs.getTableTypes();
            if (re4 != null && re4.getStatement() != null)
                TestUtil.assertError("Raw Statement exposed(dbs)");

            if (re1 != null && re1.getStatement() != st)
                TestUtil.assertError("Raw Result exposed(st)");
            if (re2 != null && re2.getStatement() != ps)
                TestUtil.assertError("Raw Result exposed(ps)");
            if (re3 != null && re3.getStatement() != cs)
                TestUtil.assertError("Raw Result exposed(cs)");
        } finally {
            if (st != null)
                TestUtil.oclose(st);
            if (cs != null)
                TestUtil.oclose(cs);
            if (ps != null)
                TestUtil.oclose(ps);
            if (con != null)
                TestUtil.oclose(con);
        }
    }
}
