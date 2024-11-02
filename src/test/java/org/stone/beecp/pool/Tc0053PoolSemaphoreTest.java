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

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0053PoolSemaphoreTest extends TestCase {

    public void testWaitTimeout() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(1);
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
        Assert.assertEquals(1, pool.getSemaphoreAcquiredSize());

        //second thread wait on pool semaphore
        BorrowThread first2 = new BorrowThread(pool);
        first2.start();
        first2.join();

        //check failure exception
        try {
            SQLException failure = first2.getFailureCause();
            Assert.assertNotNull(failure);
            Assert.assertTrue(failure.getMessage().contains("Waited timeout on pool semaphore"));
        } finally {
            pool.close();
        }
    }

    public void testInterruptWaiters() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(1);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10L));
        config.setForceCloseUsingOnClear(true);

        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: first mock thread stuck in driver.getConnection()
        BorrowThread first = new BorrowThread(pool);
        first.start();
        factory.waitOnLatch();

        //2: second thread wait on pool semaphore
        InterruptionSemaphore semaphore = (InterruptionSemaphore) TestUtil.getFieldValue(pool, "semaphore");
        BorrowThread first2 = new BorrowThread(pool);
        first2.start();
        for (; ; ) {
            if (semaphore.getQueueLength() == 1) {//first2 in wait queue of semaphore
                break;
            } else {
                LockSupport.parkNanos(5L);
            }
        }

        //3: interrupt the second thread and get its failure exception to check
        first2.interrupt();
        first2.join();
        try {
            SQLException failure = first2.getFailureCause();
            Assert.assertNotNull(failure);
            Assert.assertEquals("An interruption occurred while waiting on pool semaphore", failure.getMessage());
        } finally {
            pool.close();
        }
    }
}


