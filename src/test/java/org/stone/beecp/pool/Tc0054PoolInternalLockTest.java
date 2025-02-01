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
import org.stone.tools.extension.InterruptionReentrantReadWriteLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.base.TestUtil.waitUtilWaiting;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0054PoolInternalLockTest extends TestCase {

    public void testWaitTimeout() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(1L));
        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: create first thread
        BorrowThread firstBorrower = new BorrowThread(pool);
        firstBorrower.start();

        //2: attempt to get connection in current thread
        if (waitUtilWaiting(firstBorrower)) {//block 1 second in pool instance creation
            BorrowThread secondBorrower = new BorrowThread(pool);
            secondBorrower.start();
            secondBorrower.join();
            Assert.assertTrue(secondBorrower.getFailureCause().getMessage().contains("Waited timeout on pool lock"));
            firstBorrower.interrupt();
        }
    }

    public void testInterruptWaiting() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(1L));
        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: create first thread
        BorrowThread firstBorrower = new BorrowThread(pool);
        firstBorrower.start();

        //2: attempt to get connection in current thread
        if (waitUtilWaiting(firstBorrower)) {//block 1 second in pool instance creation
            InterruptionReentrantReadWriteLock lock = (InterruptionReentrantReadWriteLock) TestUtil.getFieldValue(pool, "connectionArrayInitLock");
            BorrowThread secondBorrower = new BorrowThread(pool);
            secondBorrower.start();
            while (lock.getQueueLength() == 0) {
                LockSupport.parkNanos(50L);
            }

            secondBorrower.interrupt();
            secondBorrower.join();
            Assert.assertTrue(secondBorrower.getFailureCause().getMessage().contains("An interruption occurred while waiting on pool lock"));
            firstBorrower.interrupt();
        }
    }

    public void testInterruptCreator() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
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

        //2: attempt to get connection in current thread
        if (waitUtilWaiting(firstBorrower)) {//block 1 second in pool instance creation
            InterruptionReentrantReadWriteLock lock = (InterruptionReentrantReadWriteLock) TestUtil.getFieldValue(pool, "connectionArrayInitLock");
            BorrowThread secondBorrower = new BorrowThread(pool);
            secondBorrower.start();
            while (lock.getQueueLength() == 0) {
                LockSupport.parkNanos(50L);
            }

            firstBorrower.interrupt();
            secondBorrower.join();
            Assert.assertTrue(secondBorrower.getFailureCause().getMessage().contains("Pool first connection created fail or first connection initialized fail"));
            firstBorrower.interrupt();
        }
    }
}
