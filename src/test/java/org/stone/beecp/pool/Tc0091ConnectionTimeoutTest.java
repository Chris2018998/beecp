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
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSourceConfig;

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
        config.setIdleTimeout(1L);
        config.setPrintRuntimeLog(true);
        config.setTimerCheckInterval(5000L);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: logs print test
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));
        Assert.assertEquals(initSize, pool.getIdleSize());
        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        pool.closeIdleTimeoutConnection();
        Assert.assertEquals(0, pool.getIdleSize());
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains("before timed scan,idle:"));
        Assert.assertTrue(logs.contains("after timed scan,idle:"));
        pool.close();

        //2: clear by timed task
        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        config2.setIdleTimeout(1L);
        config2.setTimerCheckInterval(50L);
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        Assert.assertEquals(1, pool2.getIdleSize());
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));
        Assert.assertEquals(0, pool2.getIdleSize());
        pool2.close();
    }

    public void testIdleTimeoutClear() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setInitialSize(1);
        config.setIdleTimeout(50L);
        config.setTimerCheckInterval(50L);
        config.setPrintRuntimeLog(true);
        config.setBorrowSemaphoreSize(1);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        BeeConnectionPoolMonitorVo vo = pool.getPoolMonitorVo();
        Assert.assertEquals(1, vo.getIdleSize());
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));
        pool.close();
    }


    public void testHoldTimeout() throws Exception {//pool timer clear timeout connections
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setHoldTimeout(100L);// hold and not using connection;
        config.setTimerCheckInterval(500L);

        Connection con = null;
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertEquals(100L, getFieldValue(pool, "holdTimeoutMs"));
        Assert.assertTrue((Boolean) getFieldValue(pool, "supportHoldTimeout"));

        try {
            con = pool.getConnection();
            Assert.assertEquals(1, pool.getTotalSize());
            Assert.assertEquals(1, pool.getBorrowedSize());

            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));
            Assert.assertEquals(0, pool.getBorrowedSize());

            try {
                con.getCatalog();
                fail("must throw closed exception");
            } catch (SQLException e) {
                Assert.assertTrue(e.getMessage().contains("No operations allowed after connection closed"));
            }
        } finally {
            oclose(con);
            pool.close();
        }
    }

    public void testNotHoldTimeout() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setHoldTimeout(0);//default is zero,not timeout
        config.setTimerCheckInterval(500L);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertEquals(0L, getFieldValue(pool, "holdTimeoutMs"));
        Assert.assertFalse((Boolean) getFieldValue(pool, "supportHoldTimeout"));

        Connection con = null;
        try {
            con = pool.getConnection();
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));//first sleeping

            Assert.assertEquals(1, pool.getTotalSize());
            Assert.assertEquals(1, pool.getBorrowedSize());

            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));//second sleeping

            Assert.assertEquals(1, pool.getTotalSize());
            Assert.assertEquals(1, pool.getBorrowedSize());

            try {
                con.getCatalog();
            } catch (SQLException e) {
                Assert.assertEquals(e.getMessage(), "Connection has been recycled by force");
            }
        } finally {
            oclose(con);
            pool.close();
        }
    }

//    public void testCreatingNotTimeout() throws Exception {
//        BeeDataSourceConfig config = createDefault();
//        config.setMaxActive(1);
//        config.setBorrowSemaphoreSize(1);
//
//        long maxWait = TimeUnit.SECONDS.toMillis(1L);
//        config.setMaxWait(maxWait);
//        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
//        config.setConnectionFactory(factory);
//        FastConnectionPool pool = new FastConnectionPool();
//        pool.init(config);
//
//        BorrowThread first = new BorrowThread(pool);
//        first.start();
//        factory.waitUtilCreatorArrival();
//
//        BeeConnectionPoolMonitorVo vo = pool.getPoolMonitorVo();
//        Assert.assertEquals(1, vo.getCreatingCount());
//        Assert.assertEquals(0, vo.getCreatingTimeoutCount());
//
//        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2L));
//        vo = pool.getPoolMonitorVo();
//        Assert.assertEquals(1, vo.getCreatingCount());
//        Assert.assertEquals(1, vo.getCreatingTimeoutCount());
//
//        boolean found = false;
//        Thread[] threads = pool.interruptConnectionCreating(true);
//        for (Thread thread : threads) {
//            if (first == thread) {
//                found = true;
//                break;
//            }
//        }
//
//        Assert.assertTrue(found);
//        pool.close();
//    }
}
