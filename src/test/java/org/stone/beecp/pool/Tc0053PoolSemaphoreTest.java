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
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.objects.BorrowThread;
import org.stone.beecp.objects.MockNetBlockConnectionFactory;
import org.stone.tools.extension.InterruptionSemaphore;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.base.TestUtil.waitUtilWaiting;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0053PoolSemaphoreTest extends TestCase {

    public void testWaitTimeout() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(1);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(1L);
        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: create first borrow thread to get connection
        BorrowThread firstBorrower = new BorrowThread(pool);
        firstBorrower.start();

        //2: attempt to get connection in current thread
        if (waitUtilWaiting(firstBorrower)) {//block 1 second in pool instance creation
            Assert.assertEquals(1, pool.getSemaphoreAcquiredSize());

            BorrowThread secondBorrower = new BorrowThread(pool);
            secondBorrower.start();
            secondBorrower.join();
            Assert.assertTrue(secondBorrower.getFailureCause().getMessage().contains("Waited timeout on pool semaphore"));
            firstBorrower.interrupt();
        }
    }

    public void testInterruptWaiters() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(1);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10L));
        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: create two borrower thread
        BorrowThread firstBorrower = new BorrowThread(pool);
        firstBorrower.start();

        if (waitUtilWaiting(firstBorrower)) {//block 1 second in pool instance creation
            Assert.assertEquals(1, pool.getSemaphoreAcquiredSize());
            InterruptionSemaphore semaphore = (InterruptionSemaphore) TestUtil.getFieldValue(pool, "semaphore");
            BorrowThread secondBorrower = new BorrowThread(pool);
            secondBorrower.start();
            while (semaphore.getQueueLength() == 0) {
                LockSupport.parkNanos(50L);
            }
            secondBorrower.interrupt();
            secondBorrower.join();
            Assert.assertTrue(secondBorrower.getFailureCause().getMessage().contains("An interruption occurred while waiting on pool semaphore"));
            firstBorrower.interrupt();
        }
    }
}


