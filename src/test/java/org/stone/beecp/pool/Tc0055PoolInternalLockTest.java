package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.config.DsConfigFactory;
import org.stone.beecp.objects.BorrowThread;
import org.stone.beecp.objects.MockNetBlockConnectionFactory;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class Tc0055PoolInternalLockTest extends TestCase {
    private final Logger log = LoggerFactory.getLogger(Tc0055PoolInternalLockTest.class);

    public void testWaitOnPoolLock() throws SQLException {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10));
        config.setRawConnectionFactory(new MockNetBlockConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertEquals(0, pool.getIdleSize());

        BorrowThread first = new BorrowThread(pool);
        first.start();
        TestUtil.joinUtilWaiting(first);
        long lockHoldTime = pool.getPoolLockHoldTime();
        System.out.print("Current hold time point:" + lockHoldTime);
        Assert.assertTrue(lockHoldTime !=0L);//first thread has hold lock and blocked in driver

        BorrowThread second = new BorrowThread(pool);
        second.start();
        TestUtil.joinUtilWaiting(second);//wait on pool lock

        boolean firstInterrupted = false;
        boolean secondInterrupted = false;
        Thread[] threads = pool.interruptOnPoolLock();
        Assert.assertNotNull(threads);
        for (Thread thread : threads) {
            if (thread == first) firstInterrupted = true;
            else if (thread == second) secondInterrupted = true;
        }

        try {
            Assert.assertTrue(firstInterrupted);
            Assert.assertTrue(secondInterrupted);
        } finally {
            pool.close();
        }
    }
}
