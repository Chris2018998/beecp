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
import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0053PoolSemaphoreTest extends TestCase {

    public void testWaitTimeout() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(2);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(2));
        config.setConnectionFactory(new MockNetBlockConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        BorrowThread first = new BorrowThread(pool);//mock stuck in driver.getConnection()
        first.start();
        TestUtil.joinUtilWaiting(first);

        Assert.assertEquals(1, pool.getSemaphoreAcquiredSize());
        try {
            pool.getConnection();
        } catch (ConnectionGetTimeoutException e) {
            Assert.assertTrue(e.getMessage().contains("Waited timeout on pool semaphore"));
            first.interrupt();
        }
        pool.close();
    }

    public void testInterruptWaiters() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(1);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10));
        config.setConnectionFactory(new MockNetBlockConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        BorrowThread first = new BorrowThread(pool);
        first.start();
        TestUtil.joinUtilWaiting(first);
        new InterruptionAction(Thread.currentThread()).start();

        try {
            pool.getConnection();
        } catch (ConnectionGetInterruptedException e) {
            Assert.assertTrue(e.getMessage().contains("An interruption occurred while waiting on pool semaphore"));
            first.interrupt();
        }
        pool.close();
    }
}


