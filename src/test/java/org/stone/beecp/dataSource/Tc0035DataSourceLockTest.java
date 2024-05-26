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
import java.util.concurrent.TimeUnit;

import static org.stone.base.TestUtil.joinUtilWaiting;

public class Tc0035DataSourceLockTest extends TestCase {

    public void testWaitTimeoutOnDsRLock() {
        BeeDataSource ds;
        FirstGetThread firstThread;

        ds = new BeeDataSource();
        ds.setJdbcUrl(JdbcConfig.JDBC_URL);
        ds.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        ds.setUsername(JdbcConfig.JDBC_USER);
        ds.setMaxWait(TimeUnit.SECONDS.toMillis(1));//timeout on wait
        ds.setPoolImplementClassName(BlockPoolImplementation.class.getName());

        firstThread = new FirstGetThread(ds);//first thread create pool under write-lock
        firstThread.start();

        if (joinUtilWaiting(firstThread)) {
            try {
                ds.getConnection();//second thread will be locked on read-lock
                Assert.fail("Ds Lock timeout test failed");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof ConnectionGetTimeoutException);
            }
        }
    }

    public void testInterruptionOnDsRLock() {
        BeeDataSource ds;
        ds = new BeeDataSource();
        ds.setJdbcUrl(JdbcConfig.JDBC_URL);
        ds.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        ds.setUsername(JdbcConfig.JDBC_USER);
        ds.setPoolImplementClassName(BlockPoolImplementation.class.getName());

        FirstGetThread firstThread = new FirstGetThread(ds);
        firstThread.start();

        if (joinUtilWaiting(firstThread)) {
            new DsLockInterruptedThread(Thread.currentThread()).start();

            try {
                ds.getConnection();
                Assert.fail("Ds Lock interruption test failed");
            } catch (SQLException e) {
                Assert.assertTrue(e instanceof ConnectionGetInterruptedException);
            }
        }
    }


    public void testSuccessOnRLock() {
        BeeDataSource ds;
        FirstGetThread firstThread;

        ds = new BeeDataSource();
        ds.setJdbcUrl(JdbcConfig.JDBC_URL);
        ds.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        ds.setUsername(JdbcConfig.JDBC_USER);
        ds.setMaxWait(TimeUnit.SECONDS.toMillis(2));//timeout on wait
        ds.setPoolImplementClassName(BlockPoolImplementation2.class.getName());

        //first borrower thread for this case test(blocked in write lock)
        firstThread = new FirstGetThread(ds);
        firstThread.start();

        if (joinUtilWaiting(firstThread)) {
            Connection con = null;
            try {
                con = ds.getConnection();
            } catch (Exception e) {
                //do nothing
            } finally {
                if (con != null) ConnectionPoolStatics.oclose(con);
                ds.close();
            }
        }
    }

    private static class FirstGetThread extends Thread {
        private final BeeDataSource ds;

        FirstGetThread(BeeDataSource ds) {
            this.ds = ds;
            this.setDaemon(true);
        }

        public void run() {
            try {
                ds.getConnection();
            } catch (Exception e) {
                //do noting
            } finally {
                ds.close();
            }
        }
    }

    //A mock thread to interrupt wait threads on ds-read lock
    private static class DsLockInterruptedThread extends Thread {
        private final Thread readThread;

        DsLockInterruptedThread(Thread readThread) {
            this.readThread = readThread;
        }

        public void run() {
            try {
                if (joinUtilWaiting(readThread))
                    readThread.interrupt();
            } catch (Exception e) {
                //do nothing
            }
        }
    }
}
