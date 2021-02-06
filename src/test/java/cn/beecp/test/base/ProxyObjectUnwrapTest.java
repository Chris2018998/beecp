package cn.beecp.test.base;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import cn.beecp.test.Config;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;

import java.sql.*;

public class ProxyObjectUnwrapTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(Config.JDBC_URL);
        config.setDriverClassName(Config.JDBC_DRIVER);
        config.setUsername(Config.JDBC_USER);
        config.setPassword(Config.JDBC_PASSWORD);
        config.setInitialSize(5);
        config.setConnectionTestSQL("SELECT 1 from dual");
        config.setIdleTimeout(3000);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws InterruptedException, Exception {
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
