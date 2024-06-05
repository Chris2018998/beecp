package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.config.DsConfigFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class Tc0052ConnectionGetTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        ds = new BeeDataSource(DsConfigFactory.createDefault());
    }

    public void tearDown() {
        ds.close();
    }

    public void test() throws SQLException {
        Connection con = null;
        try {
            con = ds.getConnection();
            Assert.assertNotNull(con);
        } finally {
            TestUtil.oclose(con);
        }
    }
}
