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
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;

import java.sql.SQLException;

public class JdbcDriverFailureTest extends TestCase {

    public void testNotMatch() throws Exception {
        try {
            BeeDataSourceConfig config = new BeeDataSourceConfig();
            config.setUrl("jdbcX:beecp://localhost/testdb");
            config.setDriverClassName("org.stone.beecp.mock.MockDriver");
            config.check();
        } catch (BeeDataSourceConfigException e) {
            String msg = e.getMessage();
            boolean matched = msg != null && msg.contains("can not match configured driver");
            if (!matched) throw new TestException();
        }
    }

    public void testNotFound() throws Exception {
        try {
            BeeDataSourceConfig config = new BeeDataSourceConfig();
            config.setUrl("jdbc:beecp://localhost/testdb");
            config.check();
        } catch (SQLException e) {
            String msg = e.getMessage();
            boolean matched = msg != null && msg.contains("No suitable driver");
            if (!matched) throw new TestException();
        }
    }
}
