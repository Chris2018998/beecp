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
import org.stone.beecp.objects.BorrowThread;
import org.stone.beecp.objects.InterruptionAction;
import org.stone.beecp.objects.MockBlockPoolImplementation1;
import org.stone.beecp.objects.MockBlockPoolImplementation2;
import org.stone.beecp.pool.ConnectionPoolStatics;
import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.stone.base.TestUtil.joinUtilWaiting;
import static org.stone.beecp.config.DsConfigFactory.*;

/**
 * @author Chris Liao
 */
public class Tc0035DataSourceLockTest extends TestCase {

    public void testWaitTimeoutOnDsRLock() {
        BeeDataSource ds;
        BorrowThread firstThread;

        ds = new BeeDataSource();
        ds.setJdbcUrl(JDBC_URL);
        ds.setDriverClassName(JDBC_DRIVER);
        ds.setUsername(JDBC_USER);
        ds.setMaxWait(TimeUnit.SECONDS.toMillis(1));//timeout on wait
        ds.setPoolImplementClassName(MockBlockPoolImplementation1.class.getName());

        firstThread = new BorrowThread(ds);//first thread create pool under write-lock
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
        ds.setJdbcUrl(JDBC_URL);
        ds.setDriverClassName(JDBC_DRIVER);
        ds.setUsername(JDBC_USER);
        ds.setPoolImplementClassName(MockBlockPoolImplementation1.class.getName());

        BorrowThread firstThread = new BorrowThread(ds);
        firstThread.start();

        if (joinUtilWaiting(firstThread)) {
            new InterruptionAction(Thread.currentThread()).start();

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
        BorrowThread firstThread;

        ds = new BeeDataSource();
        ds.setJdbcUrl(JDBC_URL);
        ds.setDriverClassName(JDBC_DRIVER);
        ds.setUsername(JDBC_USER);
        ds.setMaxWait(TimeUnit.SECONDS.toMillis(2));//timeout on wait
        ds.setPoolImplementClassName(MockBlockPoolImplementation2.class.getName());

        //first borrower thread for this case test(blocked in write lock)
        firstThread = new BorrowThread(ds);
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
}
