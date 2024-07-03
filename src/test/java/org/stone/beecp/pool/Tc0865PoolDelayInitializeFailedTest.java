/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.stone.beecp.BeeDataSource;

import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.beecp.config.DsConfigFactory.*;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0865PoolDelayInitializeFailedTest extends TestCase {
    private final int initSize = 5;

    public void setUp() {
        //do nothing
    }

    public void tearDown() {
        //do nothing
    }

    public void testPoolInit() {
        Connection con = null;
        BeeDataSource ds = null;
        try {
            ds = new BeeDataSource();
            ds.setJdbcUrl("jdbc:beecp://localhost/testdb2");//give valid URL
            ds.setDriverClassName(JDBC_DRIVER);
            ds.setUsername(JDBC_USER);
            ds.setPassword(JDBC_PASSWORD);
            ds.setInitialSize(initSize);
            con = ds.getConnection();
            fail("A pool fail to init e need be thrown,but not");
        } catch (SQLException e) {
        } finally {
            if (con != null) oclose(con);
        }
    }
}
