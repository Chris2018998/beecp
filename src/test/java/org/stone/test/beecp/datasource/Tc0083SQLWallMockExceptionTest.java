/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.test.beecp.config.DsConfigFactory;
import org.stone.test.beecp.objects.listener.SQLWallConnectionPreparationListener;
import org.stone.test.beecp.objects.listener.SQLWallPreparedStatementExecutionListener;
import org.stone.test.beecp.objects.listener.SQLWallStatementExecutionListener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Chris Liao
 */
public class Tc0083SQLWallMockExceptionTest {

    @Test
    public void testExceptionFromConnectionPrepare() throws SQLException {
        String sql = "select * from system_users";
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setEnableLogCache(true);
        config.setLogListener(new SQLWallConnectionPreparationListener(sql));
        config.setInitialSize(1);
        config.setMaxActive(1);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection con = ds.getConnection()) {
                try (PreparedStatement ignored = con.prepareStatement(sql)) {
                    Assertions.fail("[Tc0081SQLExecutionLogTest.testExceptionFromConnectionPrepare]test failed");
                } catch (SQLException e) {
                    Assertions.assertEquals("SQL check failed in Wall", e.getMessage());
                }

                try (PreparedStatement ignored = con.prepareCall(sql)) {
                    Assertions.fail("[Tc0081SQLExecutionLogTest.testExceptionFromConnectionPrepare]test failed");
                } catch (SQLException e) {
                    Assertions.assertEquals("SQL check failed in Wall", e.getMessage());
                }
            }
        }
    }

    @Test
    public void testExceptionFromPreparedStatementExecution() throws SQLException {
        String sql = "select * from system_users";
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setEnableLogCache(true);
        config.setLogListener(new SQLWallPreparedStatementExecutionListener(sql));
        config.setInitialSize(1);
        config.setMaxActive(1);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection con = ds.getConnection()) {
                try (PreparedStatement ignored = con.prepareStatement(sql)) {
                    try {
                        ignored.execute();
                        Assertions.fail("[Tc0081SQLExecutionLogTest.testExceptionFromPreparedStatementExecution]test failed");
                    } catch (SQLException e) {
                        Assertions.assertEquals("SQL check failed in Wall", e.getMessage());
                    }

                    try {
                        ignored.executeUpdate();
                        Assertions.fail("[Tc0081SQLExecutionLogTest.testExceptionFromPreparedStatementExecution]test failed");
                    } catch (SQLException e) {
                        Assertions.assertEquals("SQL check failed in Wall", e.getMessage());
                    }

                    try {
                        ignored.executeQuery();
                        Assertions.fail("[Tc0081SQLExecutionLogTest.testExceptionFromPreparedStatementExecution]test failed");
                    } catch (SQLException e) {
                        Assertions.assertEquals("SQL check failed in Wall", e.getMessage());
                    }
                } catch (SQLException e) {
                    Assertions.assertEquals("SQL check failed in Wall", e.getMessage());
                }
            }
        }
    }

    @Test
    public void testExceptionFromStatementExecution() throws SQLException {
        String sql = "select * from system_users";
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setEnableLogCache(true);
        config.setLogListener(new SQLWallStatementExecutionListener(sql));
        config.setInitialSize(1);
        config.setMaxActive(1);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection con = ds.getConnection()) {
                try (Statement ignored = con.createStatement()) {
                    try {
                        ignored.execute(sql);
                        Assertions.fail("[Tc0081SQLExecutionLogTest.testExceptionFromStatementExecution]test failed");
                    } catch (SQLException e) {
                        Assertions.assertEquals("SQL check failed in Wall", e.getMessage());
                    }

                    try {
                        ignored.executeQuery(sql);
                        Assertions.fail("[Tc0081SQLExecutionLogTest.testExceptionFromStatementExecution]test failed");
                    } catch (SQLException e) {
                        Assertions.assertEquals("SQL check failed in Wall", e.getMessage());
                    }

                    try {
                        ignored.executeUpdate(sql);
                        Assertions.fail("[Tc0081SQLExecutionLogTest.testExceptionFromStatementExecution]test failed");
                    } catch (SQLException e) {
                        Assertions.assertEquals("SQL check failed in Wall", e.getMessage());
                    }
                } catch (SQLException e) {
                    Assertions.assertEquals("SQL check failed in Wall", e.getMessage());
                }
            }
        }
    }
}
