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
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;

import java.sql.Connection;

public class DsGetConnectionTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testGetConnection() throws Exception {
        Connection con = null;
        try {
            con = ds.getConnection();
            Assert.assertNotNull(con);
            //if (con == null) throw new TestException("");
        } finally {
            if (con != null)
                TestUtil.oclose(con);
        }
    }

    public void testGetConnectionWithUserId() throws Exception {
        Connection con = null;
        try {
            con = ds.getConnection("test", "test");
            Assert.assertNotNull(con);
            //if (con == null) throw new TestException("");
        } finally {
            if (con != null)
                TestUtil.oclose(con);
        }
    }
}
