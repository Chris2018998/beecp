package org.stone.beecp.pool;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class ConnectionHoldNotTimeoutTest extends TestCase {

    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        config.setInitialSize(0);
        config.setValidTestSql("SELECT 1 from dual");

        config.setHoldTimeout(0);//not timeout in hold
        config.setTimerCheckInterval(1000L);// two seconds interval
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws Exception {
        Connection con = null;
        try {
            FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
            con = ds.getConnection();
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));//first sleeping
            if (pool.getTotalSize() != 1)
                TestUtil.assertError("Total connections not as expected 1");
            if (pool.getUsingSize() != 1)
                TestUtil.assertError("Using connections not as expected 1");

            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));//second sleeping
            if (pool.getTotalSize() != 1)
                TestUtil.assertError("Total connections not as expected 1");
            if (pool.getUsingSize() != 1)
                TestUtil.assertError("Using connections not as expected 1");

            try {
                con.getCatalog();
            } catch (SQLException e) {
                TestUtil.assertError("Connection has been recycled by force");
            }
        } finally {
            if (con != null)
                TestUtil.oclose(con);
        }
    }

}
