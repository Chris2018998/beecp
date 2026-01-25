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
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeMethodLog;
import org.stone.beecp.exception.BeeDataSourcePoolLazyInitializationException;
import org.stone.test.beecp.objects.listener.MockMethodExecutionListener1;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Chris Liao
 */
public class Tc0031DsPoolLazyInitializationTest {

    @Test
    public void testPoolLazyException() throws SQLException {
        String driver = "org.stone.test.beecp.driver.MockDriver";
        String url = "jdbc:beecp:testdb";
        String user = "root";
        String password = "root";

        String poolReadyDesc = "Pool is ready";
        String poolLazyExceptionMsg = "No operations allowed on lazy pool";
        String poolLazyDesc = "Pool is lazy and initialized by calling one of its methods:getObjectHandle or getXAConnection";

        try (BeeDataSource ds = new BeeDataSource(driver, url, user, password)) {
            BeeConnectionPoolMonitorVo poolMonitorVo = ds.getPoolMonitorVo();
            Assertions.assertTrue(poolMonitorVo.isLazy());
            Assertions.assertEquals(poolLazyDesc, poolMonitorVo.toString());
            Assertions.assertEquals(poolLazyDesc, ds.toString());

            //1: lazy initialization
            try {
                ds.suspend();
                Assertions.fail("[testPoolLazyException]test failed");
            } catch (SQLException e) {
                Assertions.assertInstanceOf(BeeDataSourcePoolLazyInitializationException.class, e);
                Assertions.assertEquals(poolLazyExceptionMsg, e.getMessage());
            }

            try {
                ds.resume();
                Assertions.fail("[testPoolLazyException]test failed");
            } catch (SQLException e) {
                Assertions.assertInstanceOf(BeeDataSourcePoolLazyInitializationException.class, e);
                Assertions.assertEquals(poolLazyExceptionMsg, e.getMessage());
            }

            try {
                ds.restart(true);
                Assertions.fail("[testPoolLazyException]test failed");
            } catch (SQLException e) {
                Assertions.assertInstanceOf(BeeDataSourcePoolLazyInitializationException.class, e);
                Assertions.assertEquals(poolLazyExceptionMsg, e.getMessage());
            }

            try {
                ds.restart(true, new BeeDataSourceConfig());
                Assertions.fail("[testPoolLazyException]test failed");
            } catch (SQLException e) {
                Assertions.assertInstanceOf(BeeDataSourcePoolLazyInitializationException.class, e);
                Assertions.assertEquals(poolLazyExceptionMsg, e.getMessage());
            }

            try {
                ds.enableLogPrinter(false);
                Assertions.fail("[testPoolLazyException]test failed");
            } catch (SQLException e) {
                Assertions.assertInstanceOf(BeeDataSourcePoolLazyInitializationException.class, e);
                Assertions.assertEquals(poolLazyExceptionMsg, e.getMessage());
            }

            try {
                ds.enableLogCache(false);
                Assertions.fail("[testPoolLazyException]test failed");
            } catch (SQLException e) {
                Assertions.assertInstanceOf(BeeDataSourcePoolLazyInitializationException.class, e);
                Assertions.assertEquals(poolLazyExceptionMsg, e.getMessage());
            }

            try {
                ds.changeLogListener(new MockMethodExecutionListener1());
                Assertions.fail("[testPoolLazyException]test failed");
            } catch (SQLException e) {
                Assertions.assertInstanceOf(BeeDataSourcePoolLazyInitializationException.class, e);
                Assertions.assertEquals(poolLazyExceptionMsg, e.getMessage());
            }

            try {
                ds.getLogs(BeeMethodLog.Type_Pool_Log);
                Assertions.fail("[testPoolLazyException]test failed");
            } catch (SQLException e) {
                Assertions.assertInstanceOf(BeeDataSourcePoolLazyInitializationException.class, e);
                Assertions.assertEquals(poolLazyExceptionMsg, e.getMessage());
            }

            try {
                ds.clearLogs(BeeMethodLog.Type_Pool_Log);
                Assertions.fail("[testPoolLazyException]test failed");
            } catch (SQLException e) {
                Assertions.assertInstanceOf(BeeDataSourcePoolLazyInitializationException.class, e);
                Assertions.assertEquals(poolLazyExceptionMsg, e.getMessage());
            }

            try {
                ds.clearLogs(BeeMethodLog.Type_Pool_Log);
                Assertions.fail("[testPoolLazyException]test failed");
            } catch (SQLException e) {
                Assertions.assertInstanceOf(BeeDataSourcePoolLazyInitializationException.class, e);
                Assertions.assertEquals(poolLazyExceptionMsg, e.getMessage());
            }

            try {
                ds.cancelStatement(null);
                Assertions.fail("[testPoolLazyException]test failed");
            } catch (SQLException e) {
                Assertions.assertInstanceOf(BeeDataSourcePoolLazyInitializationException.class, e);
                Assertions.assertEquals(poolLazyExceptionMsg, e.getMessage());
            }

            try {
                ds.enableLogPrinter(true);
                Assertions.fail("[testPoolLazyException]test failed");
            } catch (SQLException e) {
                Assertions.assertInstanceOf(BeeDataSourcePoolLazyInitializationException.class, e);
                Assertions.assertEquals(poolLazyExceptionMsg, e.getMessage());
            }

            //2: lazy initialization
            try (Connection ignored = ds.getConnection()) {
                Assertions.assertNotNull(ignored);
            }

            poolMonitorVo = ds.getPoolMonitorVo();
            Assertions.assertFalse(poolMonitorVo.isLazy());
            Assertions.assertEquals(poolReadyDesc, poolMonitorVo.toString());
            Assertions.assertEquals(poolReadyDesc, ds.toString());
        }
    }
}
