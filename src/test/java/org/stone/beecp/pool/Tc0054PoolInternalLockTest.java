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
import org.stone.beecp.pool.exception.ConnectionCreateException;
import org.stone.tools.extension.InterruptionReentrantReadWriteLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0054PoolInternalLockTest extends TestCase {

    public void testWaitTimeout() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(1));

        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        BorrowThread first = new BorrowThread(pool);//mock stuck in driver.getConnection()
        first.start();
        factory.waitOnLatch();
        System.out.println("Tc0054PoolInternalLockTest.testWaitTimeout: exit waitForCount");

        try {
            pool.getConnection();
        } catch (ConnectionCreateException e) {
            Assert.assertTrue(e.getMessage().contains("Waited timeout on pool lock"));
            first.interrupt();
        } finally {
            pool.close();
        }
    }

    public void testInterruptWaiters() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10));
        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        BorrowThread first = new BorrowThread(pool);
        first.start();
        factory.waitOnLatch();
        System.out.println("Tc0054PoolInternalLockTest.testInterruptWaiters: exit waitForCount");

        InterruptionReentrantReadWriteLock lock = (InterruptionReentrantReadWriteLock) TestUtil.getFieldValue(pool, "connectionArrayInitLock");
        Assert.assertTrue(lock.isWriteLocked());

        BorrowThread second = new BorrowThread(pool);
        second.start();//block on lock
        for (; ; ) {
            if (lock.getQueueLength() == 1) {
                break;
            } else {
                LockSupport.parkNanos(5L);
            }
        }

        try {
            Assert.assertEquals(1, pool.getConnectionCreatingCount());
            Thread[] threads = pool.interruptConnectionCreating(false);
            Assert.assertEquals(2, threads.length);
        } finally {
            pool.close();
        }
    }
}
