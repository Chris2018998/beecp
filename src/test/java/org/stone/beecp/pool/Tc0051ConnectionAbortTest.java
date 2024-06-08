package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.config.DsConfigFactory;

import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0051ConnectionAbortTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setInitialSize(4);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void test() throws SQLException {
        Connection con = null;
        try {
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assert.assertEquals(4, vo.getIdleSize());
            con = ds.getConnection();
            con.abort(null);

            vo = ds.getPoolMonitorVo();
            Assert.assertEquals(3, vo.getIdleSize());
        } finally {
            if (con != null)
                oclose(con);
        }
    }
}

