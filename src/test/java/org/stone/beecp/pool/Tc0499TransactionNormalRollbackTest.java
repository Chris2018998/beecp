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
import java.util.Random;

import static org.stone.beecp.config.DsConfigFactory.*;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0499TransactionNormalRollbackTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setDriverClassName(JDBC_DRIVER);
        config.setUsername(JDBC_USER);
        config.setPassword(JDBC_PASSWORD);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testTransactionNormalRollback() throws Exception {
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
                    .prepareStatement("select count(*) from " + TEST_TABLE + " where TEST_ID='" + userId + "'");
            re1 = ps1.executeQuery();
            if (re1.next()) {
                int size = re1.getInt(1);
                Assert.assertEquals(0, size);
            }

            ps2 = con1.prepareStatement("insert into " + TEST_TABLE + "(TEST_ID,TEST_NAME)values(?,?)");
            ps2.setString(1, userId);
            ps2.setString(2, userId);
            int rows = ps2.executeUpdate();
            Assert.assertEquals(1, rows);
            con1.rollback();

            ps3 = con1
                    .prepareStatement("select count(*) from " + TEST_TABLE + " where TEST_ID='" + userId + "'");
            re3 = ps3.executeQuery();
            if (re3.next()) {
                int size = re3.getInt(1);
                Assert.assertEquals(0, size);
            }
        } finally {
            if (re1 != null)
                oclose(re1);
            if (ps1 != null)
                oclose(ps1);
            if (ps2 != null)
                oclose(ps2);
            if (re3 != null)
                oclose(re3);
            if (ps3 != null)
                oclose(ps3);
            if (con1 != null)
                oclose(con1);
        }
    }
}
