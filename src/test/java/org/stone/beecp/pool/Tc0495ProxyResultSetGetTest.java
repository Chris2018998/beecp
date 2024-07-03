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
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0495ProxyResultSetGetTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setAliveTestSql("SELECT 1 from dual");
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testProxyResultSetGet() throws Exception {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        ResultSet rs2;
        try {
            con = ds.getConnection();
            ps = con.prepareStatement("select * from BEECP_TEST");
            rs = ps.executeQuery();
            rs2 = ps.getResultSet();
            Assert.assertEquals(rs2, rs);
            Assert.assertEquals(rs2, ps.getResultSet());
        } finally {
            oclose(rs);
            oclose(ps);
            oclose(con);
        }
    }
}
