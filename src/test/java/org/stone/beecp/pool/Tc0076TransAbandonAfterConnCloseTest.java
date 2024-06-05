package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.JdbcConfig;
import org.stone.beecp.config.DsConfigFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Random;

public class Tc0076TransAbandonAfterConnCloseTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        ds = new BeeDataSource(DsConfigFactory.createDefault());
    }

    public void tearDown() {
        ds.close();
    }

    public void test() throws Exception {
        Connection con1 = null;
        PreparedStatement ps1 = null;
        ResultSet re1 = null;
        PreparedStatement ps2 = null;
        String userId = String.valueOf(new Random(Long.MAX_VALUE).nextLong());

        try {
            con1 = ds.getConnection();
            con1.setAutoCommit(false);
            ps1 = con1.prepareStatement("select count(*) from " + JdbcConfig.TEST_TABLE + " where TEST_ID='" + userId + "'");
            re1 = ps1.executeQuery();

            if (re1.next()) Assert.assertEquals(0, re1.getInt(1));
            ps2 = con1.prepareStatement("insert into " + JdbcConfig.TEST_TABLE + "(TEST_ID,TEST_NAME)values(?,?)");
            ps2.setString(1, userId);
            ps2.setString(2, userId);
            Assert.assertEquals(1, ps2.executeUpdate());
        } finally {
            TestUtil.oclose(re1);
            TestUtil.oclose(ps1);
            TestUtil.oclose(ps2);
            TestUtil.oclose(con1);
        }

        Connection con2 = null;
        PreparedStatement ps3 = null;
        ResultSet re3 = null;
        try {
            con2 = ds.getConnection();
            ps3 = con2.prepareStatement("select count(*) from " + JdbcConfig.TEST_TABLE + " where TEST_ID='" + userId + "'");
            re3 = ps3.executeQuery();
            if (re3.next())
                Assert.assertEquals(0, re3.getInt(1));
        } finally {
            TestUtil.oclose(re3);
            TestUtil.oclose(ps3);
            TestUtil.oclose(con2);
        }
    }
}
