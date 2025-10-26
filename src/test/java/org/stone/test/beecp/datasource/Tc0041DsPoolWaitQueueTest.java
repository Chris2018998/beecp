/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.test.base.TestUtil;
import org.stone.test.beecp.objects.threads.BorrowThread;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0041DsPoolWaitQueueTest {

    @Test
    public void testTimeout() throws Exception {
        //1: wait timeout on wait queue
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setMaxWait(1L);//1 Millisecond(test point)
        config.setParkTimeForRetry(0L);
        config.setSemaphoreSize(2);
        config.setUseThreadLocal(true);
        config.setForceRecycleBorrowedOnClose(true);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection ignored = ds.getConnection()) {
                BorrowThread secondBorrower = new BorrowThread(ds);
                secondBorrower.start();
                secondBorrower.join();
                Assertions.assertTrue(secondBorrower.getFailureCause().getMessage().contains("Waited timeout for a released connection"));
            }
        }
    }

    @Test
    public void testWaitInterruption() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10L));//<-- test point

        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection ignored = ds.getConnection()) {
                BorrowThread secondBorrower = new BorrowThread(ds);
                secondBorrower.start();
                if (TestUtil.waitUtilWaiting(secondBorrower)) {
                    secondBorrower.interrupt();
                    secondBorrower.join();
                    Assertions.assertTrue(secondBorrower.getFailureCause().getMessage().contains("An interruption occurred while waiting for a released connection"));
                }
            }
        }
    }

    @Test
    public void testTransferConnection() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setMaxWait(1L);//1 Millisecond(test point)
        config.setParkTimeForRetry(0L);
        config.setSemaphoreSize(2);
        config.setUseThreadLocal(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10L));//<-- test point

        try (BeeDataSource ds = new BeeDataSource(config)) {
            Connection con = ds.getConnection();
            BorrowThread secondBorrower = new BorrowThread(ds);
            secondBorrower.start();
            if (TestUtil.waitUtilWaiting(secondBorrower)) {
                Assertions.assertEquals(1, ds.getPoolMonitorVo().getTransferWaitingSize());
                con.close();
                secondBorrower.join();
                Assertions.assertNotNull(secondBorrower.getConnection());
            }
        }

        config.setUseThreadLocal(false);
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Connection con = ds.getConnection();
            BorrowThread secondBorrower = new BorrowThread(ds);
            secondBorrower.start();
            if (TestUtil.waitUtilWaiting(secondBorrower)) {
                con.close();
                secondBorrower.join();
                Assertions.assertNotNull(secondBorrower.getConnection());
            }
        }
    }

    @Test
    public void testTransferException() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setMaxWait(1L);//1 Millisecond(test point)
        config.setParkTimeForRetry(0L);
        config.setSemaphoreSize(2);
        config.setUseThreadLocal(true);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10L));//<-- test point

        try (BeeDataSource ds = new BeeDataSource(config)) {
            Connection con = ds.getConnection();
            BorrowThread secondBorrower = new BorrowThread(ds);
            secondBorrower.start();

            if (TestUtil.waitUtilWaiting(secondBorrower)) {
                ds.restart(true);
            }
        }
    }


    private static class BorrowerThread extends Thread {
        private final BeeDataSource ds;
        private Connection con;

        public BorrowerThread(BeeDataSource ds) {
            this.ds = ds;
        }

        public Connection getConnection() {
            return con;
        }

        public void run() {
            try {
                this.con = ds.getConnection();
            } catch (SQLException e) {
                //do nothing
            }
        }
    }


    private static class WaitTransferThread extends Thread {
        private final BeeDataSource ds;
        private final CountDownLatch latch;
        private Connection con;

        public WaitTransferThread(BeeDataSource ds) {
            this.ds = ds;
            this.latch = new CountDownLatch(1);
        }

        public Connection getConnection() {
            return con;
        }

        public CountDownLatch getCountDownLatch() {
            return latch;
        }

        public void run() {
            try {
                con = ds.getConnection();//get transferred connection by first
            } catch (SQLException e) {
                //not print stack info
            }

            try {
                latch.await();
            } catch (Exception e) {
            }

            try {
                con = ds.getConnection();////get transferred connection by second
            } catch (SQLException e) {
                //not print stack info
            }
        }
    }
}
