/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.test.beecp.objects.threads.ParkDelayThread;

import java.sql.Connection;
import java.util.concurrent.TimeUnit;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0065ConnectionTimeoutTest {

    @Test
    public void testIdleTimeout() throws Exception {
        final int initSize = 1;
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(initSize);
        config.setMaxActive(initSize);
        config.setIdleTimeout(1L);
        config.setIntervalOfClearTimeout(100L);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertEquals(initSize, ds.getPoolMonitorVo().getIdleSize());

            ParkDelayThread delayThread = new ParkDelayThread(TimeUnit.MILLISECONDS.toNanos(500L));
            delayThread.start();
            delayThread.join();

            Assertions.assertEquals(0, ds.getPoolMonitorVo().getIdleSize());
        }
    }

    @Test
    public void testHoldTimeout() throws Exception {
        final int initSize = 1;
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(initSize);
        config.setMaxActive(initSize);
        config.setPrintRuntimeLogs(true);
        config.setIntervalOfClearTimeout(100L);

        //1: borrowed connection not be recycled
        Assertions.assertEquals(0L, config.getHoldTimeout());
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertEquals(initSize, ds.getPoolMonitorVo().getIdleSize());
            try (Connection con = ds.getConnection()) {
                Assertions.assertEquals(0, ds.getPoolMonitorVo().getIdleSize());
                Assertions.assertEquals(initSize, ds.getPoolMonitorVo().getBorrowedSize());

                ParkDelayThread delayThread = new ParkDelayThread(TimeUnit.MILLISECONDS.toNanos(500L));
                delayThread.start();
                delayThread.join();

                Assertions.assertEquals(0, ds.getPoolMonitorVo().getIdleSize());
                Assertions.assertEquals(initSize, ds.getPoolMonitorVo().getBorrowedSize());//not recycled
                Assertions.assertFalse(con.isClosed());
            }
        }

        //2:force recycle test
        config.setHoldTimeout(1L);
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertEquals(initSize, ds.getPoolMonitorVo().getIdleSize());
            try (Connection con = ds.getConnection()) {
                Assertions.assertEquals(0, ds.getPoolMonitorVo().getIdleSize());
                Assertions.assertEquals(initSize, ds.getPoolMonitorVo().getBorrowedSize());

                ParkDelayThread delayThread = new ParkDelayThread(TimeUnit.MILLISECONDS.toNanos(500L));
                delayThread.start();
                delayThread.join();

                Assertions.assertEquals(initSize, ds.getPoolMonitorVo().getIdleSize());//force recycled
                Assertions.assertTrue(con.isClosed());
            }
        }
    }
}
