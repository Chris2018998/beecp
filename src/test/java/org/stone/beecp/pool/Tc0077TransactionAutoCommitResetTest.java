/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

public class Tc0077TransactionAutoCommitResetTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        config.setDefaultAutoCommit(false);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void test() throws Exception {
        Connection con1 = null;
        PreparedStatement ps1 = null;
        ResultSet re1 = null;
        try {
            con1 = ds.getConnection();
            String userId = String.valueOf(new Random(Long.MAX_VALUE).nextLong());
            ps1 = con1
                    .prepareStatement("select count(*) from " + JdbcConfig.TEST_TABLE + " where TEST_ID='" + userId + "'");
            re1 = ps1.executeQuery();
            try {
                con1.setAutoCommit(true);
                TestUtil.assertError("AutoCommit reset false before rollback or commit");
            } catch (SQLException e) {
            }
        } finally {
            if (re1 != null)
                TestUtil.oclose(re1);
            if (ps1 != null)
                TestUtil.oclose(ps1);
            if (con1 != null)
                TestUtil.oclose(con1);
        }
    }
}