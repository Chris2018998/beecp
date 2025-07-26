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
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.beecp.pool.exception.PoolInClearingException;
import org.stone.test.base.LogCollector;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0059PoolClearTest {

    @Test
    public void testClearAllIdles() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setMaxActive(2);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        Assertions.assertEquals(2, pool.getTotalSize());
        LogCollector logCollector = LogCollector.startLogCollector();
        pool.clear(false);
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("begin to remove all connections"));
        Assertions.assertTrue(logs.contains("completed to remove all connections"));
        Assertions.assertEquals(0, pool.getTotalSize());
        pool.close();

        //test clear on closed pool
        logCollector = LogCollector.startLogCollector();
        try {
            pool.clear(false);
            fail("fail to test clear");
        } catch (SQLException e) {
            Assertions.assertInstanceOf(PoolInClearingException.class, e);
            Assertions.assertEquals("Pool has been closed or is being cleared", e.getMessage());
        }
        logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.isEmpty());
    }

    @Test
    public void testForceClearUsings() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        pool.getConnection();
        Assertions.assertEquals(1, pool.getBorrowedSize());
        pool.clear(true);

        Assertions.assertEquals(0, pool.getBorrowedSize());
        Assertions.assertEquals(0, pool.getTotalSize());
    }

    @Test
    public void testClearHoldTimeout() throws SQLException {//manual clear hold timeout connections
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setHoldTimeout(500L);// hold and not using connection;
        config.setParkTimeForRetry(500L);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        pool.getConnection();
        Assertions.assertEquals(1, pool.getBorrowedSize());
        pool.clear(false);
        Assertions.assertEquals(0, pool.getBorrowedSize());
        Assertions.assertEquals(0, pool.getTotalSize());
    }

    @Test
    public void testClearUsingOnReturn() throws Exception {//manual clear hold timeout connections
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setParkTimeForRetry(500L);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        DelayCloseThread borrowThread = new DelayCloseThread(pool);
        borrowThread.start();
        borrowThread.join();
        pool.clear(false);
        Assertions.assertEquals(0, pool.getBorrowedSize());
        Assertions.assertEquals(0, pool.getTotalSize());
    }

    @Test
    public void testClearAndStartupPool() throws SQLException {
        BeeDataSourceConfig config1 = createDefault();
        config1.setInitialSize(5);
        config1.setMaxActive(5);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config1);
        Assertions.assertEquals(5, pool.getTotalSize());

        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(10);
        config2.setMaxActive(10);
        pool.clear(false, config2);
        Assertions.assertEquals(10, pool.getTotalSize());
    }

    @Test
    public void testPoolRestart() throws SQLException {
        BeeDataSourceConfig config1 = createDefault();
        config1.setInitialSize(2);
        config1.setMaxActive(2);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config1);
        Assertions.assertEquals(2, pool.getTotalSize());

        try {
            pool.clear(false, null);
            fail("failed test clear");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertEquals("Pool reinitialization configuration can't be null", e.getMessage());
        }

        BeeDataSourceConfig config2 = createDefault();
        config2.setMaxActive(5);
        config2.setInitialSize(10);
        try {
            pool.clear(false, config2);
            fail("failed test clear");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertEquals("The configured value of item 'initial-size' cannot be greater than the configured value of item 'max-active'", e.getMessage());
            config2.setMaxActive(10);
            config2.setInitialSize(10);
            pool.clear(false, config2);
            Assertions.assertEquals(10, pool.getTotalSize());
        }

        BeeDataSourceConfig config3 = createDefault();
        config3.setInitialSize(1);
        MockCommonConnectionFactory connectionFactory = new MockCommonConnectionFactory();
        connectionFactory.setCreateException1(new SQLException("Network communications error"));
        config3.setConnectionFactory(connectionFactory);
        try {
            pool.clear(false, config3);
            fail("failed test clear");
        } catch (SQLException e) {
            Assertions.assertEquals("Network communications error", e.getMessage());
            config3.setConnectionFactory(null);

            pool.clear(false, config3);
            Assertions.assertEquals(1, pool.getTotalSize());
        }
    }

    private final static class DelayCloseThread extends Thread {
        private final FastConnectionPool pool;

        public DelayCloseThread(FastConnectionPool pool) {
            this.pool = pool;
        }

        public void run() {
            try {
                Connection con = pool.getConnection();
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500L));
                con.close();
            } catch (Exception e) {
                //do nothing
            }
        }
    }
}
