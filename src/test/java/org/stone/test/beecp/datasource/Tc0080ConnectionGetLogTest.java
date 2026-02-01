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
import org.stone.beecp.BeeMethodLog;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;
import org.stone.test.beecp.objects.factory.MockXaConnectionFactory;
import org.stone.test.beecp.objects.listener.MockMethodExecutionListener1;
import org.stone.test.beecp.objects.threads.BorrowThread;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.stone.beecp.BeeMethodLog.Type_Pool_Log;

/**
 * @author Chris Liao
 */
public class Tc0080ConnectionGetLogTest {

    @Test
    public void testExceptionLog() throws SQLException {
        //1: get connection
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setEnableLogCache(true);
            ds.setLogListener(new MockMethodExecutionListener1());

            MockConnectionFactory connectionFactory = new MockConnectionFactory();
            connectionFactory.setFailCause(new SQLException("Failed to connect database"));
            ds.setConnectionFactory(connectionFactory);

            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("[testConnectionExceptionLog]Test failed");
            } catch (SQLException e) {
                Assertions.assertTrue(ds.getPoolMonitorVo().isEnabledLogCache());
                List<BeeMethodLog> logList = ds.getLogs(Type_Pool_Log);
                Assertions.assertEquals(1, logList.size());
                BeeMethodLog log = logList.get(0);
                Assertions.assertNotNull(log.getId());
                Assertions.assertEquals(Type_Pool_Log, log.getType());

                Assertions.assertEquals("FastConnectionPool.getConnection()", log.getMethod());
                Assertions.assertFalse(log.isSuccessful());
                Assertions.assertTrue(log.isException());
                Assertions.assertFalse(log.isRunning());
                Assertions.assertNull(log.getResult());
                Assertions.assertNotNull(log.getFailureCause());

                Assertions.assertTrue(log.getStartTime() != 0);
                Assertions.assertTrue(log.getEndTime() != 0);
                Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
            }
        }

        //2: get XA connection
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setLogListener(new MockMethodExecutionListener1());
            ds.setEnableLogCache(true);//sync mode
            MockXaConnectionFactory xaConnectionFactory = new MockXaConnectionFactory();
            xaConnectionFactory.setFailCause(new SQLException("Failed to connect database"));
            ds.setXaConnectionFactory(xaConnectionFactory);

            try {
                XAConnection xaCon = ds.getXAConnection();
                try (Connection ignored = xaCon.getConnection()) {
                    Assertions.fail("[testConnectionExceptionLog]Test failed");
                }
            } catch (SQLException e) {
                Assertions.assertTrue(ds.getPoolMonitorVo().isEnabledLogCache());
                List<BeeMethodLog> logList = ds.getLogs(Type_Pool_Log);
                Assertions.assertEquals(1, logList.size());
                BeeMethodLog log = logList.get(0);
                Assertions.assertNotNull(log.getId());
                Assertions.assertEquals(Type_Pool_Log, log.getType());

                Assertions.assertEquals("FastConnectionPool.getXAConnection()", log.getMethod());
                Assertions.assertTrue(log.isException());
                Assertions.assertFalse(log.isSuccessful());
                Assertions.assertFalse(log.isRunning());
                Assertions.assertNull(log.getResult());
                Assertions.assertTrue(log.getStartTime() != 0);
                Assertions.assertTrue(log.getEndTime() != 0);
                Assertions.assertTrue(log.getEndTime() >= log.getStartTime());

                Assertions.assertEquals(1, ds.getLogs(Type_Pool_Log).size());
                ds.clearLogs(Type_Pool_Log);
            }
        }
    }

    @Test
    public void testSlowLog() throws Exception {
        //1: get connection
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setMaxActive(1);
            ds.setLogListener(new MockMethodExecutionListener1());
            ds.setEnableLogCache(true);
            ds.setSlowConnectionThreshold(1L);
            MockConnectionFactory connectionFactory = new MockConnectionFactory();
            connectionFactory.setNeedPark(true);
            connectionFactory.setParkNanos(TimeUnit.MILLISECONDS.toNanos(500L));
            ds.setConnectionFactory(connectionFactory);

            BorrowThread borrowThread = new BorrowThread(ds);
            borrowThread.start();
            borrowThread.join();

            Assertions.assertTrue(ds.getPoolMonitorVo().isEnabledLogCache());
            List<BeeMethodLog> logList = ds.getLogs(Type_Pool_Log);
            Assertions.assertEquals(1, logList.size());
            BeeMethodLog log = logList.get(0);
            Assertions.assertNotNull(log.getId());
            Assertions.assertEquals(Type_Pool_Log, log.getType());
            Assertions.assertTrue(log.isSlow());
            Assertions.assertTrue(log.isSuccessful());
            Assertions.assertFalse(log.isException());
        }

        //2: get XA connection
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setMaxActive(1);
            ds.setLogListener(new MockMethodExecutionListener1());
            ds.setEnableLogCache(true);
            ds.setSlowConnectionThreshold(1L);
            MockXaConnectionFactory xaConnectionFactory = new MockXaConnectionFactory();
            xaConnectionFactory.setNeedPark(true);
            xaConnectionFactory.setParkNanos(TimeUnit.MILLISECONDS.toNanos(500L));
            ds.setXaConnectionFactory(xaConnectionFactory);

            BorrowThread borrowThread = new BorrowThread(ds, true);
            borrowThread.start();
            borrowThread.join();

            List<BeeMethodLog> logList = ds.getLogs(Type_Pool_Log);
            BeeMethodLog log = logList.get(0);
            Assertions.assertEquals(Type_Pool_Log, log.getType());
            Assertions.assertTrue(log.isSlow());
        }
    }
}
