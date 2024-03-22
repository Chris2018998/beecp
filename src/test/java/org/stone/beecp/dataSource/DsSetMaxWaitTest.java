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
import org.stone.base.TestException;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;

public class DsSetMaxWaitTest extends TestCase {

    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setMaxWait(8000);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testSetMaxWait() throws Exception {
        long wait = ds.getMaxWait();
        if (wait != 8000) throw new TestException();

        ds.setMaxWait(0);
        wait = ds.getMaxWait();
        if (wait != 8000) throw new TestException();

        ds.setMaxWait(5000);
        wait = ds.getMaxWait();
        if (wait != 5000) throw new TestException();
    }
}
