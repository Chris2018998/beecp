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
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.objects.BorrowThread;
import org.stone.beecp.objects.MockNetBlockConnectionFactory;
import org.stone.beecp.objects.MockNetBlockXaConnectionFactory;
import org.stone.beecp.pool.exception.ConnectionGetForbiddenException;
import org.stone.beecp.pool.exception.PoolInitializeFailedException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.base.TestUtil.waitUtilWaiting;
import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

/**
 * @author Chris Liao
 */
public class Tc0063RawConnectionPoolTest extends TestCase {

    public void testInitialization() throws Exception {
        try {
            new RawConnectionPool().init(null);
            fail("Test failed to check null config");
        } catch (PoolInitializeFailedException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Pool initialization configuration can't be null"));
        }

        BeeDataSourceConfig config2 = createDefault();
        RawConnectionPool pool2 = new RawConnectionPool();
        pool2.init(config2);
        BeeConnectionPoolMonitorVo vo2 = pool2.getPoolMonitorVo();
        Assert.assertEquals("compete", vo2.getPoolMode());

        BeeDataSourceConfig config3 = createDefault();
        config3.setFairMode(true);
        RawConnectionPool pool3 = new RawConnectionPool();
        pool3.init(config3);
        BeeConnectionPoolMonitorVo vo3 = pool3.getPoolMonitorVo();
        Assert.assertEquals("fair", vo3.getPoolMode());
        pool3.clear(false);
        pool3.clear(false, null);
        pool3.setPrintRuntimeLog(false);
        pool3.recycle(null);
        Assert.assertEquals(0, pool3.getTotalSize());
        Assert.assertEquals(0, pool3.getIdleSize());
        Assert.assertEquals(0, pool3.getBorrowedSize());
        Assert.assertEquals(0, pool3.getBorrowedSize());
        Assert.assertEquals(0, pool3.getSemaphoreAcquiredSize());
        Assert.assertEquals(0, pool3.getSemaphoreWaitingSize());
        Assert.assertEquals(0, pool3.getTransferWaitingSize());
    }

    public void testPoolClose() throws Exception {
        BeeDataSourceConfig config = createDefault();
        RawConnectionPool pool = new RawConnectionPool();
        pool.init(config);

        long timePoint = System.nanoTime() + TimeUnit.SECONDS.toNanos(1L);

        Assert.assertFalse(pool.isClosed());
        CloseThread thread1 = new CloseThread(pool, timePoint);
        CloseThread thread2 = new CloseThread(pool, timePoint);
        CloseThread thread3 = new CloseThread(pool, timePoint);
        thread1.start();
        thread2.start();
        thread3.start();
        thread1.join();
        thread2.join();
        thread3.join();

        Assert.assertTrue(pool.isClosed());
        try {
            pool.getConnection();
        } catch (ConnectionGetForbiddenException e) {
            Assert.assertEquals("Access forbidden,connection pool was closed or in clearing", e.getMessage());
        }
        try {
            pool.getXAConnection();
        } catch (ConnectionGetForbiddenException e) {
            Assert.assertEquals("Access forbidden,connection pool was closed or in clearing", e.getMessage());
        }

    }

    public void testGetConnection() throws Exception {
        BeeDataSourceConfig config1 = createDefault();
        config1.setConnectionFactoryClassName("org.stone.beecp.driver.MockDataSource");
        RawConnectionPool pool1 = new RawConnectionPool();
        pool1.init(config1);

        Connection con1 = null;
        Connection con2 = null;
        try {
            con1 = pool1.getConnection();
            con2 = pool1.getConnection("root", "root");
            Assert.assertNotNull(con1);
            Assert.assertNotNull(con2);
        } finally {
            oclose(con1);
            oclose(con2);
            con1 = null;
            con2 = null;
        }

        try {
            pool1.getXAConnection();
        } catch (SQLException e) {
            Assert.assertEquals("Not support", e.getMessage());
        }

        BeeDataSourceConfig config2 = createDefault();
        config2.setConnectionFactoryClassName("org.stone.beecp.driver.MockXaDataSource");
        RawConnectionPool pool2 = new RawConnectionPool();
        pool2.init(config2);
        try {
            con1 = pool2.getConnection();
            con2 = pool2.getConnection("root", "root");
            Assert.assertNotNull(con1);
            Assert.assertNotNull(con2);
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }

    public void testTimeoutOnSemaphore() throws Exception {
        BeeDataSourceConfig config1 = createDefault();
        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config1.setConnectionFactory(factory);
        RawConnectionPool pool1 = new RawConnectionPool();
        config1.setBorrowSemaphoreSize(1);
        config1.setMaxWait(1000L);
        pool1.init(config1);

        //1: timeout for create connection
        BorrowThread firstBorrower = new BorrowThread(pool1);
        firstBorrower.start();
        if (waitUtilWaiting(firstBorrower)) {//block 1 second in pool instance creation
            Assert.assertEquals(1, pool1.getSemaphoreAcquiredSize());
            BorrowThread secondBorrower = new BorrowThread(pool1);
            secondBorrower.start();
            secondBorrower.join();
            Assert.assertTrue(secondBorrower.getFailureCause().getMessage().contains("Waited timeout on pool semaphore"));
            firstBorrower.interrupt();
        }

        //2: timeout for create XAConnection
        BeeDataSourceConfig config2 = createDefault();
        MockNetBlockXaConnectionFactory factory2 = new MockNetBlockXaConnectionFactory();
        config2.setXaConnectionFactory(factory2);
        config2.setBorrowSemaphoreSize(1);
        config2.setMaxWait(1000L);
        RawConnectionPool pool2 = new RawConnectionPool();
        pool2.init(config2);
        BorrowThread firstBorrower2 = new BorrowThread(null, pool2, true);
        firstBorrower2.start();
        if (waitUtilWaiting(firstBorrower2)) {//block 1 second in pool instance creation
            Assert.assertEquals(1, pool2.getSemaphoreAcquiredSize());
            BorrowThread secondBorrower = new BorrowThread(null, pool2, true);
            secondBorrower.start();
            secondBorrower.join();
            Assert.assertTrue(secondBorrower.getFailureCause().getMessage().contains("Waited timeout on pool semaphore"));
            firstBorrower2.interrupt();
        }
    }

    public void testInterruptionOnSemaphore() throws Exception {
        BeeDataSourceConfig config1 = createDefault();
        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config1.setConnectionFactory(factory);
        RawConnectionPool pool1 = new RawConnectionPool();
        config1.setBorrowSemaphoreSize(1);
        config1.setMaxWait(Long.MAX_VALUE);
        pool1.init(config1);

        //1: timeout for create connection
        BorrowThread firstBorrower = new BorrowThread(pool1);
        firstBorrower.start();
        if (waitUtilWaiting(firstBorrower)) {//block 1 second in pool instance creation
            Assert.assertEquals(1, pool1.getSemaphoreAcquiredSize());
            BorrowThread secondBorrower = new BorrowThread(pool1);
            secondBorrower.start();
            if (waitUtilWaiting(secondBorrower)) {
                secondBorrower.interrupt();
                secondBorrower.join();
                Assert.assertEquals(0, pool1.getConnectionCreatingCount());
                Assert.assertEquals(0, pool1.getConnectionCreatingTimeoutCount());
                Assert.assertTrue(secondBorrower.getFailureCause().getMessage().contains("An interruption occurred while waiting on pool semaphore"));
            }
            firstBorrower.interrupt();
        }

        //2: timeout for create XAConnection
        BeeDataSourceConfig config2 = createDefault();
        MockNetBlockXaConnectionFactory factory2 = new MockNetBlockXaConnectionFactory();
        config2.setXaConnectionFactory(factory2);
        config2.setBorrowSemaphoreSize(1);
        config2.setMaxWait(Long.MAX_VALUE);
        RawConnectionPool pool2 = new RawConnectionPool();
        pool2.init(config2);
        BorrowThread firstBorrower2 = new BorrowThread(null, pool2, true);
        firstBorrower2.start();
        if (waitUtilWaiting(firstBorrower2)) {//block 1 second in pool instance creation
            Assert.assertEquals(1, pool2.getSemaphoreAcquiredSize());
            BorrowThread secondBorrower = new BorrowThread(null, pool2, true);
            secondBorrower.start();
            if (waitUtilWaiting(secondBorrower)) {
                secondBorrower.interrupt();
                secondBorrower.join();
                Assert.assertEquals(0, pool1.getConnectionCreatingCount());
                Assert.assertEquals(0, pool1.getConnectionCreatingTimeoutCount());
                Assert.assertTrue(secondBorrower.getFailureCause().getMessage().contains("An interruption occurred while waiting on pool semaphore"));
            }
            firstBorrower.interrupt();
        }
    }

    private static class CloseThread extends Thread {
        private final RawConnectionPool pool;
        private final long timePoint;

        public CloseThread(RawConnectionPool pool, long timePoint) {
            this.pool = pool;
            this.timePoint = timePoint;
        }

        public void run() {
            LockSupport.parkNanos(timePoint - System.nanoTime());
            pool.close();
        }
    }
}
