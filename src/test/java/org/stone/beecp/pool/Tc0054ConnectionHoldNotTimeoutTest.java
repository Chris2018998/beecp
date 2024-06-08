package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.config.DsConfigFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0054ConnectionHoldNotTimeoutTest extends TestCase {

    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setInitialSize(0);
        config.setAliveTestSql("SELECT 1 from dual");
        config.setHoldTimeout(0);//not timeout in hold

        config.setTimerCheckInterval(1000L);// two seconds interval
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void test() throws Exception {
        Connection con = null;
        try {
            FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
            con = ds.getConnection();
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));//first sleeping

            Assert.assertEquals(1, pool.getTotalSize());
            Assert.assertEquals(1, pool.getUsingSize());

            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));//second sleeping

            Assert.assertEquals(1, pool.getTotalSize());
            Assert.assertEquals(1, pool.getUsingSize());

            try {
                con.getCatalog();
            } catch (SQLException e) {
                Assert.assertEquals(e.getMessage(), "Connection has been recycled by force");
            }
        } finally {
            if (con != null)
                oclose(con);
        }
    }
}
