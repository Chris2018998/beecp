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
import org.stone.beecp.objects.MockDriverConnectionFactory;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0055PoolWaitQueueTest extends TestCase {

    public void testTimeout() throws Exception {
        //enable ThreadLocal
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setParkTimeForRetry(0L);
        config.setBorrowSemaphoreSize(2);
        config.setEnableThreadLocal(true);
        config.setForceCloseUsingOnClear(true);
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
        config.setForceCloseUsingOnClear(true);
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
        config.setForceCloseUsingOnClear(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10L));
        config.setConnectionFactory(new MockDriverConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: create first borrow thread to get connection
        pool.getConnection();

        //2: create a thread to get connection
        BorrowThread second = new BorrowThread(pool);
        second.start();

        //3: attempt to get connection in current thread
        TestUtil.blockUtilWaiter((ConcurrentLinkedQueue) TestUtil.getFieldValue(pool, "waitQueue"));

        //4: create a mock thread to interrupt first thread in blocking
        new InterruptionAction(second).start();
        //5: attempt to get connection in current thread
        second.join();

        //6: get failure exception from second
        try {
            SQLException e = second.getFailureCause();
            Assert.assertTrue(e != null && e.getMessage().contains("An interruption occurred while waiting for a released connection"));
        } finally {
            pool.close();
        }
    }
}
