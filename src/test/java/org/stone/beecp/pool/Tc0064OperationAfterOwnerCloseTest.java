/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.*;

import static org.stone.beecp.config.DsConfigFactory.*;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0064OperationAfterOwnerCloseTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setDriverClassName(JDBC_DRIVER);
        config.setUsername(JDBC_USER);
        config.setPassword(JDBC_PASSWORD);
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
            cs = con.prepareCall("?={call " + TEST_PROCEDURE + "}");
            ps = con.prepareStatement("select 1 from dual");
            DatabaseMetaData dbs = con.getMetaData();

            con.close();
            try {
                st.getConnection();
                fail("statement operation after connection close(dbs)");
            } catch (SQLException e) {
                Assert.assertTrue(e.getMessage().contains("No operations allowed after statement closed"));
            }

            try {
                ps.getConnection();
                fail("preparedStatement operation after connection close(ps)");
            } catch (SQLException e) {
                Assert.assertTrue(e.getMessage().contains("No operations allowed after statement closed"));
            }

            try {
                cs.getConnection();
                fail("callableStatement operation after connection close(cs)");
            } catch (SQLException e) {
                Assert.assertTrue(e.getMessage().contains("No operations allowed after statement closed"));
            }

            try {
                dbs.getConnection();
                fail("DatabaseMetaData operation after connection close(dbs)");
            } catch (SQLException e) {
                Assert.assertTrue(e.getMessage().contains("No operations allowed after connection closed"));
            }
        } finally {
            if (st != null)
                oclose(st);
            if (cs != null)
                oclose(cs);
            if (ps != null)
                oclose(ps);
        }
    }

    public void testStatementClose() throws Exception {
        Connection con = ds.getConnection();

        Statement st;
        CallableStatement cs;
        PreparedStatement ps;
        try {
            st = con.createStatement();
            cs = con.prepareCall("?={call " + TEST_PROCEDURE + "}");
            ps = con.prepareStatement("select 1 from dual");
            ResultSet rs1 = null;

            try {
                rs1 = st.getResultSet();
                st.close();
                if (rs1 != null) {
                    rs1.getStatement();
                    fail("result operation after statement close(st)");
                }
            } catch (SQLException e) {
                Assert.assertTrue(e.getMessage().contains("No operations allowed after statement closed"));
            } finally {
                if (rs1 != null) oclose(rs1);
            }

            ResultSet rs2 = null;
            try {
                rs2 = ps.getResultSet();
                ps.close();
                if (rs2 != null) {
                    rs2.getStatement();
                    fail("result operation after preparedStatement close(ps)");
                }
            } catch (SQLException e) {
                Assert.assertTrue(e.getMessage().contains("No operations allowed after statement closed"));
            } finally {
                if (rs2 != null) oclose(rs2);
            }

            ResultSet rs3 = null;
            try {
                rs3 = cs.getResultSet();
                cs.close();
                if (rs3 != null) {
                    rs3.getStatement();
                    fail("result operation after callableStatement close(cs)");
                }
            } catch (SQLException e) {
                Assert.assertTrue(e.getMessage().contains("No operations allowed after statement closed"));
            } finally {
                if (rs3 != null) oclose(rs3);
            }
        } finally {
            if (con != null) oclose(con);
        }
    }
}
