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

import javax.sql.XAConnection;

public class DsGetXAConnectionTest extends TestCase {
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

    public void testGetXAConnection() throws Exception {
        XAConnection con = null;
        try {
            con = ds.getXAConnection();
            Assert.assertNotNull(con);
            //if (con == null) throw new TestException("");
        } finally {
            if (con != null)
                TestUtil.oclose(con);
        }
    }

    public void testGetXAConnectionWithUserId() throws Exception {
        XAConnection con = null;
        try {
            con = ds.getXAConnection("test", "test");
            Assert.assertNotNull(con);
            //if (con == null) throw new TestException("");
        } finally {
            if (con != null)
                TestUtil.oclose(con);
        }
    }
}
