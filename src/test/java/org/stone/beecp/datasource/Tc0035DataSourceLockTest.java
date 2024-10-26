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
import org.stone.beecp.pool.ConnectionPoolStatics;
import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.stone.base.TestUtil.joinUtilWaiting;
import static org.stone.beecp.config.DsConfigFactory.*;

/**
 * @author Chris Liao
 */
public class Tc0035DataSourceLockTest extends TestCase {

    public void testWaitTimeoutOnDsRLock() throws SQLException {
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
            } catch (ConnectionGetTimeoutException e) {
                Assert.assertTrue(e.getMessage().contains("Timeout on waiting for pool ready"));
            }
        }
    }

    public void testInterruptionOnDsRLock() throws SQLException {
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
            } catch (ConnectionGetInterruptedException e) {
                Assert.assertTrue(e.getMessage().contains("An interruption occurred while waiting for pool ready"));
            }
        }
    }

    public void testSuccessOnRLock() throws SQLException {
        BeeDataSource ds;
        BorrowThread firstThread;

        ds = new BeeDataSource();
        ds.setJdbcUrl(JDBC_URL);
        ds.setDriverClassName(JDBC_DRIVER);
        ds.setUsername(JDBC_USER);
        ds.setMaxWait(TimeUnit.SECONDS.toMillis(10));//timeout on wait
        ds.setPoolImplementClassName(MockBlockPoolImplementation2.class.getName());

        firstThread = new BorrowThread(ds);
        firstThread.start();

        if (joinUtilWaiting(firstThread)) {//block 1 second in pool instance creation
            Connection con = null;
            try {
                con = ds.getConnection();
            } catch (ConnectionGetTimeoutException e) {
                fail("test failed on testSuccessOnRLock");
            } finally {
                if (con != null) ConnectionPoolStatics.oclose(con);
                ds.close();
            }
        }
    }

    public void testSuccessOnRLock2() throws SQLException {
        BeeDataSource ds;
        BorrowThread firstThread;

        ds = new BeeDataSource();
        ds.setJdbcUrl(JDBC_URL);
        ds.setDriverClassName(JDBC_DRIVER);
        ds.setUsername(JDBC_USER);
        ds.setMaxWait(TimeUnit.SECONDS.toMillis(10));//timeout on wait
        ds.setPoolImplementClassName(MockBlockPoolImplementation2.class.getName());

        firstThread = new BorrowThread(ds);
        firstThread.start();

        if (joinUtilWaiting(firstThread)) {//block 1 second in pool instance creation
            XAConnection con = null;
            try {
                con = ds.getXAConnection();
            } catch (ConnectionGetTimeoutException e) {
                fail("test failed on testSuccessOnRLock");
            } finally {
                if (con != null) ConnectionPoolStatics.oclose(con);
                ds.close();
            }
        }
    }


}
