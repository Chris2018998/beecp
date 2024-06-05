/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.config.DsConfigFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class Tc0060DataSourceConnectionCloseTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        ds = new BeeDataSource(DsConfigFactory.createDefault());
    }

    public void tearDown() {
        //do nothing
    }

    public void test() throws Exception {
        ds.close();
        Connection con = null;
        try {
            con = ds.getConnection();
            fail("Failed test on closed data source");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof SQLException);
        } finally {
            if (con != null)
                TestUtil.oclose(con);
        }
    }
}
