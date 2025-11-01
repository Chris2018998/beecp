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
import org.stone.test.base.LogCollector;
import org.stone.test.beecp.driver.MockConnectionProperties;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0069ConnectionDefaultTest {

    @Test
    public void testInitExceptionOnAutoCommit() {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(1);
        config.setEnableDefaultAutoCommit(true);

        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setMockException1(new SQLException("Communication failed"));
        MockConnectionFactory factory = new MockConnectionFactory(connectionProperties);
        config.setConnectionFactory(factory);
        connectionProperties.throwsExceptionWhenCallMethod("getAutoCommit");
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("[testAutoCommitInitException]Test failed");
            } catch (SQLException e) {
                Assertions.assertEquals("Failed to get default value of 'auto-commit' from initial test connection", e.getMessage());
            }
        }

        connectionProperties.clearExceptionableMethod("getAutoCommit");
        connectionProperties.throwsExceptionWhenCallMethod("setAutoCommit");
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("[testAutoCommitInitException]Test failed");
            } catch (SQLException e) {
                Assertions.assertEquals("Failed to set default value(" + true + ") of 'auto-commit' to initial test connection", e.getMessage());
            }
        }
    }

    @Test
    public void testInitExceptionOnTransactionIsolation() {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(1);
        config.setEnableDefaultTransactionIsolation(true);

        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setMockException1(new SQLException("Communication failed"));
        MockConnectionFactory factory = new MockConnectionFactory(connectionProperties);
        config.setConnectionFactory(factory);
        connectionProperties.throwsExceptionWhenCallMethod("getTransactionIsolation");
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("[testInitExceptionOnTransactionIsolation]Test failed");
            } catch (SQLException e) {
                Assertions.assertEquals("Failed to get default value of 'transaction-isolation' from initial test connection", e.getMessage());
            }
        }

        connectionProperties.clearExceptionableMethod("getTransactionIsolation");
        connectionProperties.throwsExceptionWhenCallMethod("setTransactionIsolation");
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("[testInitExceptionOnTransactionIsolation]Test failed");
            } catch (SQLException e) {
                Assertions.assertEquals("Failed to set default value(" + Connection.TRANSACTION_READ_COMMITTED + ") of 'transaction-isolation' to initial test connection", e.getMessage());
            }
        }
    }

    @Test
    public void testInitExceptionOnReadOnly() {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(1);
        config.setEnableDefaultReadOnly(true);

        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setMockException1(new SQLException("Communication failed"));
        MockConnectionFactory factory = new MockConnectionFactory(connectionProperties);
        config.setConnectionFactory(factory);
        connectionProperties.throwsExceptionWhenCallMethod("isReadOnly");
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("[testInitExceptionOnReadOnly]Test failed");
            } catch (SQLException e) {
                Assertions.assertEquals("Failed to get default value of 'read-only' from initial test connection", e.getMessage());
            }
        }

        connectionProperties.clearExceptionableMethod("isReadOnly");
        connectionProperties.throwsExceptionWhenCallMethod("setReadOnly");
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("[testInitExceptionOnReadOnly]Test failed");
            } catch (SQLException e) {
                Assertions.assertEquals("Failed to set default value(" + false + ") of 'read-only' to initial test connection", e.getMessage());
            }
        }
    }

    @Test
    public void testInitExceptionOnCatalog() {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(1);
        config.setEnableDefaultCatalog(true);

        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setMockException1(new SQLException("Communication failed"));
        MockConnectionFactory factory = new MockConnectionFactory(connectionProperties);
        config.setConnectionFactory(factory);
        connectionProperties.throwsExceptionWhenCallMethod("getCatalog");
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("[testInitExceptionOnReadOnly]Test failed");
            } catch (SQLException e) {
                Assertions.assertEquals("Failed to get default value of 'catalog' from initial test connection", e.getMessage());
            }
        }

        String catalog = "testCatLog";
        connectionProperties.setCatalog(catalog);
        connectionProperties.clearExceptionableMethod("getCatalog");
        connectionProperties.throwsExceptionWhenCallMethod("setCatalog");
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("[testInitExceptionOnReadOnly]Test failed");
            } catch (SQLException e) {
                Assertions.assertEquals("Failed to set default value(" + catalog + ") of 'catalog' to initial test connection", e.getMessage());
            }
        }
    }

    @Test
    public void testInitExceptionOnSchema() {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(1);
        config.setEnableDefaultSchema(true);

        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setMockException1(new SQLException("Communication failed"));
        MockConnectionFactory factory = new MockConnectionFactory(connectionProperties);
        config.setConnectionFactory(factory);
        connectionProperties.throwsExceptionWhenCallMethod("getSchema");
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("[testInitExceptionOnSchema]Test failed");
            } catch (SQLException e) {
                Assertions.assertEquals("Failed to get default value of 'schema' from initial test connection", e.getMessage());
            }
        }

        String schema = "testSchema";
        connectionProperties.setSchema(schema);
        connectionProperties.clearExceptionableMethod("getSchema");
        connectionProperties.throwsExceptionWhenCallMethod("setSchema");
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("[testInitExceptionOnSchema]Test failed");
            } catch (SQLException e) {
                Assertions.assertEquals("Failed to set default value(" + schema + ") of 'schema' to initial test connection", e.getMessage());
            }
        }
    }

    @Test
    public void testInitExceptionOnValid() {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(1);
        config.setPrintRuntimeLogs(true);

        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setMockException1(new SQLException("Communication failed"));
        MockConnectionFactory factory = new MockConnectionFactory(connectionProperties);
        config.setConnectionFactory(factory);
        connectionProperties.throwsExceptionWhenCallMethod("isValid");
        try (BeeDataSource ds = new BeeDataSource(config)) {
            LogCollector logCollector = LogCollector.startLogCollector();
            try (Connection con = ds.getConnection()) {
                Assertions.assertNotNull(con);
            } catch (SQLException e) {
                Assertions.fail("[testInitExceptionOnValid]Test failed");
            }
            String logInfo = logCollector.endLogCollector();
            Assertions.assertTrue(logInfo.contains("Exception occurred when call 'isValid' method on initial test connection"));
        }

        connectionProperties.clearExceptionableMethod("isValid");
        connectionProperties.setValid(false);
        try (BeeDataSource ds = new BeeDataSource(config)) {
            LogCollector logCollector = LogCollector.startLogCollector();
            try (Connection con = ds.getConnection()) {
                Assertions.assertNotNull(con);
            } catch (SQLException e) {
                Assertions.fail("[testInitExceptionOnValid]Test failed");
            }
            String logInfo = logCollector.endLogCollector();
            Assertions.assertTrue(logInfo.contains("Driver not support 'isValid' method call on connection"));
        }
    }


    @Test
    public void testInitExceptionOnNetworkTimeout() {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(1);
        config.setPrintRuntimeLogs(true);

        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setMockException1(new SQLException("Communication failed"));
        MockConnectionFactory factory = new MockConnectionFactory(connectionProperties);
        config.setConnectionFactory(factory);

        connectionProperties.setNetworkTimeout(-1);
        try (BeeDataSource ds = new BeeDataSource(config)) {
            LogCollector logCollector = LogCollector.startLogCollector();
            try (Connection con = ds.getConnection()) {
                Assertions.assertNotNull(con);
            } catch (SQLException e) {
                Assertions.fail("[testInitExceptionOnValid]Test failed");
            }
            String logInfo = logCollector.endLogCollector();
            Assertions.assertTrue(logInfo.contains("Driver not support 'getNetworkTimeout()/setNetworkTimeout(time)' method call on connection"));
        }

        //test exception from 'getNetworkTimeout()'
        connectionProperties.throwsExceptionWhenCallMethod("getNetworkTimeout");
        try (BeeDataSource ds = new BeeDataSource(config)) {
            LogCollector logCollector = LogCollector.startLogCollector();
            try (Connection con = ds.getConnection()) {
                Assertions.assertNotNull(con);
            } catch (SQLException e) {
                Assertions.fail("[testInitExceptionOnValid]Test failed");
            }
            String logInfo = logCollector.endLogCollector();
            Assertions.assertTrue(logInfo.contains("Exception occurred when call 'getNetworkTimeout()/setNetworkTimeout(time)' method on initial test connection"));
        }

        //test exception from 'setNetworkTimeout()'
        connectionProperties.setNetworkTimeout(0);
        connectionProperties.clearExceptionableMethod("getNetworkTimeout");
        connectionProperties.throwsExceptionWhenCallMethod("setNetworkTimeout");
        try (BeeDataSource ds = new BeeDataSource(config)) {
            LogCollector logCollector = LogCollector.startLogCollector();
            try (Connection con = ds.getConnection()) {
                Assertions.assertNotNull(con);
            } catch (SQLException e) {
                Assertions.fail("[testInitExceptionOnValid]Test failed");
            }
            String logInfo = logCollector.endLogCollector();
            Assertions.assertTrue(logInfo.contains("Exception occurred when call 'getNetworkTimeout()/setNetworkTimeout(time)' method on initial test connection"));
        }
    }

    @Test
    public void testDisableDefault() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setEnableDefaultSchema(false);
        config.setEnableDefaultCatalog(false);
        config.setEnableDefaultReadOnly(false);
        config.setEnableDefaultAutoCommit(false);
        config.setEnableDefaultTransactionIsolation(false);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection con = ds.getConnection()) {
                Assertions.assertFalse(con.isReadOnly());
                Assertions.assertTrue(con.getAutoCommit());
                Assertions.assertEquals(Connection.TRANSACTION_READ_COMMITTED, con.getTransactionIsolation());
                Assertions.assertNull(con.getSchema());
                Assertions.assertNull(con.getCatalog());
            }
        }

        config.setDefaultAutoCommit(Boolean.FALSE);
        config.setDefaultReadOnly(Boolean.TRUE);
        config.setDefaultTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection con = ds.getConnection()) {
                Assertions.assertFalse(con.isReadOnly());
                Assertions.assertTrue(con.getAutoCommit());
                Assertions.assertEquals(Connection.TRANSACTION_READ_COMMITTED, con.getTransactionIsolation());
                Assertions.assertNull(con.getSchema());
                Assertions.assertNull(con.getCatalog());
            }
        }
    }
}
