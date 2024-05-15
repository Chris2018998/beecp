/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.dataSource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;

import java.io.PrintWriter;
import java.sql.DriverManager;

public class CommonDsMethodTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setConnectTimeout(10);//seconds
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testLogWriter() throws Exception {
        PrintWriter printer1 = ds.getLogWriter();
        PrintWriter printer2 = DriverManager.getLogWriter();

        Assert.assertEquals(printer1, printer2);
    }

    public void testLoginTimeout() throws Exception {
        Assert.assertEquals(10, ds.getConnectTimeout());
        Assert.assertEquals(10, ds.getLoginTimeout());
        Assert.assertEquals(10, DriverManager.getLoginTimeout());

        ds.setLoginTimeout(5);

        Assert.assertEquals(5, ds.getLoginTimeout());
        Assert.assertEquals(5, DriverManager.getLoginTimeout());
    }

}
