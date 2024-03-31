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
import org.stone.base.TestException;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.JdbcConfig;

import java.sql.SQLException;

public class Case3_JdbcDriverTest extends TestCase {

    public void testNoSuitableDriver() throws Exception {
        try {
            BeeDataSourceConfig config = new BeeDataSourceConfig();
            config.setUrl("jdbc:beecp://localhost/testdb");
            config.check();
        } catch (SQLException e) {//thrown from DriverManager
            if (!TestUtil.containsMessage(e, "No suitable driver")) throw new TestException();
        }
    }

    public void testUrlNotMatchDriver() throws Exception {
        try {
            BeeDataSourceConfig config = new BeeDataSourceConfig();
            config.setJdbcUrl("Test:" + JdbcConfig.JDBC_URL);
            config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
            config.check();
        } catch (SQLException e) {
            throw e;
        } catch (BeeDataSourceConfigException e) {//thrown from Config.check()
            if (!TestUtil.containsMessage(e, "can not match configured driver")) throw new TestException();
        }
    }
}
