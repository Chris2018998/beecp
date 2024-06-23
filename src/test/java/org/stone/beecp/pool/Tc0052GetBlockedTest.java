package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.objects.InterruptionAction;
import org.stone.beecp.objects.MockNetBlockConnectionFactory;
import org.stone.beecp.objects.MockNetBlockXaConnectionFactory;
import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0052GetBlockedTest extends TestCase {

    public void testGetSuccess() throws SQLException {
        BeeDataSource ds = new BeeDataSource(createDefault());
        Connection con = null;

        try {
            con = ds.getConnection();
            Assert.assertNotNull(con);
        } finally {
            oclose(con);
            ds.close();
        }
    }

    public void testBlockedInCreatingConnection() throws SQLException {
        //1: connection creation blocked in driver(mock)
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(1));
        config.setRawConnectionFactory(new MockNetBlockConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        new InterruptionAction(Thread.currentThread()).start();

        try {
            pool.getConnection();
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof ConnectionGetInterruptedException);
            Assert.assertTrue(e.getMessage().contains("An interruption occurred in connection factory"));
        }
        pool.close();
    }

    public void testBlockedInCreatingXaConnection() throws SQLException {
        //2: xa-connection creation blocked in driver(mock)
        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(0);
        config2.setMaxActive(2);
        config2.setMaxWait(TimeUnit.SECONDS.toMillis(1));
        config2.setRawXaConnectionFactory(new MockNetBlockXaConnectionFactory());
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        new InterruptionAction(Thread.currentThread()).start();
        try {
            pool2.getXAConnection();
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof ConnectionGetInterruptedException);
            Assert.assertTrue(e.getMessage().contains("An interruption occurred in xa-connection factory"));
        }
        pool2.close();
    }
}


