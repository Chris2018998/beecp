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
import org.stone.base.TestException;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.JdbcConfig;
import org.stone.beecp.pool.ConnectionPoolStatics;

import java.sql.Connection;
import java.sql.SQLException;

public class DsPoolMonitorTest extends TestCase {

    public void testMonitor() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        ds.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        ds.setUsername(JdbcConfig.JDBC_USER);

        try {
            ds.getPoolMonitorVo();//pool not be initialized
            throw new TestException();
        } catch (SQLException e) {
            //do nothing
        }

        Connection con = null;
        try {
            con = ds.getConnection();
            Assert.assertNotNull(ds.getPoolMonitorVo());
            //if (ds.getPoolMonitorVo() == null) throw new TestException();
        } catch (SQLException e) {
            //do nothing
        } finally {
            if (con != null) ConnectionPoolStatics.oclose(con);
        }
    }
}
