/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;

import java.sql.*;

public class Tc0062OperationAfterOwnerCloseTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testConnectionClose() throws Exception {
        Connection con = ds.getConnection();

        Statement st = null;
        CallableStatement cs = null;
        PreparedStatement ps = null;
        try {
            st = con.createStatement();
            cs = con.prepareCall("?={call " + JdbcConfig.TEST_PROCEDURE + "}");
            ps = con.prepareStatement("select 1 from dual");
            DatabaseMetaData dbs = con.getMetaData();

            con.close();
            try {
                st.getConnection();
                fail("statement operation after connection close(dbs)");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof SQLException);
            }

            try {
                ps.getConnection();
                fail("preparedStatement operation after connection close(ps)");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof SQLException);
            }

            try {
                cs.getConnection();
                fail("callableStatement operation after connection close(cs)");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof SQLException);
            }

            try {
                dbs.getConnection();
                fail("DatabaseMetaData operation after connection close(dbs)");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof SQLException);
            }
        } finally {
            if (st != null)
                TestUtil.oclose(st);
            if (cs != null)
                TestUtil.oclose(cs);
            if (ps != null)
                TestUtil.oclose(ps);
        }
    }

    public void testStatementClose() throws Exception {
        Connection con = ds.getConnection();

        Statement st;
        CallableStatement cs;
        PreparedStatement ps;
        try {
            st = con.createStatement();
            cs = con.prepareCall("?={call " + JdbcConfig.TEST_PROCEDURE + "}");
            ps = con.prepareStatement("select 1 from dual");
            ResultSet rs1 = null;

            try {
                rs1 = st.getResultSet();
                st.close();
                if (rs1 != null) {
                    rs1.getStatement();
                    fail("result operation after statememnt close(st)");
                }
            } catch (Exception e) {
                Assert.assertTrue(e instanceof SQLException);
            } finally {
                if (rs1 != null)
                    TestUtil.oclose(rs1);
            }

            ResultSet rs2 = null;
            try {
                rs2 = ps.getResultSet();
                ps.close();
                if (rs2 != null) {
                    rs2.getStatement();
                    fail("result operation after preparedStatement close(ps)");
                }
            } catch (Exception e) {
                Assert.assertTrue(e instanceof SQLException);
            } finally {
                if (rs2 != null)
                    TestUtil.oclose(rs2);
            }

            ResultSet rs3 = null;
            try {
                rs3 = cs.getResultSet();
                cs.close();
                if (rs3 != null) {
                    rs3.getStatement();
                    fail("result operation after callableStatement close(cs)");
                }
            } catch (Exception e) {
                Assert.assertTrue(e instanceof SQLException);
            } finally {
                if (rs3 != null)
                    TestUtil.oclose(rs3);
            }
        } finally {
            if (con != null)
                TestUtil.oclose(con);
        }
    }
}
