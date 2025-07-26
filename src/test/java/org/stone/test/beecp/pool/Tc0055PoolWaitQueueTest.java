/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.pool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.test.base.TestUtil;
import org.stone.test.beecp.objects.BorrowThread;

import java.sql.Connection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0055PoolWaitQueueTest {
    @Test
    public void testTimeout() throws Exception {
        //1: enable ThreadLocal
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setParkTimeForRetry(0L);
        config.setBorrowSemaphoreSize(2);
        config.setEnableThreadLocal(true);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(1L);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        try (Connection ignored = pool.getConnection()) {
            BorrowThread secondBorrower = new BorrowThread(pool);
            secondBorrower.start();
            secondBorrower.join();
            Assertions.assertTrue(secondBorrower.getFailureCause().getMessage().contains("Waited timeout for a released connection"));
        } finally {
            pool.close();
        }

        //2: disable thread local
        BeeDataSourceConfig config2 = createDefault();
        config2.setMaxActive(1);
        config2.setParkTimeForRetry(0L);
        config.setEnableThreadLocal(false);
        config2.setBorrowSemaphoreSize(2);
        config2.setEnableThreadLocal(true);
        config2.setForceRecycleBorrowedOnClose(true);
        config2.setMaxWait(1L);
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        try (Connection ignored = pool2.getConnection()) {
            BorrowThread secondBorrower = new BorrowThread(pool2);
            secondBorrower.start();
            secondBorrower.join();
            Assertions.assertTrue(secondBorrower.getFailureCause().getMessage().contains("Waited timeout for a released connection"));
        } finally {
            pool2.close();
        }
    }

    @Test
    public void testInterruptWaiters() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10L));
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        try (Connection ignored = pool.getConnection()) {
            ConcurrentLinkedQueue<?> waitQueue = (ConcurrentLinkedQueue) TestUtil.getFieldValue(pool, "waitQueue");
            BorrowThread secondBorrower = new BorrowThread(pool);
            secondBorrower.start();
            while (waitQueue.isEmpty()) {
                LockSupport.parkNanos(50L);
            }
            secondBorrower.interrupt();
            secondBorrower.join();
            Assertions.assertTrue(secondBorrower.getFailureCause().getMessage().contains("An interruption occurred while waiting for a released connection"));
        } finally {
            pool.close();
        }
    }

    @Test
    public void testWaitersInQueueWhenClear() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10L));
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        try (Connection ignored = pool.getConnection()) {
            ConcurrentLinkedQueue<?> waitQueue = (ConcurrentLinkedQueue) TestUtil.getFieldValue(pool, "waitQueue");
            BorrowThread secondBorrower = new BorrowThread(pool);
            secondBorrower.start();
            while (waitQueue.isEmpty()) {
                LockSupport.parkNanos(50L);
            }

            Assertions.assertEquals(1, pool.getTransferWaitingSize());
            pool.clear(true);
            secondBorrower.join();
            Assertions.assertTrue(secondBorrower.getFailureCause().getMessage().contains("Pool has been closed or is being cleared"));
        } finally {
            pool.close();
        }
    }
}
