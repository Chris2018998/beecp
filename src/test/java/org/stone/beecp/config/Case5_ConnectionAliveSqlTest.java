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

public class Case5_ConnectionAliveSqlTest extends TestCase {

    public void testOnSetAndGet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setAliveTestSql(null);
        Assert.assertNull(config.getAliveTestSql());

        config.setAliveTestSql("SELECT1");
        Assert.assertEquals("SELECT1", config.getAliveTestSql());
    }

    public void testOnInvalidSql() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);

        config.setAliveTestSql("SELECT1");
        try {
            config.check();
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Alive test sql must be start with 'select '"));
        }
    }
}
