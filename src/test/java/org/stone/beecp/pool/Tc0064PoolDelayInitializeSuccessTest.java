package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;

import java.sql.Connection;

import static org.stone.beecp.config.DsConfigFactory.*;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

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
        ds.setJdbcUrl(JDBC_URL);
        ds.setDriverClassName(JDBC_DRIVER);
        ds.setUsername(JDBC_USER);
        ds.setPassword(JDBC_PASSWORD);

        ds.setInitialSize(initSize);

        Connection con = null;
        try {
            con = ds.getConnection();
            FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
            Assert.assertEquals(initSize, pool.getTotalSize());
        } finally {
            if (con != null) oclose(con);
            ds.close();
        }
    }
}
