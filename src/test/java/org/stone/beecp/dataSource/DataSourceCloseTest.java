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
import org.stone.beecp.JdbcConfig;
import org.stone.beecp.pool.ConnectionPoolStatics;

import java.sql.Connection;

public class DataSourceCloseTest extends TestCase {

    public void testDataSourceClose() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setJdbcUrl(JdbcConfig.JDBC_URL);
        ds.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        ds.setUsername(JdbcConfig.JDBC_USER);
        Assert.assertTrue(ds.isClosed());

        Connection con = null;
        try {
            con = ds.getConnection();
        } finally {
            ConnectionPoolStatics.oclose(con);
        }

        Assert.assertFalse(ds.isClosed());
        ds.close();
        Assert.assertTrue(ds.isClosed());
    }
}
