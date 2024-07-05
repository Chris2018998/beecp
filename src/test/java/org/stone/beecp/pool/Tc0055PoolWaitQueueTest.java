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
import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;
import static org.stone.tools.BeanUtil.setAccessible;

public class Tc0055PoolWaitQueueTest extends TestCase {

    public void testTimeout() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setForceCloseUsingOnClear(true);
        config.setBorrowSemaphoreSize(2);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(2));
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        BorrowThread first = new BorrowThread(pool);
        first.start();
        first.join();

        try {
            pool.getConnection();
        } catch (ConnectionGetTimeoutException e) {
            Assert.assertTrue(e.getMessage().contains("Wait timeout for a released connection"));
        }

        Method createMethod = FastConnectionPool.class.getDeclaredMethod("createPooledConn", Integer.TYPE);
        setAccessible(createMethod);
        Assert.assertNull(createMethod.invoke(pool, 1));
        oclose(first.getConnection());
        pool.close();
    }

    public void testInterruptWaiters() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setForceCloseUsingOnClear(true);
        config.setBorrowSemaphoreSize(2);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10));
        FastConnectionPool pool3 = new FastConnectionPool();
        pool3.init(config);
        BorrowThread first = new BorrowThread(pool3);
        first.start();

        TestUtil.joinUtilWaiting(first);
        new InterruptionAction(Thread.currentThread()).start();//main thread will be blocked on pool lock

        try {
            pool3.getConnection();
        } catch (ConnectionGetInterruptedException e) {
            Assert.assertTrue(e.getMessage().contains("An interruption occurred while waiting for a released connection"));
        }
    }
}
