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

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0054PoolInternalLockTest extends TestCase {

    public void testWaitTimeout() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        config.setForceCloseUsingOnClear(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(1));

        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //first mock thread stuck in driver.getConnection()
        BorrowThread first = new BorrowThread(pool);
        first.start();
        factory.waitOnLatch();

        //second thread wait on pool read-lock
        BorrowThread first2 = new BorrowThread(pool);
        first2.start();
        TestUtil.waitUtilAlive(first2);
        first2.join();

        //check failure exception
        try {
            SQLException failure = first2.getFailureCause();
            Assert.assertNotNull(failure);
            Assert.assertTrue(failure.getMessage().contains("Waited timeout on pool lock"));
        } finally {
            pool.close();
        }
    }

    public void testInterruptWaiters() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        config.setForceCloseUsingOnClear(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10L));
        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: first mock thread stuck in driver.getConnection()
        BorrowThread first = new BorrowThread(pool);
        first.start();
        factory.waitOnLatch();
        Assert.assertEquals(1, pool.getConnectionCreatingCount());

        //2: second thread wait on pool lock
        InterruptionReentrantReadWriteLock lock = (InterruptionReentrantReadWriteLock) TestUtil.getFieldValue(pool, "connectionArrayInitLock");
        BorrowThread first2 = new BorrowThread(pool);
        first2.start();
        for (; ; ) {
            if (lock.getQueueLength() == 1) {
                break;
            } else {
                LockSupport.parkNanos(5L);
            }
        }

        //3: interrupt connection creating thread and waiting thread on lock
        try {
            Thread[] threads = pool.interruptConnectionCreating(false);
            Assert.assertEquals(2, threads.length);
        } finally {
            pool.close();
        }
    }
}
