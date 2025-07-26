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

import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.*;

/**
 * @author Chris Liao
 */

public class Tc0005JdbcDriverTest {

    @Test
    public void testNotFindSuitableDriver() {
        try {
            BeeDataSourceConfig config = createEmpty();
            config.setUrl("jdbc:beecp1://localhost/testdb");
            config.check();

            fail("[testNotFindSuitableDriver]Not threw exception when not found suitable driver");
        } catch (SQLException e) {//thrown from DriverManager
            String message = e.getMessage();
            assertTrue(message != null && message.contains("No suitable driver"));
        }
    }

    @Test
    public void testConfigDriverNotMatchUrl() throws Exception {
        try {
            BeeDataSourceConfig config = createEmpty();
            config.setJdbcUrl("Test:" + JDBC_URL);
            config.setDriverClassName(JDBC_DRIVER);
            config.check();
            fail("[testConfigDriverNotMatchUrl]Not threw exception when url not matched driver");
        } catch (BeeDataSourceConfigException e) {//thrown from Config.check()
            String message = e.getMessage();
            assertTrue(message != null && message.contains("can not match configured driver"));
        }
    }
}
