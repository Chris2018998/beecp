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
import org.stone.beecp.exception.ConnectionCreatedException;
import org.stone.beecp.exception.ConnectionGetInterruptedException;
import org.stone.beecp.exception.XaConnectionCreatedException;
import org.stone.test.beecp.objects.factory.ExceptionConnectionFactory;
import org.stone.test.beecp.objects.factory.ExceptionXaConnectionFactory;
import org.stone.test.beecp.objects.factory.NullConnectionFactory;
import org.stone.test.beecp.objects.factory.NullXaConnectionFactory;
import org.stone.test.beecp.objects.threads.BorrowThread;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.test.base.TestUtil.waitUtilWaiting;

/**
 * @author Chris Liao
 */
public class Tc0061ConnectionCreationTest {

    @Test
    public void testConnectionCreateException() {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setConnectionFactory(new NullConnectionFactory());
            //1: fail to create connection
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("[testConnectionCreateException]Test failed");
            } catch (SQLException e) {
                Assertions.assertInstanceOf(ConnectionCreatedException.class, e);
                Assertions.assertEquals("A unknown error occurred when created a connection", e.getMessage());
            }
        }

        //2: fail to create xa-connection
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setXaConnectionFactory(new NullXaConnectionFactory());
            try {
                XAConnection ignored = ds.getXAConnection();
                Assertions.fail("[testConnectionCreateException]Test failed");
            } catch (SQLException e) {
                Assertions.assertInstanceOf(XaConnectionCreatedException.class, e);
                Assertions.assertEquals("A unknown error occurred when created an XA connection", e.getMessage());
            }
        }
    }

    @Test
    public void testConnectionGetInterruptedException() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setConnectionFactory(new NullConnectionFactory(true));
            BorrowThread borrowThread = new BorrowThread(ds);
            borrowThread.start();
            if (waitUtilWaiting(borrowThread)) {
                ds.interruptWaitingThreads();
            }
            borrowThread.join();
            Assertions.assertNotNull(borrowThread.getFailureCause());
            Assertions.assertInstanceOf(ConnectionGetInterruptedException.class, borrowThread.getFailureCause());
            Assertions.assertEquals("An interruption occurred when created a connection", borrowThread.getFailureCause().getMessage());
        }

        //2: fail to create xa-connection
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setXaConnectionFactory(new NullXaConnectionFactory(true));
            BorrowThread borrowThread = new BorrowThread(ds, null, true);
            borrowThread.start();
            if (waitUtilWaiting(borrowThread)) {
                ds.interruptWaitingThreads();
            }
            borrowThread.join();
            Assertions.assertNotNull(borrowThread.getFailureCause());
            Assertions.assertInstanceOf(ConnectionGetInterruptedException.class, borrowThread.getFailureCause());
            Assertions.assertEquals("An interruption occurred when created an XA connection", borrowThread.getFailureCause().getMessage());
        }
    }

    @Test
    public void testOtherException() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ExceptionConnectionFactory exceptionConFactory = new ExceptionConnectionFactory();
            ds.setConnectionFactory(exceptionConFactory);
            String errorMsg = "Unknown exception when created connection";
            exceptionConFactory.setFailCause(new SQLException(errorMsg));

            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("[testOtherException]Test failed");
            } catch (SQLException e) {
                Assertions.assertEquals(errorMsg, e.getMessage());
            }

            String errorMsg2 = "Unknown exception when created connection";
            exceptionConFactory.setFailCause(new RuntimeException(errorMsg2));
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("[testOtherException]Test failed");
            } catch (SQLException e) {
                Assertions.assertInstanceOf(ConnectionCreatedException.class, e);
                Throwable cause = e.getCause();
                Assertions.assertInstanceOf(RuntimeException.class, cause);
                Assertions.assertEquals(errorMsg2, cause.getMessage());
            }
        }


        try (BeeDataSource ds = new BeeDataSource()) {
            ExceptionXaConnectionFactory exceptionConFactory = new ExceptionXaConnectionFactory();
            ds.setXaConnectionFactory(exceptionConFactory);
            String errorMsg = "Unknown exception when created connection";
            exceptionConFactory.setFailCause(new SQLException(errorMsg));

            try {
                XAConnection ignored = ds.getXAConnection();
                Assertions.fail("[testOtherException]Test failed");
            } catch (SQLException e) {
                Assertions.assertEquals(errorMsg, e.getMessage());
            }

            String errorMsg2 = "Unknown exception when created connection";
            exceptionConFactory.setFailCause(new RuntimeException(errorMsg2));
            try {
                XAConnection ignored = ds.getXAConnection();
                Assertions.fail("[testOtherException]Test failed");
            } catch (SQLException e) {
                Assertions.assertInstanceOf(ConnectionCreatedException.class, e);
                Throwable cause = e.getCause();
                Assertions.assertInstanceOf(RuntimeException.class, cause);
                Assertions.assertEquals(errorMsg2, cause.getMessage());
            }
        }
    }
}




