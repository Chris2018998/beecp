package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.*;

import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0492ProxyObjectUnwrapTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(5);
        config.setAliveTestSql("SELECT 1 from dual");
        config.setIdleTimeout(3000);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testUnwrap() throws Exception {
        Connection con = null;
        Statement st = null;
        PreparedStatement ps = null;
        CallableStatement cs = null;
        ResultSet re = null;
        try {
            con = ds.getConnection();
            Assert.assertEquals(con, con.unwrap(Connection.class));

            st = con.createStatement();
            Assert.assertEquals(st, st.unwrap(Statement.class));

            ps = con.prepareStatement("select 1 from dual");
            Assert.assertEquals(ps, ps.unwrap(PreparedStatement.class));

            cs = con.prepareCall("?={call test(}");
            Assert.assertEquals(cs, cs.unwrap(CallableStatement.class));

            DatabaseMetaData metaData = con.getMetaData();
            Assert.assertEquals(metaData, metaData.unwrap(DatabaseMetaData.class));

            re = ps.executeQuery();
            Assert.assertEquals(re, re.unwrap(ResultSet.class));
        } finally {
            if (re != null)
                oclose(re);
            if (st != null)
                oclose(st);
            if (cs != null)
                oclose(cs);
            if (ps != null)
                oclose(ps);
            if (con != null)
                oclose(con);
        }
    }
}
