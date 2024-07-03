package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.*;

import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0103ConnectionWrapperTest extends TestCase {
    private FastConnectionPool pool;

    public void setUp() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(5);
        config.setAliveTestSql("SELECT 1 from dual");
        config.setIdleTimeout(3000);

        pool = new FastConnectionPool();
        pool.init(config);
    }

    public void tearDown() throws Exception {
        pool.close();
    }

    public void testConnectionWrapper() throws Exception {
        Connection con = null;
        Statement st = null;
        CallableStatement cs = null;
        PreparedStatement ps = null;

        try {
            con = pool.getConnection();
            st = con.createStatement();
            cs = con.prepareCall("?={call test(}");
            ps = con.prepareStatement("select 1 from dual");
            DatabaseMetaData dbs = con.getMetaData();

            Assert.assertEquals(con.unwrap(Connection.class), con);
            Assert.assertEquals(st.getConnection(), con);
            Assert.assertEquals(cs.getConnection(), con);
            Assert.assertEquals(ps.getConnection(), con);
            Assert.assertEquals(dbs.getConnection(), con);

            ResultSet re1 = st.executeQuery("select 1 from dual");
            Assert.assertEquals(st, re1.getStatement());
            ResultSet re2 = ps.executeQuery();
            Assert.assertEquals(ps, re2.getStatement());
            ResultSet re3 = cs.getResultSet();
            Assert.assertNull(re3);
            ResultSet re4 = dbs.getTableTypes();
            Assert.assertNull(re4.getStatement());

            Assert.assertEquals(st, re1.getStatement());
            Assert.assertEquals(ps, re2.getStatement());
        } finally {
            oclose(st);
            oclose(cs);
            oclose(ps);
            oclose(con);
        }
    }
}
