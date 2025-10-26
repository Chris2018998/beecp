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
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.BeeMethodExecutionLog;
import org.stone.beecp.pool.exception.PoolInitializeFailedException;
import org.stone.beecp.pool.exception.PoolNotCreatedException;
import org.stone.test.beecp.driver.MockDriver;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author Chris Liao
 */
public class Tc0031DsPoolNotReadyTest {

    @Test
    public void testPoolNotCreatedException() throws SQLException {
        //1: Pool Creation in BeeDataSource constructor: new BeeDataSource(BeeDataSourceConfig config)
        //2: Pool Lazy Creation(ds.getConnection(),ds.getXAConnection(),ds.getConnection(String,String),ds.getXAConnection(String,String))
        try (BeeDataSource ds = new BeeDataSource()) {
            Assertions.assertTrue(ds.isClosed());
            Assertions.assertFalse(ds.isReady());
            try {
                ds.restart(false);
                Assertions.fail("[testPoolNotCreatedException]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            try {
                ds.restart(false, new BeeDataSourceConfig());
                Assertions.fail("[testPoolNotCreatedException]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }


            try {
                ds.enableLogPrint(false);
                Assertions.fail("[testPoolNotCreatedException]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            try {
                ds.isEnabledLogPrint();
                Assertions.fail("[testPoolNotCreatedException]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            try {
                ds.enableMethodExecutionLogCache(false);
                Assertions.fail("[testPoolNotCreatedException]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            try {
                ds.clearMethodExecutionLog(BeeMethodExecutionLog.Type_Connection_Get);
                Assertions.fail("[testPoolNotCreatedException]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            try {
                ds.getMethodExecutionLog(BeeMethodExecutionLog.Type_Connection_Get);
                Assertions.fail("[testPoolNotCreatedException]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            try {
                ds.clearMethodExecutionLog(BeeMethodExecutionLog.Type_Connection_Get);
                Assertions.fail("[testPoolNotCreatedException]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            try {
                ds.isEnabledMethodExecutionLogCache();
                Assertions.fail("[testPoolNotCreatedException]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            try {
                ds.getPoolMonitorVo();
                Assertions.fail("[testPoolNotCreatedException]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            try {
                ds.getConnection();
                Assertions.fail("[testPoolNotCreatedException]test failed");
            } catch (PoolInitializeFailedException e) {
                Throwable cause = e.getCause();
                Assertions.assertInstanceOf(BeeDataSourceConfigException.class, cause);
                Assertions.assertEquals("jdbcUrl must not be null or blank", cause.getMessage());
            }
            try {
                ds.getConnection("root", "root");
                Assertions.fail("[testPoolNotCreatedException]test failed");
            } catch (PoolInitializeFailedException e) {
                Throwable cause = e.getCause();
                Assertions.assertInstanceOf(BeeDataSourceConfigException.class, cause);
                Assertions.assertEquals("jdbcUrl must not be null or blank", cause.getMessage());
            }

            try {
                ds.getXAConnection();
                Assertions.fail("[testPoolNotCreatedException]test failed");
            } catch (PoolInitializeFailedException e) {
                Throwable cause = e.getCause();
                Assertions.assertInstanceOf(BeeDataSourceConfigException.class, cause);
                Assertions.assertEquals("jdbcUrl must not be null or blank", cause.getMessage());
            }

            try {
                ds.getXAConnection("root", "root");
                Assertions.fail("[testPoolNotCreatedException]test failed");
            } catch (PoolInitializeFailedException e) {
                Throwable cause = e.getCause();
                Assertions.assertInstanceOf(BeeDataSourceConfigException.class, cause);
                Assertions.assertEquals("jdbcUrl must not be null or blank", cause.getMessage());
            }
        }
    }

    @Test
    public void testPoolNotCreatedException2() throws SQLException {
        DriverManager.registerDriver(new MockDriver());
        String driver = "org.stone.test.beecp.driver.MockDriver";
        String url = "jdbc:beecp:testdb";
        String user = "root";
        String password = "root";

        try (BeeDataSource ds = new BeeDataSource(driver, url, user, password)) {
            try {
                ds.restart(false);
                Assertions.fail("[testPoolNotCreatedException2]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            try {
                ds.restart(false, new BeeDataSourceConfig());
                Assertions.fail("[testPoolNotCreatedException2]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            try {
                ds.enableLogPrint(false);
                Assertions.fail("[testPoolNotCreatedException2]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            try {
                ds.isEnabledLogPrint();
                Assertions.fail("[testPoolNotCreatedException2]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            try {
                ds.enableMethodExecutionLogCache(false);
                Assertions.fail("[testPoolNotCreatedException2]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            try {
                ds.clearMethodExecutionLog(BeeMethodExecutionLog.Type_Connection_Get);
                Assertions.fail("[testPoolNotCreatedException2]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            try {
                ds.getMethodExecutionLog(BeeMethodExecutionLog.Type_Connection_Get);
                Assertions.fail("[testPoolNotCreatedException2]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            try {
                ds.clearMethodExecutionLog(BeeMethodExecutionLog.Type_Connection_Get);
                Assertions.fail("[testPoolNotCreatedException2]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            try {
                ds.isEnabledMethodExecutionLogCache();
                Assertions.fail("[testPoolNotCreatedException2]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            try {
                ds.getPoolMonitorVo();
                Assertions.fail("[testPoolNotCreatedException2]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Internal pool was not ready", e.getMessage());
            }

            //pool created during getConnection() method call
            try (Connection ignored = ds.getConnection()) {
                Assertions.assertNotNull(ignored);
            } catch (PoolInitializeFailedException e) {
                Assertions.fail("[testPoolNotCreatedException2]test failed");
            }

            try (Connection ignored = ds.getConnection("root", "root")) {
                Assertions.assertNotNull(ignored);
            } catch (PoolInitializeFailedException e) {
                Assertions.fail("[testPoolNotCreatedException2]test failed");
            }

            try {
                XAConnection xaConnection1 = ds.getXAConnection();
                try (Connection ignored = xaConnection1.getConnection()) {
                    Assertions.assertNotNull(ignored);
                }
            } catch (PoolInitializeFailedException e) {
                Assertions.fail("[testPoolNotCreatedException2]test failed");
            }

            try {
                XAConnection xaConnection2 = ds.getXAConnection("root", "root");
                try (Connection ignored = xaConnection2.getConnection()) {
                    Assertions.assertNotNull(ignored);
                }
            } catch (PoolInitializeFailedException e) {
                Assertions.fail("[testPoolNotCreatedException2]test failed");
            }
        }
    }
}
