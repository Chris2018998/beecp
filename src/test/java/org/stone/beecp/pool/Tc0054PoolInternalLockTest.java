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
import org.stone.beecp.objects.InterruptionAction;
import org.stone.beecp.objects.MockNetBlockConnectionFactory;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0054PoolInternalLockTest extends TestCase {

    public void testWaitTimeout() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceCloseUsingOnClear(true);
        config.setMaxWait(TimeUnit.MILLISECONDS.toMillis(1L));

        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: create first borrow thread to get connection
        new BorrowThread(pool).start();
        factory.waitOnArrivalLatch();

        //2: attempt to get connection in current thread
        try {
            pool.getConnection();
        } catch (ConnectionGetTimeoutException e) {
            Assert.assertTrue(e.getMessage().contains("Waited timeout on pool lock"));
        } finally {
            factory.getBlockingLatch().countDown();
            pool.close();
        }
    }

    public void testInterruptWaiters() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceCloseUsingOnClear(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10L));

        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: create first borrow thread to get connection
        new BorrowThread(pool).start();
        factory.waitOnArrivalLatch();

        //2: create second thread to acquire read-lock of initialization
        BorrowThread second = new BorrowThread(pool);
        second.start();

        //3: loop to wait util second thread has existed in lock wait queue
        TestUtil.blockUtilWaiter((ReentrantReadWriteLock) TestUtil.getFieldValue(pool, "connectionArrayInitLock"));

        //4: create a mock thread to interrupt first thread in blocking
        new InterruptionAction(second).start();
        //5: attempt to get connection in current thread
        second.join();

        //6: get failure exception from second
        try {
            SQLException e = second.getFailureCause();
            Assert.assertTrue(e != null && e.getMessage().contains("An interruption occurred while waiting on pool lock"));
        } finally {
            factory.getBlockingLatch().countDown();
            pool.close();
        }
    }

    public void testInterruptCreator() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceCloseUsingOnClear(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10L));

        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: create first borrow thread to get connection
        BorrowThread first = new BorrowThread(pool);
        first.start();
        factory.waitOnArrivalLatch();//first thread has entered <method>factory.create</method>

        //2: create second thread to acquire read-lock of initialization
        BorrowThread second = new BorrowThread(pool);
        second.start();

        //3: loop to wait util second thread has existed in lock wait queue
        TestUtil.blockUtilWaiter((ReentrantReadWriteLock) TestUtil.getFieldValue(pool, "connectionArrayInitLock"));

        //4: create a mock thread to interrupt first thread in blocking
        new InterruptionAction(first).start();
        //5: attempt to get connection in current thread
        second.join();

        //6: get failure exception from second
        try {
            SQLException e = second.getFailureCause();
            Assert.assertTrue(e != null && e.getMessage().contains("Waited failed on pool lock for initialization ready on first connection by another"));
        } finally {
            pool.close();
        }
    }
}
