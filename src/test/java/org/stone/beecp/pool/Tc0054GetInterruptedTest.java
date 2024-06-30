package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.objects.BorrowThread;
import org.stone.beecp.objects.InterruptionAction;
import org.stone.beecp.objects.MockNetBlockConnectionFactory;
import org.stone.beecp.pool.exception.ConnectionCreateException;
import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0054GetInterruptedTest extends TestCase {

    public void testGetInterruptionOnSemaphore() throws SQLException {
        //1: interrupt waiter on semaphore
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(1);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10));
        config.setRawConnectionFactory(new MockNetBlockConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        BorrowThread first = new BorrowThread(pool);
        first.start();
        TestUtil.joinUtilWaiting(first);
        new InterruptionAction(Thread.currentThread()).start();

        try {
            pool.getConnection();
        } catch (ConnectionGetInterruptedException e) {
            Assert.assertTrue(e.getMessage().contains("An interruption occurred on pool semaphore acquisition"));
            first.interrupt();
        }
        pool.close();
    }

    public void testGetInterruptionOnPoolLock() throws SQLException {
        //2: interrupt waiter on lock
        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(0);
        config2.setMaxActive(2);
        config2.setBorrowSemaphoreSize(2);
        config2.setMaxWait(TimeUnit.SECONDS.toMillis(10));
        config2.setRawConnectionFactory(new MockNetBlockConnectionFactory());
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);

        BorrowThread first = new BorrowThread(pool2);
        first.start();
        TestUtil.joinUtilWaiting(first);
        new InterruptionAction(Thread.currentThread()).start();//main thread will be blocked on pool lock

        try {
            pool2.getConnection();
        } catch (ConnectionCreateException e) {
            Assert.assertTrue(e.getMessage().contains("An interruption occurred on pool lock acquisition"));
            first.interrupt();
        }
        pool2.close();
    }

    public void testAutoInterruptionOnPoolLock() throws SQLException {
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

        Assert.assertTrue(pool.getPoolLockHoldTime() > 0);
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        Assert.assertTrue(pool.isPoolLockHoldTimeout());

        try {
            pool.getConnection();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("An interruption occurred on pool lock acquisition"));
        }
        pool.close();
    }


    public void testGetInterruptionOnWaitQueue() throws SQLException {
        //3: timeout in wait queue
        BeeDataSourceConfig config3 = createDefault();
        config3.setInitialSize(0);
        config3.setMaxActive(1);
        config3.setForceCloseUsingOnClear(true);
        config3.setBorrowSemaphoreSize(2);
        config3.setMaxWait(TimeUnit.SECONDS.toMillis(10));
        FastConnectionPool pool3 = new FastConnectionPool();
        pool3.init(config3);
        BorrowThread first = new BorrowThread(pool3);
        first.start();

        TestUtil.joinUtilWaiting(first);
        new InterruptionAction(Thread.currentThread()).start();//main thread will be blocked on pool lock

        try {
            pool3.getConnection();
        } catch (ConnectionGetInterruptedException e) {
            Assert.assertTrue(e.getMessage().contains("An interruption occurred while waiting for a released connection"));
        }
    }
}
