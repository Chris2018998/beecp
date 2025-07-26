/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.pool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.test.base.LogCollector;
import org.stone.test.base.TestUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;
import static org.stone.test.base.TestUtil.getFieldValue;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0091ConnectionTimeoutTest {

    @Test
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
        Assertions.assertEquals(initSize, pool.getIdleSize());
        LogCollector logCollector = LogCollector.startLogCollector();
        //pool.closeIdleTimeoutConnection();
        TestUtil.invokeMethod(pool, "closeIdleTimeoutConnection");
        Assertions.assertEquals(0, pool.getIdleSize());
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("before timed scan,idle:"));
        Assertions.assertTrue(logs.contains("after timed scan,idle:"));
        pool.close();

        //2: clear by timed task
        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        config2.setIdleTimeout(1L);
        config2.setTimerCheckInterval(50L);
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        Assertions.assertEquals(1, pool2.getIdleSize());
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));
        Assertions.assertEquals(0, pool2.getIdleSize());
        pool2.close();
    }

    @Test
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
        Assertions.assertEquals(1, vo.getIdleSize());
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));
        pool.close();
    }

    @Test
    public void testHoldTimeout() throws Exception {//pool timer clear timeout connections
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setHoldTimeout(100L);// hold and not using connection;
        config.setTimerCheckInterval(500L);

        Connection con = null;
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assertions.assertEquals(100L, getFieldValue(pool, "holdTimeoutMs"));
        Assertions.assertTrue((Boolean) getFieldValue(pool, "supportHoldTimeout"));

        try {
            con = pool.getConnection();
            Assertions.assertEquals(1, pool.getTotalSize());
            Assertions.assertEquals(1, pool.getBorrowedSize());

            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));
            Assertions.assertEquals(0, pool.getBorrowedSize());

            try {
                con.getCatalog();
                fail("must throw closed exception");
            } catch (SQLException e) {
                Assertions.assertTrue(e.getMessage().contains("No operations allowed after connection closed"));
            }
        } finally {
            oclose(con);
            pool.close();
        }
    }

    @Test
    public void testNotHoldTimeout() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setHoldTimeout(0);//default is zero,not timeout
        config.setTimerCheckInterval(500L);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assertions.assertEquals(0L, getFieldValue(pool, "holdTimeoutMs"));
        Assertions.assertFalse((Boolean) getFieldValue(pool, "supportHoldTimeout"));

        Connection con = null;
        try {
            con = pool.getConnection();
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));//first sleeping

            Assertions.assertEquals(1, pool.getTotalSize());
            Assertions.assertEquals(1, pool.getBorrowedSize());

            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));//second sleeping

            Assertions.assertEquals(1, pool.getTotalSize());
            Assertions.assertEquals(1, pool.getBorrowedSize());

            con.getCatalog();
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
//        Assertions.assertEquals(1, vo.getCreatingCount());
//        Assertions.assertEquals(0, vo.getCreatingTimeoutCount());
//
//        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2L));
//        vo = pool.getPoolMonitorVo();
//        Assertions.assertEquals(1, vo.getCreatingCount());
//        Assertions.assertEquals(1, vo.getCreatingTimeoutCount());
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
//        Assertions.assertTrue(found);
//        pool.close();
//    }
}
