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
import org.stone.base.StoneLogAppender;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.objects.BorrowThread;
import org.stone.beecp.objects.MockNetBlockConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.base.TestUtil.getFieldValue;
import static org.stone.base.TestUtil.getStoneLogAppender;
import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0091ConnectionTimeoutTest extends TestCase {

    public void testIdleTimeout() throws Exception {
        final int initSize = 5;
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(initSize);
        config.setMaxActive(initSize);
        config.setIdleTimeout(1000);
        config.setTimerCheckInterval(1000);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        try {
            Assert.assertEquals(initSize, pool.getTotalSize());
            Assert.assertEquals(initSize, pool.getIdleSize());
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));

            Assert.assertEquals(0, pool.getTotalSize());
            Assert.assertEquals(0, pool.getIdleSize());
        } finally {
            pool.close();
        }
    }

    public void testHoldTimeout() throws Exception {//pool timer clear timeout connections
        BeeDataSourceConfig config = createDefault();
        config.setHoldTimeout(100L);// hold and not using connection;
        config.setTimerCheckInterval(500L);// two seconds interval
        config.setParkTimeForRetry(0L);

        Connection con = null;
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertEquals(100L, getFieldValue(pool, "holdTimeoutMs"));
        Assert.assertTrue((Boolean) getFieldValue(pool, "supportHoldTimeout"));

        try {
            con = pool.getConnection();
            Assert.assertEquals(1, pool.getTotalSize());
            Assert.assertEquals(1, pool.getUsingSize());

            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            Assert.assertEquals(0, pool.getUsingSize());

            try {
                con.getCatalog();
                fail("must throw closed exception");
            } catch (SQLException e) {
                Assert.assertTrue(e.getMessage().contains("No operations allowed after connection closed"));
            }

            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
        } finally {
            oclose(con);
            pool.close();
        }
    }

    public void testNotHoldTimeout() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setHoldTimeout(0);//default is zero,not timeout
        config.setTimerCheckInterval(500L);// 500 mill-seconds interval
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertEquals(0L, getFieldValue(pool, "holdTimeoutMs"));
        Assert.assertFalse((Boolean) getFieldValue(pool, "supportHoldTimeout"));

        Connection con = null;
        try {
            con = pool.getConnection();
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(600L));//first sleeping

            Assert.assertEquals(1, pool.getTotalSize());
            Assert.assertEquals(1, pool.getUsingSize());

            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(600L));//second sleeping

            Assert.assertEquals(1, pool.getTotalSize());
            Assert.assertEquals(1, pool.getUsingSize());

            try {
                con.getCatalog();
            } catch (SQLException e) {
                Assert.assertEquals("Connection has been recycled by force", e.getMessage());
            }
        } finally {
            oclose(con);
            pool.close();
        }
    }

    public void testCreatingNotTimeout() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(1);

        long maxWait = TimeUnit.SECONDS.toMillis(1L);
        config.setMaxWait(maxWait);
        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        BorrowThread first = new BorrowThread(pool);
        first.start();
        factory.getArrivalLatch().await();

        Assert.assertEquals(1, pool.getConnectionCreatingCount());
        Assert.assertEquals(0, pool.getConnectionCreatingTimeoutCount());

        BeeConnectionPoolMonitorVo vo = pool.getPoolMonitorVo();
        Assert.assertEquals(1, vo.getCreatingCount());
        Assert.assertEquals(0, vo.getCreatingTimeoutCount());

        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2L));
        Assert.assertEquals(1, pool.getConnectionCreatingCount());
        Assert.assertEquals(1, pool.getConnectionCreatingTimeoutCount());
        vo = pool.getPoolMonitorVo();
        Assert.assertEquals(1, vo.getCreatingCount());
        Assert.assertEquals(1, vo.getCreatingTimeoutCount());

        boolean found = false;
        Thread[] threads = pool.interruptConnectionCreating(true);
        for (Thread thread : threads) {
            if (first == thread) {
                found = true;
                break;
            }
        }

        Assert.assertTrue(found);
        pool.close();
    }

    public void testIdleTimeoutClear() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setInitialSize(1);
        config.setIdleTimeout(1000L);
        config.setParkTimeForRetry(0L);
        config.setPrintRuntimeLog(true);
        config.setBorrowSemaphoreSize(1);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        BeeConnectionPoolMonitorVo vo = pool.getPoolMonitorVo();
        Assert.assertEquals(1, vo.getIdleSize());
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        TestUtil.invokeMethod2(pool, "closeIdleTimeoutConnection");
        vo = pool.getPoolMonitorVo();
        Assert.assertEquals(0, vo.getIdleSize());

        String logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains("before timed scan,idle:"));
        Assert.assertTrue(logs.contains("after timed scan,idle:"));
        pool.close();
    }
}
