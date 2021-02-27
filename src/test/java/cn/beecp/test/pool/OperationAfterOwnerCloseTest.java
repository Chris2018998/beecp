/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.beecp.test.pool;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import cn.beecp.test.Config;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;

import java.sql.*;

public class OperationAfterOwnerCloseTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(Config.JDBC_URL);
        config.setDriverClassName(Config.JDBC_DRIVER);
        config.setUsername(Config.JDBC_USER);
        config.setPassword(Config.JDBC_PASSWORD);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void testConnectionClose() throws InterruptedException, Exception {
        Connection con = ds.getConnection();

        Statement st = null;
        CallableStatement cs = null;
        PreparedStatement ps = null;
        try {
            st = con.createStatement();
            cs = con.prepareCall("?={call " + Config.TEST_PROCEDURE + "}");
            ps = con.prepareStatement("select 1 from dual");
            DatabaseMetaData dbs = con.getMetaData();

            con.close();
            try {
                st.getConnection();
                TestUtil.assertError("statement operation after connection close(dbs)");
            } catch (SQLException e) {
            }

            try {
                ps.getConnection();
                TestUtil.assertError("preparedStatement operation after connection close(ps)");
            } catch (SQLException e) {
            }

            try {
                cs.getConnection();
                TestUtil.assertError("callableStatement operation after connection close(cs)");
            } catch (SQLException e) {
            }

            try {
                dbs.getConnection();
                TestUtil.assertError("DatabaseMetaData operation after connection close(dbs)");
            } catch (SQLException e) {
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

    public void testStatementClose() throws InterruptedException, Exception {
        Connection con = ds.getConnection();

        Statement st = null;
        CallableStatement cs = null;
        PreparedStatement ps = null;
        try {
            st = con.createStatement();
            cs = con.prepareCall("?={call " + Config.TEST_PROCEDURE + "}");
            ps = con.prepareStatement("select 1 from dual");
            ResultSet rs1 = null;

            try {
                rs1 = st.getResultSet();
                st.close();
                if (rs1 != null) {
                    rs1.getStatement();
                    TestUtil.assertError("result operation after statememnt close(st)");
                }
            } catch (SQLException e) {
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
                    TestUtil.assertError("result operation after preparedStatement close(ps)");
                }
            } catch (SQLException e) {
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
                    TestUtil.assertError("result operation after callableStatement close(cs)");
                }
            } catch (SQLException e) {
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
