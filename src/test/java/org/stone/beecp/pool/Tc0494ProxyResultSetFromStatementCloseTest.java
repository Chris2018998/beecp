package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0494ProxyResultSetFromStatementCloseTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setAliveTestSql("SELECT 1 from dual");
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testProxyResultSet() throws Exception {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = ds.getConnection();
            ps = con.prepareStatement("select * from BEECP_TEST");
            rs = ps.executeQuery();
            Assert.assertEquals(rs, ps.getResultSet());
            rs.close();
            Assert.assertNull(ps.getResultSet());
        } finally {
            if (rs != null) oclose(rs);
            if (ps != null) oclose(ps);
            if (con != null) oclose(con);
        }
    }
}
