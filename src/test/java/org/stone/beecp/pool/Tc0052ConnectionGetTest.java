package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.config.DsConfigFactory;
import org.stone.beecp.objects.MockNetBlockConnectionFactory;
import org.stone.beecp.objects.MockNetBlockXaConnectionFactory;
import org.stone.beecp.pool.exception.ConnectionCreateException;
import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.stone.base.TestUtil.joinUtilWaiting;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0052ConnectionGetTest extends TestCase {

    public void testGetSuccess() throws SQLException {
        BeeDataSource ds = new BeeDataSource(DsConfigFactory.createDefault());
        Connection con = null;

        try {
            con = ds.getConnection();
            Assert.assertNotNull(con);
        } finally {
            oclose(con);
            ds.close();
        }
    }

    public void testStuckInDriver() throws SQLException {
        //1: stuck in connection factory
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(1));
        config.setRawConnectionFactory(new MockNetBlockConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        new InterruptedThread(Thread.currentThread()).start();

        try {
            pool.getConnection();
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof ConnectionGetInterruptedException);
            Assert.assertTrue(e.getMessage().contains("An interruption occurred in connection factory"));
        }
        pool.close();

        //2: stuck in xa-connection factory
        BeeDataSourceConfig config2 = DsConfigFactory.createDefault();
        config2.setInitialSize(0);
        config2.setMaxActive(2);
        config2.setMaxWait(TimeUnit.SECONDS.toMillis(1));
        config2.setRawXaConnectionFactory(new MockNetBlockXaConnectionFactory());
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        new InterruptedThread(Thread.currentThread()).start();

        try {
            pool2.getXAConnection();
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof ConnectionGetInterruptedException);
            Assert.assertTrue(e.getMessage().contains("An interruption occurred in xa-connection factory"));
        }
        pool2.close();
    }

    public void testGetTimeout() throws Exception {
        //1:timeout on pool semaphore
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(1));
        config.setRawConnectionFactory(new MockNetBlockConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        FirstGetThread first = new FirstGetThread(pool);//mock stuck in driver.getConnection()
        first.start();
        TestUtil.joinUtilWaiting(first);
        try {
            pool.getConnection();
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof ConnectionGetTimeoutException);
            Assert.assertTrue(e.getMessage().contains("Wait timeout on pool semaphore acquisition"));
            first.interrupt();
        }
        pool.close();

        //2:timeout on pool lock
        BeeDataSourceConfig config2 = DsConfigFactory.createDefault();
        config2.setInitialSize(0);
        config2.setMaxActive(2);
        config2.setBorrowSemaphoreSize(2);
        config2.setMaxWait(TimeUnit.SECONDS.toMillis(1));
        config2.setRawConnectionFactory(new MockNetBlockConnectionFactory());
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);

        first = new FirstGetThread(pool2);//mock stuck in driver.getConnection()
        first.start();
        TestUtil.joinUtilWaiting(first);
        try {
            pool2.getConnection();
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof ConnectionCreateException);
            Assert.assertTrue(e.getMessage().contains("Wait timeout on pool lock acquisition"));
            first.interrupt();
        }
        pool2.close();


        //3: timeout in wait queue
        BeeDataSourceConfig config3 = DsConfigFactory.createDefault();
        config3.setInitialSize(0);
        config3.setMaxActive(1);
        config3.setForceCloseUsingOnClear(true);
        config3.setBorrowSemaphoreSize(2);
        config3.setMaxWait(TimeUnit.SECONDS.toMillis(1));
        FastConnectionPool pool3 = new FastConnectionPool();
        pool3.init(config3);
        first = new FirstGetThread(pool3);
        first.start();
        TestUtil.joinUtilWaiting(first);
        try {
            pool3.getConnection();
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof ConnectionGetTimeoutException);
            Assert.assertTrue(e.getMessage().contains("Wait timeout for a released connection"));
        }
        pool3.close();
    }

    public void testGetInterruption() throws Exception {
        //1: interrupt waiter on semaphore
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(1));
        config.setRawConnectionFactory(new MockNetBlockConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        FirstGetThread first = new FirstGetThread(pool);
        first.start();
        TestUtil.joinUtilWaiting(first);
        new InterruptedThread(Thread.currentThread()).start();
        try {
            pool.getConnection();
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof ConnectionGetInterruptedException);
            Assert.assertTrue(e.getMessage().contains("An interruption occurred on pool semaphore acquisition"));
            first.interrupt();
        }
        pool.close();

        //2: interrupt waiter on lock
        BeeDataSourceConfig config2 = DsConfigFactory.createDefault();
        config2.setInitialSize(0);
        config2.setMaxActive(2);
        config2.setBorrowSemaphoreSize(2);
        config2.setMaxWait(TimeUnit.SECONDS.toMillis(1));
        config2.setRawConnectionFactory(new MockNetBlockConnectionFactory());
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);

        first = new FirstGetThread(pool2);
        first.start();
        TestUtil.joinUtilWaiting(first);

        new InterruptedThread(Thread.currentThread()).start();

        try {
            pool2.getConnection();
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof ConnectionCreateException);
            Assert.assertTrue(e.getMessage().contains("An interruption occurred on pool lock acquisition"));
            first.interrupt();
        }
        pool2.close();
    }

    private static class FirstGetThread extends Thread {
        private final boolean getXA;
        private final FastConnectionPool pool;

        FirstGetThread(FastConnectionPool pool) {
            this(pool, false);
        }

        FirstGetThread(FastConnectionPool pool, boolean getXA) {
            this.pool = pool;
            this.getXA = getXA;
            this.setDaemon(true);
        }

        public void run() {
            try {
                if (getXA)
                    pool.getXAConnection();
                else
                    pool.getConnection();
            } catch (SQLException e) {
                Assert.assertTrue(e instanceof ConnectionGetInterruptedException);
            }
        }
    }

    //A mock thread to interrupt wait threads on ds-read lock
    private static class InterruptedThread extends Thread {
        private final Thread readThread;

        InterruptedThread(Thread readThread) {
            this.readThread = readThread;
        }

        public void run() {
            try {
                if (joinUtilWaiting(readThread))
                    readThread.interrupt();
            } catch (Exception e) {
                //do nothing
            }
        }
    }
}
