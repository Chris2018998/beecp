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
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.objects.BorrowThread;
import org.stone.beecp.objects.InterruptionAction;
import org.stone.beecp.objects.MockNetBlockConnectionFactory;
import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;

import java.util.concurrent.TimeUnit;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0053PoolSemaphoreTest extends TestCase {

    public void testWaitTimeout() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(1);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(1));
        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        BorrowThread first = new BorrowThread(pool);//mock stuck in driver.getConnection()
        first.start();
        factory.waitOnLatch();
        Assert.assertEquals(1, pool.getSemaphoreAcquiredSize());
        System.out.println("Tc0053PoolSemaphoreTest.testWaitTimeout: exit waitForCount");
        try {
            pool.getConnection();
        } catch (ConnectionGetTimeoutException e) {
            Assert.assertTrue(e.getMessage().contains("Waited timeout on pool semaphore"));
            first.interrupt();
        } finally {
            pool.close();
        }
    }

    public void testInterruptWaiters() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(1);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10));

        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        BorrowThread first = new BorrowThread(pool);
        first.start();
        factory.waitOnLatch();
        System.out.println("Tc0053PoolSemaphoreTest.testInterruptWaiters: exit waitForCount");

        Thread currrentThread = Thread.currentThread();
        new InterruptionAction(currrentThread).start();

        try {
            pool.getConnection();
        } catch (ConnectionGetInterruptedException e) {
            Assert.assertEquals("An interruption occurred while waiting on pool semaphore", e.getMessage());
            first.interrupt();
        } finally {
            pool.close();
        }
    }
}


