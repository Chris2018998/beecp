package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.config.DsConfigFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Tc0073ProxyResultSetFromStatementCloseTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setInitialSize(0);
        config.setAliveTestSql("SELECT 1 from dual");
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void test() throws Exception {
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
            if (rs != null) TestUtil.oclose(rs);
            if (ps != null) TestUtil.oclose(ps);
            if (con != null) TestUtil.oclose(con);
        }
    }
}
