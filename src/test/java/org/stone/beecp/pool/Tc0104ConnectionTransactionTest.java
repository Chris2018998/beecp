package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.*;
import java.util.Random;

import static org.stone.beecp.config.DsConfigFactory.TEST_TABLE;
import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0104ConnectionTransactionTest extends TestCase {

    public void testCloseConnectionNotCommited() throws Exception {
        BeeDataSourceConfig config = createDefault();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        Connection con1 = null;
        PreparedStatement ps1 = null;
        ResultSet re1 = null;
        PreparedStatement ps2 = null;
        String userId = String.valueOf(new Random(Long.MAX_VALUE).nextLong());

        try {
            con1 = pool.getConnection();
            con1.setAutoCommit(false);
            ps1 = con1.prepareStatement("select count(*) from " + TEST_TABLE + " where TEST_ID='" + userId + "'");
            re1 = ps1.executeQuery();

            if (re1.next()) Assert.assertEquals(0, re1.getInt(1));
            ps2 = con1.prepareStatement("insert into " + TEST_TABLE + "(TEST_ID,TEST_NAME)values(?,?)");
            ps2.setString(1, userId);
            ps2.setString(2, userId);
            Assert.assertEquals(1, ps2.executeUpdate());
        } finally {
            oclose(re1);
            oclose(ps1);
            oclose(ps2);
            oclose(con1);
        }

        Connection con2 = null;
        PreparedStatement ps3 = null;
        ResultSet re3 = null;
        try {
            con2 = pool.getConnection();
            ps3 = con2.prepareStatement("select count(*) from " + TEST_TABLE + " where TEST_ID='" + userId + "'");
            re3 = ps3.executeQuery();
            if (re3.next())
                Assert.assertEquals(0, re3.getInt(1));
        } finally {
            oclose(re3);
            oclose(ps3);
            oclose(con2);
        }

        pool.close();
    }

    public void testAutoCommitChange() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setDefaultAutoCommit(false);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        Connection con1 = null;
        PreparedStatement ps1 = null;
        ResultSet re1 = null;
        try {
            con1 = pool.getConnection();
            String userId = String.valueOf(new Random(Long.MAX_VALUE).nextLong());
            ps1 = con1
                    .prepareStatement("select count(*) from " + TEST_TABLE + " where TEST_ID='" + userId + "'");
            re1 = ps1.executeQuery();
            try {
                con1.setAutoCommit(true);
                fail("AutoCommit reset false before rollback or commit");
            } catch (SQLException e) {
                Assert.assertEquals("Change forbidden when in transaction", e.getMessage());
            }
        } finally {
            oclose(re1);
            oclose(ps1);
            oclose(con1);
        }
        pool.close();
    }

    public void testRollback() throws Exception {
        BeeDataSourceConfig config = createDefault();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        Connection con1 = null;
        PreparedStatement ps1 = null;
        ResultSet re1 = null;
        PreparedStatement ps2 = null;

        PreparedStatement ps3 = null;
        ResultSet re3 = null;
        try {
            con1 = pool.getConnection();
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
            oclose(re1);
            oclose(ps1);
            oclose(ps2);
            oclose(re3);
            oclose(ps3);
            oclose(con1);
        }

        pool.close();
    }

    public void testSavePointRollback() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setDefaultAutoCommit(true);
        config.setInitialSize(0);
        config.setMaxActive(1);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        try (Connection conn1 = pool.getConnection()) {//
            conn1.setAutoCommit(false);//
            conn1.createStatement().execute(
                    "INSERT INTO data (type, payload) VALUES ('a', '{}'::jsonb)");

            Savepoint point1 = conn1.setSavepoint();
            conn1.rollback(point1);

            /*
             * Key info
             * 1: rollback to the point1,it is no effect on the previous executing statement.
             * 2: terminate a traction, need call <method>commit</method> or <method>rollback</method>
             */
        }
        //I think that bee connection will reset to default and rollback

        try (Connection conn2 = pool.getConnection()) {
            Assert.assertTrue(conn2.getAutoCommit());
        }

        pool.close();
    }
}
