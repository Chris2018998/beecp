/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.config.DsConfigFactory;
import org.stone.beecp.pool.exception.ConnectionGetForbiddenException;

import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0060DataSourceConnectionCloseTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        ds = new BeeDataSource(DsConfigFactory.createDefault());
    }

    public void tearDown() {
        //do nothing
    }

    public void testGetFromClosedDataSource() {
        ds.close();
        Connection con = null;
        try {
            con = ds.getConnection();
            fail("Failed test on closed data source");
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof ConnectionGetForbiddenException);
            Assert.assertTrue(e.getMessage().contains("Pool was closed or in clearing"));
        } finally {
            if (con != null)
                oclose(con);
        }
    }
}
