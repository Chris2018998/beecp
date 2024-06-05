package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;

import java.sql.*;

public class Tc0071ProxyObjectUnwrapTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);

        config.setInitialSize(5);
        config.setAliveTestSql("SELECT 1 from dual");
        config.setIdleTimeout(3000);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void test() throws Exception {
        Connection con = null;
        Statement st = null;
        PreparedStatement ps = null;
        CallableStatement cs = null;
        ResultSet re = null;
        try {
            con = ds.getConnection();
            if (con.unwrap(Connection.class) != con)
                TestUtil.assertError("Connection unwrap error");

            st = con.createStatement();
            if (st.unwrap(Statement.class) != st)
                TestUtil.assertError("Statement unwrap error");

            ps = con.prepareStatement("select 1 from dual");
            if (ps.unwrap(PreparedStatement.class) != ps)
                TestUtil.assertError("PrepareStatement unwrap error");

            cs = con.prepareCall("?={call test(}");
            if (cs.unwrap(CallableStatement.class) != cs)
                TestUtil.assertError("CallableStatement unwrap error");

            DatabaseMetaData metaData = con.getMetaData();
            if (metaData.unwrap(DatabaseMetaData.class) != metaData)
                TestUtil.assertError("DatabaseMetaData unwrap error");

            re = ps.executeQuery();
            if (re.unwrap(ResultSet.class) != re)
                TestUtil.assertError("ResultSet unwrap error");
        } finally {
            if (re != null)
                TestUtil.oclose(re);
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
