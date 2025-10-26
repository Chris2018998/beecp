/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.test.base.TestUtil;
import org.stone.test.beecp.objects.factory.BlockingMockConnectionFactory;
import org.stone.test.beecp.objects.threads.BorrowThread;
import org.stone.tools.extension.InterruptionReentrantReadWriteLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.test.base.TestUtil.waitUtilWaiting;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0040DsPoolInternalLockTest {

    @Test
    public void testWaitTimeout() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(1L));
        config.setConnectionFactory(new BlockingMockConnectionFactory());

        try (BeeDataSource ds = new BeeDataSource(config)) {
            //1: create first thread
            BorrowThread firstBorrower = new BorrowThread(ds);
            firstBorrower.start();

            //2: attempt to get connection in current thread
            if (waitUtilWaiting(firstBorrower)) {//block 1 second in pool instance creation
                BorrowThread secondBorrower = new BorrowThread(ds);
                secondBorrower.start();
                secondBorrower.join();
                Assertions.assertTrue(secondBorrower.getFailureCause().getMessage().contains("Waited timeout on pool lock"));
                firstBorrower.interrupt();
            }
        }
    }

    @Test
    public void testInterruptWaiting() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(1L));
        BlockingMockConnectionFactory factory = new BlockingMockConnectionFactory();
        config.setConnectionFactory(factory);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            //1: create first thread
            BorrowThread firstBorrower = new BorrowThread(ds);
            firstBorrower.start();

            //2: attempt to get connection in current thread
            if (waitUtilWaiting(firstBorrower)) {//block 1 second in pool instance creation
                Object dsPool = TestUtil.getFieldValue(ds, "pool");
                InterruptionReentrantReadWriteLock lock = (InterruptionReentrantReadWriteLock) TestUtil.getFieldValue(dsPool, "connectionArrayInitLock");

                BorrowThread secondBorrower = new BorrowThread(ds);
                secondBorrower.start();
                while (lock.getQueueLength() == 0) {
                    LockSupport.parkNanos(50L);
                }

                secondBorrower.interrupt();
                secondBorrower.join();
                Assertions.assertTrue(secondBorrower.getFailureCause().getMessage().contains("An interruption occurred while waiting on pool lock"));
                firstBorrower.interrupt();
            }
        }
    }

    @Test
    public void testInterruptCreator() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10L));
        config.setConnectionFactory(new BlockingMockConnectionFactory());

        try (BeeDataSource ds = new BeeDataSource(config)) {

            //1: create two borrower thread
            BorrowThread firstBorrower = new BorrowThread(ds);
            firstBorrower.start();

            //2: attempt to get connection in current thread
            if (waitUtilWaiting(firstBorrower)) {//block 1 second in pool instance creation
                Object dsPool = TestUtil.getFieldValue(ds, "pool");
                InterruptionReentrantReadWriteLock lock = (InterruptionReentrantReadWriteLock) TestUtil.getFieldValue(dsPool, "connectionArrayInitLock");
                BorrowThread secondBorrower = new BorrowThread(ds);
                secondBorrower.start();
                while (lock.getQueueLength() == 0) {
                    LockSupport.parkNanos(50L);
                }

                firstBorrower.interrupt();
                secondBorrower.join();
                Assertions.assertTrue(secondBorrower.getFailureCause().getMessage().contains("Pool first connection created fail or first connection initialized fail"));
                firstBorrower.interrupt();
            }
        }
    }
}
