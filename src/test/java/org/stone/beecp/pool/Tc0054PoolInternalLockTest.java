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
import org.stone.beecp.pool.exception.ConnectionCreateException;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0054PoolInternalLockTest extends TestCase {

    public void testWaitTimeout() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(2));
        config.setConnectionFactory(new MockNetBlockConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        BorrowThread first = new BorrowThread(pool);//mock stuck in driver.getConnection()
        first.start();
        TestUtil.joinUtilWaiting(first);

        try {
            pool.getConnection();
        } catch (ConnectionCreateException e) {
            Assert.assertTrue(e.getMessage().contains("Wait timeout on pool lock acquisition"));
            first.interrupt();
        }
        pool.close();
    }

    public void testInterruptWaiters() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10));
        config.setConnectionFactory(new MockNetBlockConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        BorrowThread first = new BorrowThread(pool);
        first.start();
        TestUtil.joinUtilWaiting(first);
        new InterruptionAction(Thread.currentThread()).start();//main thread will be blocked on pool lock

        try {
            pool.getConnection();
        } catch (ConnectionCreateException e) {
            Assert.assertTrue(e.getMessage().contains("An interruption occurred on pool lock acquisition"));
            first.interrupt();
        }
        pool.close();
    }

    public void testInterruptWaitersAndLockOwner() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(2));
        config.setTimerCheckInterval(TimeUnit.SECONDS.toMillis(3));//internal thread to interrupt waiters
        config.setConnectionFactory(new MockNetBlockConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        BorrowThread first = new BorrowThread(pool);
        first.start();
        TestUtil.joinUtilWaiting(first);

        Assert.assertTrue(pool.getCreatingTime() > 0);
        Assert.assertFalse(pool.isCreatingTimeout());
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
        Assert.assertTrue(pool.isCreatingTimeout());

        try {
            pool.getConnection();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("An interruption occurred on pool lock acquisition"));
        }
        pool.close();
    }
}
