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
import org.stone.beecp.factory.BlockingNullConnectionFactory;
import org.stone.beecp.pool.ConnectionPoolStatics;
import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;

import java.sql.Connection;
import java.sql.SQLException;

public class DsInterruptCreateLockTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setRawConnectionFactory(new BlockingNullConnectionFactory());
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testInterruptCreationLock() throws SQLException {
        new MockThreadToInterruptCreateLock(ds).start();

        Connection con = null;
        try {
            con = ds.getConnection();
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof ConnectionGetInterruptedException);
        } finally {
            if (con != null) ConnectionPoolStatics.oclose(con);
        }
    }
}
