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

public class TransactionNormalRollbackTest extends TestCase {
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

        PreparedStatement ps3 = null;
        ResultSet re3 = null;
        try {
            con1 = ds.getConnection();
            con1.setAutoCommit(false);

            String userId = String.valueOf(new Random(Long.MAX_VALUE).nextLong());
            ps1 = con1
                    .prepareStatement("select count(*) from " + JdbcConfig.TEST_TABLE + " where TEST_ID='" + userId + "'");
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

            con1.rollback();

            ps3 = con1
                    .prepareStatement("select count(*) from " + JdbcConfig.TEST_TABLE + " where TEST_ID='" + userId + "'");
            re3 = ps3.executeQuery();
            if (re3.next()) {
                int size = re3.getInt(1);
                if (size != 0)
                    TestUtil.assertError("rollback failed");
            }
        } finally {
            if (re1 != null)
                TestUtil.oclose(re1);
            if (ps1 != null)
                TestUtil.oclose(ps1);
            if (ps2 != null)
                TestUtil.oclose(ps2);
            if (re3 != null)
                TestUtil.oclose(re3);
            if (ps3 != null)
                TestUtil.oclose(ps3);
            if (con1 != null)
                TestUtil.oclose(con1);
        }
    }
}
