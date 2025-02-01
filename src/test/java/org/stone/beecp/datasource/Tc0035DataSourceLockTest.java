/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.datasource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.objects.BorrowThread;
import org.stone.beecp.objects.InterruptionAction;
import org.stone.beecp.objects.MockBlockPoolImplementation1;
import org.stone.beecp.objects.MockBlockPoolImplementation2;

import java.util.concurrent.TimeUnit;

import static org.stone.base.TestUtil.waitUtilWaiting;
import static org.stone.beecp.config.DsConfigFactory.*;

/**
 * @author Chris Liao
 */
public class Tc0035DataSourceLockTest extends TestCase {

    public void testWaitTimeoutOnDsRLock() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setJdbcUrl(JDBC_URL);
        ds.setDriverClassName(JDBC_DRIVER);
        ds.setUsername(JDBC_USER);
        ds.setMaxWait(TimeUnit.MILLISECONDS.toMillis(500L));//timeout on wait
        ds.setPoolImplementClassName(MockBlockPoolImplementation1.class.getName());

        BorrowThread firstThread = new BorrowThread(ds);//first thread create pool under write-lock
        firstThread.start();

        if (waitUtilWaiting(firstThread)) {
            BorrowThread secondThread = new BorrowThread(ds);
            secondThread.start();
            secondThread.join();
            Assert.assertEquals("Timeout on waiting for pool ready", secondThread.getFailureCause().getMessage());
        }
    }

    public void testInterruptionOnDsRLock() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setJdbcUrl(JDBC_URL);
        ds.setDriverClassName(JDBC_DRIVER);
        ds.setUsername(JDBC_USER);
        ds.setPoolImplementClassName(MockBlockPoolImplementation1.class.getName());

        BorrowThread firstThread = new BorrowThread(ds);
        firstThread.start();

        if (waitUtilWaiting(firstThread)) {
            BorrowThread secondThread = new BorrowThread(ds);
            secondThread.start();
            new InterruptionAction(secondThread).start();
            secondThread.join();
            Assert.assertEquals("An interruption occurred while waiting for pool ready", secondThread.getFailureCause().getMessage());
        }
    }

    public void testSuccessOnRLock() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setJdbcUrl(JDBC_URL);
        ds.setDriverClassName(JDBC_DRIVER);
        ds.setUsername(JDBC_USER);
        ds.setMaxWait(TimeUnit.SECONDS.toMillis(10L));//timeout on wait
        ds.setPoolImplementClassName(MockBlockPoolImplementation2.class.getName());

        BorrowThread firstThread = new BorrowThread(ds);
        BorrowThread secondThread = new BorrowThread(ds);
        firstThread.start();

        if (waitUtilWaiting(firstThread)) {//block 1 second in pool instance creation
            secondThread.start();
            secondThread.join();
            Assert.assertNull(secondThread.getFailureCause());
            Assert.assertNotNull(secondThread.getConnection());
        }
    }

    public void testSuccessOnRLock2() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setJdbcUrl(JDBC_URL);
        ds.setDriverClassName(JDBC_DRIVER);
        ds.setUsername(JDBC_USER);
        ds.setMaxWait(TimeUnit.SECONDS.toMillis(10L));//timeout on wait
        ds.setPoolImplementClassName(MockBlockPoolImplementation2.class.getName());

        BorrowThread firstThread = new BorrowThread(ds, null, true);
        BorrowThread secondThread = new BorrowThread(ds, null, true);

        firstThread.start();
        if (waitUtilWaiting(firstThread)) {//block 1 second in pool instance creation
            secondThread.start();
            secondThread.join();
            Assert.assertNull(secondThread.getFailureCause());
            Assert.assertNotNull(secondThread.getXAConnection());
        }
    }
}
