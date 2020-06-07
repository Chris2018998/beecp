package cn.beecp.test.base;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import cn.beecp.test.Config;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;
import cn.beecp.util.BeecpUtil;

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
        config.setIdleCheckTimeInitDelay(10);
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
        ResultSet re=null;
        try {
            con = ds.getConnection();
           if(con.unwrap( Connection.class)!=con)
               TestUtil.assertError("Connection unwrap error");

            st = con.createStatement();
            if(st.unwrap( Statement.class)!=st)
                TestUtil.assertError("Statement unwrap error");

            ps = con.prepareStatement("select 1 from dual");
            if(ps.unwrap( PreparedStatement.class)!=ps)
                TestUtil.assertError("PrepareStatement unwrap error");

            cs = con.prepareCall("?={call test(}");
            if(cs.unwrap(CallableStatement.class)!=cs)
                TestUtil.assertError("CallableStatement unwrap error");

            DatabaseMetaData metaData= con.getMetaData();
            if(metaData.unwrap(DatabaseMetaData.class)!=metaData)
                TestUtil.assertError("DatabaseMetaData unwrap error");

            re=ps.executeQuery();
            if(re.unwrap(ResultSet.class)!=re)
                TestUtil.assertError("ResultSet unwrap error");
        } finally {
            if (re != null)
                BeecpUtil.oclose(re);
            if (st != null)
                BeecpUtil.oclose(st);
            if (cs != null)
                BeecpUtil.oclose(cs);
            if (ps != null)
                BeecpUtil.oclose(ps);
            if (con != null)
                BeecpUtil.oclose(con);
        }
    }
}
