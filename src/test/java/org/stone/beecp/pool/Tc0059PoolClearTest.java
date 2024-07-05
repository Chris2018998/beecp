package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.StoneLogAppender;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.Connection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.base.TestUtil.getStoneLogAppender;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0059PoolClearTest extends TestCase {

    public void testClearAllIdles() throws Exception {
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

    public void testForceClearUsings() throws Exception {
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

    public void testClearHoldTimeout() throws Exception {//manual clear hold timeout connections
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

        new DelayCloseThread(pool).start();
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500L));
        Assert.assertEquals(1, pool.getUsingSize());
        pool.clear(false);
        Assert.assertEquals(0, pool.getUsingSize());
        Assert.assertEquals(0, pool.getTotalSize());
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
