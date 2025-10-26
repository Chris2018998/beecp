/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.*;
import java.util.Random;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;
import static org.stone.test.beecp.config.DsConfigFactory.TEST_TABLE;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0067ConnectionTransactionTest {
    @Test
    public void testCloseConnectionNotCommited() throws Exception {
        BeeDataSourceConfig config = createDefault();
        try (BeeDataSource ds = new BeeDataSource(config)) {
            PreparedStatement ps1 = null;
            ResultSet re1 = null;
            PreparedStatement ps2 = null;
            String userId = String.valueOf(new Random(Long.MAX_VALUE).nextLong());

            try (Connection con1 = ds.getConnection()) {
                con1.setAutoCommit(false);
                ps1 = con1.prepareStatement("select count(*) from " + TEST_TABLE + " where TEST_ID='" + userId + "'");
                re1 = ps1.executeQuery();

                if (re1.next()) Assertions.assertEquals(0, re1.getInt(1));
                ps2 = con1.prepareStatement("insert into " + TEST_TABLE + "(TEST_ID,TEST_NAME)values(?,?)");
                ps2.setString(1, userId);
                ps2.setString(2, userId);
                Assertions.assertEquals(1, ps2.executeUpdate());
            } finally {
                oclose(re1);
                oclose(ps1);
                oclose(ps2);
            }

            PreparedStatement ps3 = null;
            ResultSet re3 = null;
            try (Connection con2 = ds.getConnection()) {

                ps3 = con2.prepareStatement("select count(*) from " + TEST_TABLE + " where TEST_ID='" + userId + "'");
                re3 = ps3.executeQuery();
                if (re3.next())
                    Assertions.assertEquals(0, re3.getInt(1));
            } finally {
                oclose(re3);
                oclose(ps3);
            }
        }
    }

    @Test
    public void testAutoCommitChange() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setDefaultAutoCommit(false);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            PreparedStatement ps1 = null;
            ResultSet re1 = null;
            try (Connection con1 = ds.getConnection()) {
                String userId = String.valueOf(new Random(Long.MAX_VALUE).nextLong());
                ps1 = con1
                        .prepareStatement("select count(*) from " + TEST_TABLE + " where TEST_ID='" + userId + "'");
                re1 = ps1.executeQuery();
                try {
                    con1.setAutoCommit(true);
                } catch (SQLException e) {
                    Assertions.assertEquals("Change forbidden when in transaction", e.getMessage());
                }
            } finally {
                oclose(re1);
                oclose(ps1);
            }
        }
    }

    @Test
    public void testRollback() throws Exception {
        BeeDataSourceConfig config = createDefault();
        try (BeeDataSource ds = new BeeDataSource(config)) {

            PreparedStatement ps1 = null;
            ResultSet re1 = null;
            PreparedStatement ps2 = null;

            PreparedStatement ps3 = null;
            ResultSet re3 = null;
            try (Connection con1 = ds.getConnection()) {
                con1.setAutoCommit(false);

                String userId = String.valueOf(new Random(Long.MAX_VALUE).nextLong());
                ps1 = con1
                        .prepareStatement("select count(*) from " + TEST_TABLE + " where TEST_ID='" + userId + "'");
                re1 = ps1.executeQuery();
                if (re1.next()) {
                    int size = re1.getInt(1);
                    Assertions.assertEquals(0, size);
                }

                ps2 = con1.prepareStatement("insert into " + TEST_TABLE + "(TEST_ID,TEST_NAME)values(?,?)");
                ps2.setString(1, userId);
                ps2.setString(2, userId);
                int rows = ps2.executeUpdate();
                Assertions.assertEquals(1, rows);
                con1.rollback();

                ps3 = con1
                        .prepareStatement("select count(*) from " + TEST_TABLE + " where TEST_ID='" + userId + "'");
                re3 = ps3.executeQuery();
                if (re3.next()) {
                    int size = re3.getInt(1);
                    Assertions.assertEquals(0, size);
                }
            } finally {
                oclose(re1);
                oclose(ps1);
                oclose(ps2);
                oclose(re3);
                oclose(ps3);
            }
        }
    }

    @Test
    public void testSavePointRollback() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setDefaultAutoCommit(true);
        config.setInitialSize(0);
        config.setMaxActive(1);
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection conn1 = ds.getConnection()) {
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

            try (Connection conn2 = ds.getConnection()) {
                Assertions.assertTrue(conn2.getAutoCommit());
            }
        }
    }
}
