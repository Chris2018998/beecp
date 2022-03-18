/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test.pool;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import cn.beecp.test.JdbcConfig;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Random;

public class TransAbandonAfterConnCloseTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
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
            if (re1.next()) {
                int size = re1.getInt(1);
                if (size != 0)
                    TestUtil.assertError("record size error");
            }

            ps2 = con1.prepareStatement("insert into " + JdbcConfig.TEST_TABLE + "(TEST_ID,TEST_NAME)values(?,?)");
            ps2.setString(1, userId);
            ps2.setString(2, userId);
            int rows = ps2.executeUpdate();
            if (rows != 1)
                TestUtil.assertError("Failed to insert");
        } finally {
            if (re1 != null)
                TestUtil.oclose(re1);
            if (ps1 != null)
                TestUtil.oclose(ps1);
            if (ps2 != null)
                TestUtil.oclose(ps2);
            if (con1 != null)
                TestUtil.oclose(con1);
        }

        Connection con2 = null;
        PreparedStatement ps3 = null;
        ResultSet re3 = null;

        try {
            con2 = ds.getConnection();
            ps3 = con2.prepareStatement("select count(*) from " + JdbcConfig.TEST_TABLE + " where TEST_ID='" + userId + "'");
            re3 = ps3.executeQuery();
            if (re3.next()) {
                int size = re3.getInt(1);
                if (size != 0)
                    TestUtil.assertError("rollback failed");
            }
        } finally {
            if (re3 != null)
                TestUtil.oclose(re3);
            if (ps3 != null)
                TestUtil.oclose(ps3);
            if (con2 != null)
                TestUtil.oclose(con2);
        }
    }
}
