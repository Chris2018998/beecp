package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.StoneLogAppender;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.objects.MockCreateExceptionConnectionFactory;
import org.stone.beecp.pool.exception.PoolInitializeFailedException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.base.TestUtil.getStoneLogAppender;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0059PoolClearTest extends TestCase {

    public void testClearAllIdles() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setMaxActive(2);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        Assert.assertEquals(2, pool.getTotalSize());
        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        pool.clear(false);
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains("begin to remove all connections"));
        Assert.assertTrue(logs.contains("removed all connections"));
        Assert.assertEquals(0, pool.getTotalSize());
        pool.close();

        //test clear on closed pool
        logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        pool.clear(false);
        logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.isEmpty());
    }

    public void testForceClearUsings() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        pool.getConnection();
        Assert.assertEquals(1, pool.getUsingSize());
        pool.clear(true);
        Assert.assertEquals(0, pool.getUsingSize());
        Assert.assertEquals(0, pool.getTotalSize());
    }

    public void testClearHoldTimeout() throws SQLException {//manual clear hold timeout connections
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setHoldTimeout(500L);// hold and not using connection;
        config.setDelayTimeForNextClear(500L);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        pool.getConnection();
        Assert.assertEquals(1, pool.getUsingSize());
        pool.clear(false);
        Assert.assertEquals(0, pool.getUsingSize());
        Assert.assertEquals(0, pool.getTotalSize());
    }

    public void testClearUsingOnReturn() throws Exception {//manual clear hold timeout connections
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setDelayTimeForNextClear(500L);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        DelayCloseThread borrowThread = new DelayCloseThread(pool);
        borrowThread.start();
        borrowThread.join();
        pool.clear(false);
        Assert.assertEquals(0, pool.getUsingSize());
        Assert.assertEquals(0, pool.getTotalSize());
    }

    public void testClearAndStartupPool() throws SQLException {
        BeeDataSourceConfig config1 = createDefault();
        config1.setInitialSize(5);
        config1.setMaxActive(5);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config1);
        Assert.assertEquals(5, pool.getTotalSize());

        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(10);
        config2.setMaxActive(10);
        pool.clear(false, config2);
        Assert.assertEquals(10, pool.getTotalSize());
    }

    public void testPoolRestart() throws SQLException {
        BeeDataSourceConfig config1 = createDefault();
        config1.setInitialSize(2);
        config1.setMaxActive(2);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config1);
        Assert.assertEquals(2, pool.getTotalSize());

        BeeDataSourceConfig config2 = createDefault();
        config2.setMaxActive(5);
        config2.setInitialSize(10);
        try {
            pool.clear(false, config2);
            fail("failed test clear");
        } catch (PoolInitializeFailedException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof BeeDataSourceConfigException);
            Assert.assertEquals("initialSize must not be greater than maxActive", cause.getMessage());

            config2.setMaxActive(10);
            config2.setInitialSize(10);
            pool.clear(false, config2);
            Assert.assertEquals(10, pool.getTotalSize());
        }

        BeeDataSourceConfig config3 = createDefault();
        config3.setInitialSize(1);
        config3.setConnectionFactory(new MockCreateExceptionConnectionFactory());
        try {
            pool.clear(false, config3);
            fail("failed test clear");
        } catch (SQLException e) {
            Assert.assertEquals("Network communications error", e.getMessage());
            config3.setConnectionFactory(null);

            pool.clear(false, config3);
            Assert.assertEquals(1, pool.getTotalSize());
        }
    }

    private final static class DelayCloseThread extends Thread {
        private final FastConnectionPool pool;

        public DelayCloseThread(FastConnectionPool pool) {
            this.pool = pool;
        }

        public void run() {
            try {
                Connection con = pool.getConnection();
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500L));
                con.close();
            } catch (Exception e) {
                //do nothing
            }
        }
    }
}
