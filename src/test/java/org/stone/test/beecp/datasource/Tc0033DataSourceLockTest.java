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
import org.stone.test.beecp.objects.pool.BlockingPoolImpl_Park;
import org.stone.test.beecp.objects.pool.BlockingPoolImpl_ParkNanos;
import org.stone.test.beecp.objects.threads.BorrowThread;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.test.base.TestUtil.waitUtilWaiting;
import static org.stone.test.beecp.config.DsConfigFactory.*;

/**
 * @author Chris Liao
 */
public class Tc0033DataSourceLockTest {

    @Test
    public void testOnReadLock() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setMaxActive(2);
            ds.setJdbcUrl(JDBC_URL);
            ds.setDriverClassName(JDBC_DRIVER);
            ds.setMaxWait(100L);//100 milliseconds
            ds.setPoolImplementClassName(BlockingPoolImpl_Park.class.getName());
            BorrowThread firstThread = new BorrowThread(ds);//first thread create pool under write-lock
            firstThread.start();

            //1: wait timeout on read lock(secondThread)
            if (waitUtilWaiting(firstThread)) {
                BorrowThread secondThread = new BorrowThread(ds);
                secondThread.start();
                secondThread.join();
                Assertions.assertEquals("Timeout on waiting for pool ready", secondThread.getFailureCause().getMessage());
            }

            //2: expect to success to get a connection(thirdThread)
            if (waitUtilWaiting(firstThread)) {
                BorrowThread thirdThread = new BorrowThread(ds);
                LockSupport.unpark(firstThread);//wakeup this first thread
                thirdThread.start();
                thirdThread.join();
                Assertions.assertNotNull(thirdThread.getConnection());

                //3: expect to get a connection
                firstThread.join();
                Assertions.assertNotNull(firstThread.getConnection());
            } else {
                firstThread.interrupt();
                Assertions.fail("[testOnReadLock]Test failed");
            }
        }
    }

    @Test
    public void testInterruptionOnLock() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setMaxActive(2);
            ds.setJdbcUrl(JDBC_URL);
            ds.setDriverClassName(JDBC_DRIVER);
            ds.setMaxWait(Long.MAX_VALUE);//ensure this value enough large
            ds.setPoolImplementClassName(BlockingPoolImpl_Park.class.getName());
            BorrowThread firstThread = new BorrowThread(ds);//first thread create pool under write-lock
            firstThread.start();

            if (waitUtilWaiting(firstThread)) {//blocking in pool
                BorrowThread secondThread = new BorrowThread(ds);
                secondThread.start();
                if (waitUtilWaiting(secondThread)) {//blocking lock
                    List<Thread> threadList = ds.interruptWaitingThreads();
                    Assertions.assertTrue(threadList.contains(firstThread));//blocking pool new
                    Assertions.assertTrue(threadList.contains(secondThread));//blocking in ds read-lock
                }

                firstThread.join();
                secondThread.join();
                Assertions.assertEquals("An interruption occurred while waiting for pool ready", secondThread.getFailureCause().getMessage());
            }
        }
    }

    @Test
    public void testDelayInPoolConstructor() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setJdbcUrl(JDBC_URL);
        ds.setDriverClassName(JDBC_DRIVER);
        ds.setUsername(JDBC_USER);
        ds.setMaxWait(TimeUnit.SECONDS.toMillis(10L));//timeout on wait
        ds.setPoolImplementClassName(BlockingPoolImpl_ParkNanos.class.getName());

        BorrowThread firstThread = new BorrowThread(ds, true);
        BorrowThread secondThread = new BorrowThread(ds, true);

        firstThread.start();
        if (waitUtilWaiting(firstThread)) {//block 1 second in pool instance creation
            secondThread.start();
            secondThread.join();
            Assertions.assertNull(secondThread.getFailureCause());
            Assertions.assertNotNull(secondThread.getXAConnection());
        }
    }
}
