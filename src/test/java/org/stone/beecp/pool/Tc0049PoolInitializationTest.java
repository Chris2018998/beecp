/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.StoneLogAppender;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.config.DsConfigFactory;
import org.stone.beecp.objects.MockCreateExceptionConnectionFactory;
import org.stone.beecp.objects.MockFailSizeReachConnectionFactory;
import org.stone.beecp.objects.MockNetBlockConnectionFactory;
import org.stone.beecp.pool.exception.ConnectionCreateException;
import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;
import org.stone.beecp.pool.exception.PoolInitializeFailedException;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.stone.base.TestUtil.getStoneLogAppender;
import static org.stone.base.TestUtil.joinUtilWaiting;
import static org.stone.beecp.config.DsConfigFactory.JDBC_DRIVER;
import static org.stone.beecp.config.DsConfigFactory.JDBC_URL;

public class Tc0049PoolInitializationTest extends TestCase {

    public void testNullConfig() throws Exception {
        FastConnectionPool pool = new FastConnectionPool();
        try {
            pool.init(null);
        } catch (PoolInitializeFailedException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Pool initialization configuration can't be null"));
        }
    }

    public void testDuplicatedInitialization() throws Exception {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        try {
            pool.init(config);
            fail("testDuplicatedInitialization test failed");
        } catch (PoolInitializeFailedException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Pool has already been initialized or in initializing"));
        }
    }

    public void testUrlNotMatchDriver() throws Exception {
        FastConnectionPool pool = new FastConnectionPool();
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl("Test:" + JDBC_URL);
        config.setDriverClassName(JDBC_DRIVER);
        try {
            pool.init(config);
            fail("test failed on driver match");
        } catch (PoolInitializeFailedException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof BeeDataSourceConfigException);
            String message = cause.getMessage();
            Assert.assertTrue(message != null && message.contains("can not match configured driver"));
        }
    }

    public void testCheckFailedOnInitialization() throws Exception {
        FastConnectionPool pool = new FastConnectionPool();
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setDriverClassName(JDBC_DRIVER);
        config.setInitialSize(10);
        config.setMaxActive(5);

        try {
            pool.init(config);
            fail("test failed on driver match");
        } catch (PoolInitializeFailedException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof BeeDataSourceConfigException);
            String message = cause.getMessage();
            Assert.assertTrue(message != null && message.contains("initialSize must not be greater than maxActive"));
        }
    }

    public void testOnEnableThreadLocal() throws Exception {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertNotNull(TestUtil.getFieldValue(pool, "threadLocal"));
        pool.close();

        config.setEnableThreadLocal(false);
        pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertNull(TestUtil.getFieldValue(pool, "threadLocal"));
        pool.close();
    }

    public void testPoolInitializeInFairModeAndCompeteMode() throws Exception {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setFairMode(true);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertEquals("fair", TestUtil.getFieldValue(pool, "poolMode"));
        pool.close();

        BeeDataSourceConfig config2 = DsConfigFactory.createDefault();
        config2.setFairMode(false);
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        Assert.assertEquals("compete", TestUtil.getFieldValue(pool2, "poolMode"));
        pool2.close();
    }

    public void testSyncCreateInitialConnections() throws Exception {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setInitialSize(1);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        pool.close();

        BeeDataSourceConfig config2 = DsConfigFactory.createDefault();
        config2.setInitialSize(1);
        config2.setPrintRuntimeLog(true);
        config2.setAsyncCreateInitConnection(true);//<--async
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        pool2.close();
    }

    public void testClearConnectionsOnInitializeFailed() throws Exception {
        FastConnectionPool pool = null;
        try {
            BeeDataSourceConfig config = DsConfigFactory.createDefault();
            config.setInitialSize(2);
            config.setRawConnectionFactory(new MockFailSizeReachConnectionFactory(1, true));
            pool = new FastConnectionPool();
            pool.init(config);
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("The count of creation has reach max size"));
            Assert.assertEquals(0, pool.getIdleSize());
        }

        pool.close();
        FastConnectionPool pool2 = null;
        try {
            BeeDataSourceConfig config2 = DsConfigFactory.createDefault();
            config2.setInitialSize(2);
            config2.setRawConnectionFactory(new MockCreateExceptionConnectionFactory(new IllegalArgumentException()));
            pool2 = new FastConnectionPool();
            pool2.init(config2);
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof ConnectionCreateException);
            Assert.assertTrue(e.getCause() instanceof IllegalArgumentException);
            Assert.assertEquals(0, pool2.getIdleSize());
        }
        pool2.close();

        FastConnectionPool pool3;
        StoneLogAppender logAppender = getStoneLogAppender();
        BeeDataSourceConfig config3 = DsConfigFactory.createDefault();
        config3.setInitialSize(2);
        config3.setRawConnectionFactory(new MockFailSizeReachConnectionFactory(1, true));
        config3.setAsyncCreateInitConnection(true);
        pool3 = new FastConnectionPool();

        logAppender.beginCollectStoneLog();
        pool3.init(config3);
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.isEmpty());
        pool3.close();
    }

    public void testCreationTimeout() throws Exception {
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
        try {
            pool.getConnection();
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof ConnectionGetTimeoutException);
            Assert.assertTrue(e.getMessage().contains("Wait timeout on pool semaphore acquisition"));
            first.interrupt();
        }
        pool.close();

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
        try {
            pool2.getConnection();
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof ConnectionCreateException);
            Assert.assertTrue(e.getMessage().contains("Wait timeout on pool lock acquisition"));
            first.interrupt();
        }
        pool2.close();
    }


    public void testCreationInterruption() throws Exception {
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
        private final FastConnectionPool pool;

        FirstGetThread(FastConnectionPool pool) {
            this.pool = pool;
            this.setDaemon(true);
        }

        public void run() {
            try {
                pool.getConnection();
            } catch (Exception e) {
                //do noting
            } finally {
                pool.close();
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
