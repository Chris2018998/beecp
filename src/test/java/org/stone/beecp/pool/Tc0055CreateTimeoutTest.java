package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.objects.BorrowThread;
import org.stone.beecp.objects.MockNetBlockConnectionFactory;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0055CreateTimeoutTest extends TestCase {

    public void testNotTimeout() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertFalse(pool.isCreatingTimeout());

        BeeDataSourceConfig config2 = createDefault();
        config2.setCreateTimeout(3);
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        Assert.assertFalse(pool2.isCreatingTimeout());
    }

    public void testAutoInterruptionAfterCreateTimeout() throws SQLException {
        //2: interrupt waiter on lock
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10));
        config.setCreateTimeout(1);//1 seconds
        config.setTimerCheckInterval(TimeUnit.SECONDS.toMillis(2));//internal thread to interrupt waiters
        config.setRawConnectionFactory(new MockNetBlockConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        BorrowThread first = new BorrowThread(pool);
        first.start();
        TestUtil.joinUtilWaiting(first);

        Assert.assertTrue(pool.getCreatingTime() > 0);
        Assert.assertFalse(pool.isCreatingTimeout());
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
        Assert.assertTrue(pool.isCreatingTimeout());

        try {
            pool.getConnection();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("An interruption occurred on pool lock acquisition"));
        }
        pool.close();
    }
}
