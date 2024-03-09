/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;

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
            if (re1.next() && re1.getInt(1) != 0)
                TestUtil.assertError("record size error");

            ps2 = con1.prepareStatement("insert into " + JdbcConfig.TEST_TABLE + "(TEST_ID,TEST_NAME)values(?,?)");
            ps2.setString(1, userId);
            ps2.setString(2, userId);
            if (ps2.executeUpdate() != 1) TestUtil.assertError("Failed to insert");
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
            if (re3.next() && re3.getInt(1) != 0)
                TestUtil.assertError("rollback failed");
        } finally {
            TestUtil.oclose(re3);
            TestUtil.oclose(ps3);
            TestUtil.oclose(con2);
        }
    }
}
