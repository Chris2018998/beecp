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

public class JdbcLinkInfoSetGetTest extends TestCase {

    public void testOnSetGet() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();

        String url = "jdbc:beecp://localhost/testdb";
        config.setUrl(url);
        if (!url.equals(config.getUrl())) throw new TestException();
        if (!url.equals(config.getJdbcUrl())) throw new TestException();

        String driver = "org.stone.beecp.mock.MockDriver";
        config.setDriverClassName(driver);
        if (!driver.equals(config.getDriverClassName())) throw new TestException();

        config.setUsername("test");
        if (!"test".equals(config.getUsername())) throw new TestException();

        config.setPassword("123");
        if (!"123".equals(config.getPassword())) throw new TestException();

    }
}
