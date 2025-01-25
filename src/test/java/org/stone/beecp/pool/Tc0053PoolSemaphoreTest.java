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
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;
import org.stone.tools.extension.InterruptionSemaphore;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
        new BorrowThread(pool).start();
        factory.waitUtilCreationArrival();

        //2: attempt to get connection in current thread
        try {
            pool.getConnection();
        } catch (ConnectionGetTimeoutException e) {
            Assert.assertTrue(e.getMessage().contains("Waited timeout on pool semaphore"));
        } finally {
            factory.interruptAll();
            pool.close();
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
        BorrowThread secondBorrower = new BorrowThread(pool);
        firstBorrower.start();
        secondBorrower.start();

        //2: block the current thread
        factory.waitUtilCreationArrival();

        //3: interrupt waiter thread on semaphore
        InterruptionSemaphore semaphore = (InterruptionSemaphore) TestUtil.getFieldValue(pool, "semaphore");
        try {
            List<Thread> interruptedThreads = semaphore.interruptQueuedWaitThreads();
            for (Thread thread : interruptedThreads)
                thread.join();
            for (Thread thread : interruptedThreads) {
                if (thread == firstBorrower) {
                    Assert.assertTrue(firstBorrower.getFailureCause().getMessage().contains("An interruption occurred while waiting on pool semaphore"));
                }
                if (thread == secondBorrower) {
                    Assert.assertTrue(secondBorrower.getFailureCause().getMessage().contains("An interruption occurred while waiting on pool semaphore"));
                }
            }
        } finally {
            factory.interruptAll();
            pool.interruptConnectionCreating(false);
        }
    }
}


