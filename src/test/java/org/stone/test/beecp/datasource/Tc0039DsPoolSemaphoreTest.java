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
import org.stone.tools.extension.InterruptableSemaphore;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.test.base.TestUtil.getFieldValue;
import static org.stone.test.base.TestUtil.waitUtilWaiting;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0039DsPoolSemaphoreTest {

    @Test
    public void testWaitTimeout() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setSemaphoreSize(1);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(1L);
        BlockingMockConnectionFactory factory = new BlockingMockConnectionFactory();
        config.setConnectionFactory(factory);
        try (BeeDataSource ds = new BeeDataSource(config)) {
            //1: create first borrow thread to get connection
            BorrowThread firstBorrower = new BorrowThread(ds);
            firstBorrower.start();

            //2: attempt to get connection in current thread
            if (waitUtilWaiting(firstBorrower)) {//block 1 second in pool instance creation
                Assertions.assertEquals(0, ds.getPoolMonitorVo().getSemaphoreRemainSize());

                BorrowThread secondBorrower = new BorrowThread(ds);
                secondBorrower.start();
                secondBorrower.join();
                Assertions.assertTrue(secondBorrower.getFailureCause().getMessage().contains("Waited timeout on pool semaphore"));
                firstBorrower.interrupt();
            }
        }
    }

    @Test
    public void testInterruptWaiters() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setSemaphoreSize(1);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10L));
        BlockingMockConnectionFactory factory = new BlockingMockConnectionFactory();
        config.setConnectionFactory(factory);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            //1: create two borrower thread
            BorrowThread firstBorrower = new BorrowThread(ds);
            firstBorrower.start();
            if (waitUtilWaiting(firstBorrower)) {//block 1 second in pool instance creation
                Assertions.assertEquals(0, ds.getPoolMonitorVo().getSemaphoreRemainSize());

                Object dsPool = getFieldValue(ds, "pool");
                InterruptableSemaphore semaphore = (InterruptableSemaphore) TestUtil.getFieldValue(dsPool, "semaphore");
                BorrowThread secondBorrower = new BorrowThread(ds);
                secondBorrower.start();
                while (semaphore.getQueueLength() == 0) {
                    LockSupport.parkNanos(50L);
                }
                secondBorrower.interrupt();
                secondBorrower.join();
                Assertions.assertTrue(secondBorrower.getFailureCause().getMessage().contains("An interruption occurred while waiting on pool semaphore"));
                firstBorrower.interrupt();
            }
        }

    }
}
