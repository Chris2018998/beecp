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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0055PoolWaitQueueTest extends TestCase {

    public void testTimeout() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(2);
        config.setForceCloseUsingOnClear(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(2));
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //current thread take out a connection
        Connection con = pool.getConnection();

        //launch a borrow thread to get connection
        BorrowThread first = new BorrowThread(pool);
        first.start();
        TestUtil.waitUtilAlive(first);
        first.join();

        try {
            SQLException failure = first.getFailureCause();
            Assert.assertTrue(failure.getMessage().contains("Waited timeout for a released connection"));
        } finally {
            if (con != null) TestUtil.oclose(con);
            pool.close();
        }
    }

    public void testTimeoutOnThreadLocalDisabled() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setEnableThreadLocal(false);
        config.setBorrowSemaphoreSize(2);
        config.setForceCloseUsingOnClear(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(2));
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //current thread take out a connection
        Connection con = pool.getConnection();

        //launch a borrow thread to get connection
        BorrowThread first = new BorrowThread(pool);
        first.start();
        TestUtil.waitUtilAlive(first);

        first.join();
        try {
            SQLException failure = first.getFailureCause();
            Assert.assertTrue(failure.getMessage().contains("Waited timeout for a released connection"));
        } finally {
            if (con != null) TestUtil.oclose(con);
            pool.close();
        }
    }

    public void testInterruptWaiters() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setForceCloseUsingOnClear(true);
        config.setBorrowSemaphoreSize(2);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10));
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //current thread take out a connection
        pool.getConnection();

        //launch a borrow thread to get connection
        ConcurrentLinkedQueue waitQueue = (ConcurrentLinkedQueue) TestUtil.getFieldValue(pool, "waitQueue");
        BorrowThread first = new BorrowThread(pool);
        first.start();
        for (; ; ) {
            if (waitQueue.size() == 1) {//first in wait queue
                break;
            } else {
                LockSupport.parkNanos(5L);
            }
        }

        //3: interrupt the second thread and get its failure exception to check
        first.interrupt();
        TestUtil.waitUtilTerminated(first);
        try {
            SQLException failure = first.getFailureCause();
            Assert.assertNotNull(failure);
            Assert.assertEquals("An interruption occurred while waiting for a released connection", failure.getMessage());
        } finally {
            pool.close();
        }
    }
}
