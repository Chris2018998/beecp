/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.dataSource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.JdbcConfig;
import org.stone.beecp.pool.ConnectionPoolStatics;
import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Tc0035DataSourceLockTest extends TestCase {

    public void testWaitTimeoutOnDsRLock() {
        BeeDataSource ds = null;
        FirstGetThread firstThread = null;
        try {
            ds = new BeeDataSource();
            ds.setJdbcUrl(JdbcConfig.JDBC_URL);
            ds.setDriverClassName(JdbcConfig.JDBC_DRIVER);
            ds.setUsername(JdbcConfig.JDBC_USER);
            ds.setMaxWait(TimeUnit.SECONDS.toMillis(3));//timeout on wait
            ds.setPoolImplementClassName(BlockPoolImplementation.class.getName());

            CountDownLatch latch = new CountDownLatch(1);
            //first borrower thread for this case test(blocked in write lock)
            firstThread = new FirstGetThread(ds, latch);
            firstThread.start();


            Connection con = null;
            try {
                //main thread will be blocked on ds read lock
                latch.await();
                con = ds.getConnection();
            } catch (InterruptedException e) {
                //
            } catch (SQLException e) {
                Assert.assertTrue(e instanceof ConnectionGetTimeoutException);
            } finally {
                if (con != null) ConnectionPoolStatics.oclose(con);
            }
        } finally {
            if (firstThread != null) firstThread.interrupt();
            if (ds != null) ds.close();
        }
    }

    public void testInterruptionOnDsRLock() {
        BeeDataSource ds = null;
        try {
            ds = new BeeDataSource();
            ds.setJdbcUrl(JdbcConfig.JDBC_URL);
            ds.setDriverClassName(JdbcConfig.JDBC_DRIVER);
            ds.setUsername(JdbcConfig.JDBC_USER);
            ds.setPoolImplementClassName(BlockPoolImplementation.class.getName());

            CountDownLatch latch = new CountDownLatch(2);
            List<Thread> threads = new ArrayList<>(2);

            FirstGetThread firstThread = new FirstGetThread(ds, latch);
            firstThread.start();

            threads.add(firstThread);
            threads.add(Thread.currentThread());
            new DsLockInterruptedThread(threads, latch).start();


            latch.countDown();
            Connection con = null;
            try {
                con = ds.getConnection();
            } catch (SQLException e) {
                Assert.assertTrue(e instanceof ConnectionGetInterruptedException);
            } finally {
                if (con != null) ConnectionPoolStatics.oclose(con);
            }
        } finally {
            if (ds != null) ds.close();
        }
    }

    private static class FirstGetThread extends Thread {
        private final BeeDataSource ds;
        private final CountDownLatch latch;

        FirstGetThread(BeeDataSource ds, CountDownLatch latch) {
            this.ds = ds;
            this.latch = latch;
        }

        public void run() {
            try {
                latch.countDown();
                ds.getConnection();
            } catch (Exception e) {
                //do noting
            }
        }
    }

    //A mock thread to interrupt wait threads on ds-read lock
    private static class DsLockInterruptedThread extends Thread {
        private final List<Thread> threads;
        private final CountDownLatch latch;

        DsLockInterruptedThread(List<Thread> threads, CountDownLatch latch) {
            this.latch = latch;
            this.threads = threads;
        }

        public void run() {
            try {
                latch.await();
                for (Thread thread : threads)
                    thread.interrupt();
            } catch (Exception e) {
                //do nothing
            }
        }
    }
}
