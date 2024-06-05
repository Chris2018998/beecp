package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.JdbcConfig;

import java.sql.Connection;

public class Tc0064PoolDelayInitializeSuccessTest extends TestCase {
    public void setUp() {
        //do nothing
    }

    public void tearDown() {
        //do nothing
    }

    public void testPoolInit() throws Exception {
        int initSize = 5;
        BeeDataSource ds = new BeeDataSource();
        ds.setJdbcUrl(JdbcConfig.JDBC_URL);
        ds.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        ds.setUsername(JdbcConfig.JDBC_USER);
        ds.setPassword(JdbcConfig.JDBC_PASSWORD);

        ds.setInitialSize(initSize);

        Connection con = null;
        try {
            con = ds.getConnection();
            FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
            Assert.assertEquals(initSize, pool.getTotalSize());
        } finally {
            if (con != null) TestUtil.oclose(con);
            ds.close();
        }
    }
}
