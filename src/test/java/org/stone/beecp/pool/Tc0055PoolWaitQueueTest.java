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
import org.stone.beecp.objects.MockDriverConnectionFactory;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0055PoolWaitQueueTest extends TestCase {
    public void testTimeout() throws Exception {
        //enable ThreadLocal
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setParkTimeForRetry(0L);
        config.setBorrowSemaphoreSize(2);
        config.setEnableThreadLocal(true);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(1L);
        config.setConnectionFactory(new MockDriverConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: create first borrow thread to get connection
        pool.getConnection();
        //2: attempt to get connection in current thread
        try {
            pool.getConnection();
        } catch (ConnectionGetTimeoutException e) {
            Assert.assertTrue(e.getMessage().contains("Waited timeout for a released connection"));
        } finally {
            pool.close();
        }

        //disable ThreadLocal
        config = createDefault();
        config.setMaxActive(1);
        config.setParkTimeForRetry(0L);
        config.setEnableThreadLocal(false);
        config.setBorrowSemaphoreSize(2);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(1L);
        config.setConnectionFactory(new MockDriverConnectionFactory());
        pool = new FastConnectionPool();
        pool.init(config);

        //1: create first borrow thread to get connection
        pool.getConnection();
        //2: attempt to get connection in current thread
        try {
            pool.getConnection();
        } catch (ConnectionGetTimeoutException e) {
            Assert.assertTrue(e.getMessage().contains("Waited timeout for a released connection"));
        } finally {
            pool.close();
        }
    }

    public void testInterruptWaiters() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10L));
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: create two borrower thread
        pool.getConnection();

        //2: launch a borrow thread
        BorrowThread firstBorrower = new BorrowThread(pool);
        firstBorrower.start();

        //3: block the current thread
        ConcurrentLinkedQueue<?> waitQueue = (ConcurrentLinkedQueue) TestUtil.getFieldValue(pool, "waitQueue");
        while (true) {
            if (!waitQueue.isEmpty()) {
                break;
            } else {
                LockSupport.parkNanos(50L);
            }
        }
        //4: interrupt borrower in wait queue
        try {
            firstBorrower.interrupt();
            firstBorrower.join();
            Assert.assertTrue(firstBorrower.getFailureCause().getMessage().contains("An interruption occurred while waiting for a released connection"));
        } finally {
            pool.interruptConnectionCreating(false);
        }
    }
}
