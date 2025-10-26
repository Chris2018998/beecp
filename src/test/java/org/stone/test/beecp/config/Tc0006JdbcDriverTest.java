/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.test.beecp.driver.MockDriver;
import org.stone.test.beecp.objects.driver.TestDriver_NoDefaultConstructor;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Chris Liao
 */

public class Tc0006JdbcDriverTest {

    @Test
    public void testCheckFailed_UrlNotMatchDriver() throws SQLException {
        DriverManager.registerDriver(new MockDriver());
        BeeDataSourceConfig config = new BeeDataSourceConfig();

        //1: test not found matched Driver with given url
        try {
            config.setUrl("jdbc:beecp1://localhost/testdb");
            config.check();
            fail("[testDriver]Test failed");
        } catch (SQLException e) {
            assertEquals("No suitable driver", e.getMessage());
        }

        //2: test driver can not accept given url
        try {
            config.setDriverClassName(MockDriver.class.getName());
            config.check();
            fail("[testDriver]Test failed");
        } catch (BeeDataSourceConfigException e) {
            String expectedMsg = "jdbcUrl(" + config.getJdbcUrl() + ")can not match configured driver[" + MockDriver.class.getName() + "]";
            assertEquals(expectedMsg, e.getMessage());
        }
    }

    @Test
    public void testCheckFailed_DriverLoadFail() throws SQLException {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setUrl("jdbc:beecp://localhost/testdb");

        String driverClass1 = MockDriver.class.getName() + "_ClassNotFound";
        config.setDriverClassName(driverClass1);
        try {
            config.check();
            fail("[testDriverLoadFail]Test failed");
        } catch (BeeDataSourceConfigException e) {
            assertEquals("Failed to create jdbc driver by class:" + driverClass1, e.getMessage());
        }

        String driverClass2 = TestDriver_NoDefaultConstructor.class.getName();
        config.setDriverClassName(driverClass2);
        try {
            config.check();
            fail("[testDriverLoadFail]Test failed");
        } catch (BeeDataSourceConfigException e) {
            assertEquals("Failed to create jdbc driver by class:" + driverClass2, e.getMessage());
        }
    }

    @Test
    public void testCheckPassed() throws Exception {
        DriverManager.registerDriver(new MockDriver());
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        try {
            config.setUrl("jdbc:beecp://localhost/testdb");
            config.check();
        } catch (Exception e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }
    }
}
