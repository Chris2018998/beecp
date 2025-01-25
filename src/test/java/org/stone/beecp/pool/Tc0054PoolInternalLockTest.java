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
import org.stone.tools.extension.InterruptionReentrantReadWriteLock;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0054PoolInternalLockTest extends TestCase {

    public void testWaitTimeout() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.MILLISECONDS.toMillis(1L));

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
            Assert.assertTrue(e.getMessage().contains("Waited timeout on pool lock"));
        } finally {
            factory.interruptAll();
            pool.close();
        }
    }

    public void testInterruptWaiters() throws Exception {
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
        BorrowThread secondBorrower = new BorrowThread(pool);
        firstBorrower.start();
        secondBorrower.start();

        //2: block the current thread
        factory.waitUtilCreationArrival();

        //3: interrupt waiter thread on lock
        InterruptionReentrantReadWriteLock lock = (InterruptionReentrantReadWriteLock) TestUtil.getFieldValue(pool, "connectionArrayInitLock");
        try {
            List<Thread> interruptedThreads = lock.interruptQueuedWaitThreads();
            for (Thread thread : interruptedThreads)
                thread.join();
            for (Thread thread : interruptedThreads) {
                if (thread == firstBorrower) {
                    Assert.assertTrue(firstBorrower.getFailureCause().getMessage().contains("An interruption occurred while waiting on pool lock"));
                }
                if (thread == secondBorrower) {
                    Assert.assertTrue(secondBorrower.getFailureCause().getMessage().contains("An interruption occurred while waiting on pool lock"));
                }
            }
        } finally {
            factory.interruptAll();
            pool.interruptConnectionCreating(false);
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
        BorrowThread secondBorrower = new BorrowThread(pool);
        firstBorrower.start();
        secondBorrower.start();

        //2: block the current thread
        System.out.println("block the current thread on waitUtilCreationArrival111");
        factory.waitUtilCreationArrival();
        System.out.println("block the current thread on waitUtilCreationArrival222");

        //3: block the current thread
        Thread.interrupted();//just clean interruption flag
        InterruptionReentrantReadWriteLock lock = (InterruptionReentrantReadWriteLock) TestUtil.getFieldValue(pool, "connectionArrayInitLock");
        do {
            if (lock.getQueueLength()==0)
                LockSupport.parkNanos(50L);
            else
                break;
        } while (true);

        System.out.println("block the current thread on InterruptionReentrantReadWriteLock");

        try {
            //3: interrupt lock owner
            Thread ownerThread = lock.getOwnerThread();
            if (ownerThread != null) {
                factory.interrupt(ownerThread);

                firstBorrower.join();
                secondBorrower.join();
                if (ownerThread == firstBorrower) {
                    Assert.assertTrue(firstBorrower.getFailureCause().getMessage().contains("A unknown error occurred when created a connection"));
                } else {
                    Assert.assertTrue(firstBorrower.getFailureCause().getMessage().contains("Pool first connection created fail or first connection initialized fail"));
                }

                if (ownerThread == secondBorrower) {
                    Assert.assertTrue(secondBorrower.getFailureCause().getMessage().contains("A unknown error occurred when created a connection"));
                } else {
                    Assert.assertTrue(secondBorrower.getFailureCause().getMessage().contains("Pool first connection created fail or first connection initialized fail"));
                }
            } else {
                System.out.println("lock owner thread is null");
            }
        } finally {
            factory.interruptAll();
            pool.interruptConnectionCreating(false);
        }
    }
}
