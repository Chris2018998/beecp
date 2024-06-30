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
import org.stone.beecp.pool.exception.ConnectionGetForbiddenException;

import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0062ConnectionPoolCloseTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        ds = new BeeDataSource(createDefault());
    }

    public void tearDown() {
        ds.close();
    }

    public void testGetFromClosedDataSource() throws SQLException {
        ds.close();
        Connection con = null;
        try {
            con = ds.getConnection();
            fail("Failed test on closed data source");
        } catch (ConnectionGetForbiddenException e) {
            Assert.assertTrue(e.getMessage().contains("Pool was closed or in clearing"));
        } finally {
            if (con != null)
                oclose(con);
        }
    }
}
