/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.config;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.JdbcConfig;

import java.sql.SQLException;

public class Tc0003JdbcDriverTest extends TestCase {

    public void testNoSuitableDriver() {
        try {
            BeeDataSourceConfig config = DsConfigFactory.createEmpty();
            config.setUrl("jdbc:beecp://localhost/testdb");
            config.check();
        } catch (SQLException e) {//thrown from DriverManager
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("No suitable driver"));
        }
    }

    public void testUrlNotMatchDriver() throws Exception {
        try {
            BeeDataSourceConfig config = DsConfigFactory.createEmpty();
            config.setJdbcUrl("Test:" + JdbcConfig.JDBC_URL);
            config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
            config.check();
        } catch (BeeDataSourceConfigException e) {//thrown from Config.check()
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("can not match configured driver"));
        }
    }
}
