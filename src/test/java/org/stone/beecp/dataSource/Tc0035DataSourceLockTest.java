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
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.JdbcConfig;
import org.stone.beecp.pool.ConnectionPoolStatics;
import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

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

            //first borrower thread for this case test(blocked in write lock)
            firstThread = new FirstGetThread(ds);
            firstThread.start();

            //park second ensure that first thread in blocking
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            Connection con = null;
            try {
                //main thread will be blocked on ds read lock
                con = ds.getConnection();
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

    public void testInterruptionOnDsRLock() throws Exception {
        BeeDataSource ds = null;
        FirstGetThread firstThread;
        try {
            ds = new BeeDataSource();
            ds.setJdbcUrl(JdbcConfig.JDBC_URL);
            ds.setDriverClassName(JdbcConfig.JDBC_DRIVER);
            ds.setUsername(JdbcConfig.JDBC_USER);
            ds.setPoolImplementClassName(BlockPoolImplementation.class.getName());

            //will be blocked in pool instance creation(write lock)
            firstThread = new FirstGetThread(ds);
            firstThread.start();

            //park second ensure that first thread in blocking
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1)); //ensure first getter blocked

            Object dsLock = TestUtil.getFieldValue(ds, "lock");
            Method method = dsLock.getClass().getDeclaredMethod("getQueuedReaderThreads");
            method.setAccessible(true);
            new DsLockInterruptedThread(dsLock, method).start();

            //second borrower to get connection(this thread will be blocked on ds read-lock)
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

        FirstGetThread(BeeDataSource ds) {
            this.ds = ds;
        }

        public void run() {
            try {
                ds.getConnection();
            } catch (Exception e) {
                //do noting
            }
        }
    }

    //A mock thread to interrupt wait threads on ds-read lock
    private static class DsLockInterruptedThread extends Thread {
        private final Object dsRWLock;
        private final Method getQueuedReaderThreadsMethod;

        DsLockInterruptedThread(Object dsRWLock, Method getQueuedReaderThreadsMethod) {
            this.dsRWLock = dsRWLock;
            this.getQueuedReaderThreadsMethod = getQueuedReaderThreadsMethod;
        }

        public void run() {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));//ensure the second thread in waiting on r-lock

            try {
                Object queueThreads = getQueuedReaderThreadsMethod.invoke(dsRWLock);
                if (queueThreads instanceof Collection) {
                    Collection<Thread> waitThreads = (Collection<Thread>) queueThreads;
                    for (Thread waitThread : waitThreads) {
                        waitThread.interrupt();
                    }
                }
            } catch (Exception e) {
                System.err.println(e);
                // e.printStackTrace();
            }
        }
    }
}
