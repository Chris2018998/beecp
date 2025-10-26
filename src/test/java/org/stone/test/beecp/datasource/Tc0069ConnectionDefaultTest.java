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
import org.stone.beecp.BeeDataSourceCreationException;
import org.stone.beecp.pool.exception.ConnectionDefaultSetFailedException;
import org.stone.test.base.LogCollector;
import org.stone.test.beecp.driver.MockConnectionProperties;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.test.base.LogCollector.startLogCollector;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0069ConnectionDefaultTest {

    @Test
    public void testUseDefault() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setUseDefaultSchema(true);
        config.setUseDefaultCatalog(true);
        config.setUseDefaultReadOnly(true);
        config.setUseDefaultAutoCommit(true);
        config.setUseDefaultTransactionIsolation(true);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection con = ds.getConnection()) {
                Assertions.assertFalse(con.isReadOnly());
                Assertions.assertTrue(con.getAutoCommit());
                Assertions.assertEquals(Connection.TRANSACTION_READ_COMMITTED, con.getTransactionIsolation());
                Assertions.assertNull(con.getSchema());
                Assertions.assertNull(con.getCatalog());

                con.setReadOnly(true);
                con.setAutoCommit(false);
                con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                con.setSchema("schema");
                con.setCatalog("catalog");
            }

            try (Connection con = ds.getConnection()) {//check reset
                Assertions.assertFalse(con.isReadOnly());
                Assertions.assertTrue(con.getAutoCommit());
                Assertions.assertEquals(Connection.TRANSACTION_READ_COMMITTED, con.getTransactionIsolation());
                Assertions.assertNull(con.getSchema());
                Assertions.assertNull(con.getCatalog());
            }
        }

        //set defaults to config
        config.setDefaultReadOnly(Boolean.TRUE);
        config.setDefaultAutoCommit(Boolean.FALSE);
        config.setDefaultTransactionIsolation(Integer.valueOf(Connection.TRANSACTION_READ_UNCOMMITTED));
        config.setDefaultSchema("schema");
        config.setDefaultCatalog("catalog");
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection con = ds.getConnection()) {
                Assertions.assertTrue(con.isReadOnly());
                Assertions.assertFalse(con.getAutoCommit());
                Assertions.assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con.getTransactionIsolation());
                Assertions.assertEquals("schema", con.getSchema());
                Assertions.assertEquals("catalog", con.getCatalog());

                con.setReadOnly(false);
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                con.setSchema(null);
                con.setCatalog(null);
            }

            try (Connection con = ds.getConnection()) {
                Assertions.assertTrue(con.isReadOnly());
                Assertions.assertFalse(con.getAutoCommit());
                Assertions.assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con.getTransactionIsolation());
                Assertions.assertEquals("schema", con.getSchema());
                Assertions.assertEquals("catalog", con.getCatalog());
            }
        }
    }

    @Test
    public void testDisableDefault() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setUseDefaultSchema(false);
        config.setUseDefaultCatalog(false);
        config.setUseDefaultReadOnly(false);
        config.setUseDefaultAutoCommit(false);
        config.setUseDefaultTransactionIsolation(false);

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

    @Test
    public void testDefaultSetFailed() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLogs(true);
        config.setUseDefaultCatalog(true);
        config.setUseDefaultSchema(true);
        config.setUseDefaultReadOnly(true);
        config.setUseDefaultAutoCommit(true);
        config.setUseDefaultTransactionIsolation(true);
        config.setDefaultReadOnly(true);
        config.setDefaultAutoCommit(true);
        config.setDefaultTransactionIsolation(1);
        config.setDefaultSchema("schema");
        config.setDefaultCatalog("catalog");

        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setMockException1(new SQLException("Communication failed"));
        MockConnectionFactory factory = new MockConnectionFactory(connectionProperties);
        config.setConnectionFactory(factory);

        //setAutoCommit
        connectionProperties.throwsExceptionWhenCallMethod("setAutoCommit,setTransactionIsolation,setReadOnly,setCatalog,setSchema");
        try (BeeDataSource ignored = new BeeDataSource(config)) {
            Assertions.fail("[testDefaultSetFailed]Test failed");
        } catch (BeeDataSourceCreationException e) {
            Assertions.assertInstanceOf(ConnectionDefaultSetFailedException.class, e.getCause());
        }

        //setTransactionIsolation
        connectionProperties.clearExceptionableMethod("setAutoCommit");
        connectionProperties.throwsExceptionWhenCallMethod("setTransactionIsolation,setReadOnly,setCatalog,setSchema");
        try (BeeDataSource ignored = new BeeDataSource(config)) {
            Assertions.fail("[testDefaultSetFailed]Test failed");
        } catch (BeeDataSourceCreationException e) {
            Assertions.assertInstanceOf(ConnectionDefaultSetFailedException.class, e.getCause());
        }

        //setReadOnly
        connectionProperties.clearExceptionableMethod("setAutoCommit,setTransactionIsolation");
        connectionProperties.throwsExceptionWhenCallMethod("setReadOnly,setCatalog,setSchema");
        try (BeeDataSource ignored = new BeeDataSource(config)) {
            Assertions.fail("[testDefaultSetFailed]Test failed");
        } catch (BeeDataSourceCreationException e) {
            Assertions.assertInstanceOf(ConnectionDefaultSetFailedException.class, e.getCause());
        }

        //setCatalog
        connectionProperties.clearExceptionableMethod("setAutoCommit,setTransactionIsolation,setReadOnly");
        connectionProperties.throwsExceptionWhenCallMethod("setCatalog,setSchema");
        try (BeeDataSource ignored = new BeeDataSource(config)) {
            Assertions.fail("[testDefaultSetFailed]Test failed");
        } catch (BeeDataSourceCreationException e) {
            Assertions.assertInstanceOf(ConnectionDefaultSetFailedException.class, e.getCause());
        }

        //setSchema
        connectionProperties.clearExceptionableMethod("setAutoCommit,setTransactionIsolation,setReadOnly,setTransactionIsolation");
        connectionProperties.throwsExceptionWhenCallMethod("setSchema");
        try (BeeDataSource ignored = new BeeDataSource(config)) {
            Assertions.fail("[testDefaultSetFailed]Test failed");
        } catch (BeeDataSourceCreationException e) {
            Assertions.assertInstanceOf(ConnectionDefaultSetFailedException.class, e.getCause());
        }

        connectionProperties.clearExceptionableMethod("setAutoCommit,setTransactionIsolation,setReadOnly,setTransactionIsolation,setCatalog");
        connectionProperties.throwsExceptionWhenCallMethod("setSchema");
        try (BeeDataSource ignored = new BeeDataSource(config)) {
            Assertions.fail("[testDefaultSetFailed]Test failed");
        } catch (BeeDataSourceCreationException e) {
            Assertions.assertInstanceOf(ConnectionDefaultSetFailedException.class, e.getCause());
        }
    }

    @Test
    public void testNotSetDefaultValue() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLogs(true);
        config.setUseDefaultCatalog(true);
        config.setUseDefaultSchema(true);
        config.setUseDefaultReadOnly(true);
        config.setUseDefaultAutoCommit(true);
        config.setUseDefaultTransactionIsolation(true);

        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setMockException1(new SQLException("Communication failed"));
        connectionProperties.throwsExceptionWhenCallMethod("getAutoCommit,isReadOnly,getTransactionIsolation,getCatalog,getSchema");
        MockConnectionFactory factory = new MockConnectionFactory(connectionProperties);
        config.setConnectionFactory(factory);

        LogCollector logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertNotNull(ds);
        }
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("failed to get value of auto-commit property"));
        Assertions.assertTrue(logs.contains("failed to get value of transaction-isolation property "));
        Assertions.assertTrue(logs.contains("failed to get value of read-only property"));
        Assertions.assertTrue(logs.contains("failed to get value of catalog property"));
        Assertions.assertTrue(logs.contains("failed to get value of schema property"));
        Assertions.assertTrue(logs.contains("as default value of auto-commit property"));
        Assertions.assertTrue(logs.contains("as default value of transaction-isolation property"));
        Assertions.assertTrue(logs.contains("as default value of read-only property"));

        //not print logs
        config.setPrintRuntimeLogs(false);
        logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertNotNull(ds);
        }
        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.contains("failed to get value of auto-commit property"));
        Assertions.assertFalse(logs.contains("failed to get value of transaction-isolation property "));
        Assertions.assertFalse(logs.contains("failed to get value of read-only property"));
        Assertions.assertFalse(logs.contains("failed to get value of catalog property"));
        Assertions.assertFalse(logs.contains("failed to get value of schema property"));
        Assertions.assertFalse(logs.contains("as default value of auto-commit property"));
        Assertions.assertFalse(logs.contains("as default value of transaction-isolation property"));
        Assertions.assertFalse(logs.contains("as default value of read-only property"));
    }

    //@Test
    public void testSetDefaultValue() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLogs(true);
        config.setUseDefaultCatalog(true);
        config.setUseDefaultSchema(true);
        config.setUseDefaultReadOnly(true);
        config.setUseDefaultAutoCommit(true);
        config.setUseDefaultTransactionIsolation(true);
        //set defaults to config
        config.setDefaultReadOnly(true);
        config.setDefaultAutoCommit(true);
        config.setDefaultTransactionIsolation(1);
        config.setDefaultSchema("schema");
        config.setDefaultCatalog("catalog");

        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setMockException1(new SQLException("Communication failed"));
        connectionProperties.throwsExceptionWhenCallMethod("getAutoCommit,setAutoCommit,isReadOnly,setReadOnly");
        connectionProperties.throwsExceptionWhenCallMethod("getTransactionIsolation,setTransactionIsolation");
        connectionProperties.throwsExceptionWhenCallMethod("setCatalog,getCatalog,setSchema,getSchema");
        MockConnectionFactory factory = new MockConnectionFactory(connectionProperties);
        config.setConnectionFactory(factory);


        LogCollector logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertNotNull(ds);
        }
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("of auto-commit property on first connection object"));
        Assertions.assertTrue(logs.contains("of transaction-isolation property on first connection object"));
        Assertions.assertTrue(logs.contains("of read-only property on first connection object"));
        Assertions.assertTrue(logs.contains("of catalog property on first connection object"));
        Assertions.assertTrue(logs.contains("of schema property on first connection object"));

        //not print logs
        config.setPrintRuntimeLogs(false);
        logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertNotNull(ds);
        }
        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.contains("of auto-commit property on first connection object"));
        Assertions.assertFalse(logs.contains("of transaction-isolation property on first connection object"));
        Assertions.assertFalse(logs.contains("of read-only property on first connection object"));
        Assertions.assertFalse(logs.contains("of catalog property on first connection object"));
        Assertions.assertFalse(logs.contains("of schema property on first connection object"));
    }

    @Test
    public void testSupportOnIsValidMethod() throws Exception {
        //return false from isValid
        BeeDataSourceConfig config1 = createDefault();
        config1.setInitialSize(1);
        config1.setPrintRuntimeLogs(true);
        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setValid(false);
        MockConnectionFactory factory = new MockConnectionFactory(connectionProperties);
        config1.setConnectionFactory(factory);
        LogCollector logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertNotNull(ds);
        }
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("get false from call of isValid method on first connection object"));

        //not print logs
        config1.setPrintRuntimeLogs(false);
        logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertNotNull(ds);
        }

        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.contains("isValid method tested failed on first connection object"));

        //exception from isValid
        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        config2.setPrintRuntimeLogs(true);
        connectionProperties.setValid(true);
        connectionProperties.throwsExceptionWhenCallMethod("isValid");
        connectionProperties.setMockException1(new SQLException("Communication failed"));
        config2.setConnectionFactory(factory);

        logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Assertions.assertNotNull(ds);
        }
        String logs2 = logCollector.endLogCollector();
        Assertions.assertTrue(logs2.contains("isValid method tested failed on first connection object"));

        //not print logs
        config2.setPrintRuntimeLogs(false);
        logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Assertions.assertNotNull(ds);
        }
        logs2 = logCollector.endLogCollector();
        Assertions.assertFalse(logs2.contains("isValid method tested failed on first connection object"));
    }

    @Test
    public void testSupportOnNetworkTimeoutMethod() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLogs(true);
        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setNetworkTimeout(-1);
        MockConnectionFactory factory = new MockConnectionFactory(connectionProperties);
        config.setConnectionFactory(factory);

        LogCollector logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertNotNull(ds);
        }
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("networkTimeout property not supported by connections due to a negative number returned from first connection object"));

        //not print logs
        config.setPrintRuntimeLogs(false);
        logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertNotNull(ds);
        }
        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.contains("networkTimeout property not supported by connections due to a negative number returned from first connection object"));

        //exception from  getNetworkTimeout
        connectionProperties.setNetworkTimeout(0);
        connectionProperties.throwsExceptionWhenCallMethod("getNetworkTimeout");
        connectionProperties.setMockException1(new SQLException("NetworkTimeout"));

        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        config2.setPrintRuntimeLogs(true);
        config2.setConnectionFactory(factory);
        logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Assertions.assertNotNull(ds);
        }
        String logs2 = logCollector.endLogCollector();
        Assertions.assertTrue(logs2.contains("networkTimeout property tested failed on first connection object"));

        //not print logs
        config2.setPrintRuntimeLogs(false);
        logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Assertions.assertNotNull(ds);
        }
        logs2 = logCollector.endLogCollector();
        Assertions.assertFalse(logs2.contains("networkTimeout property tested failed on first connection object"));

        //exception from setNetworkTimeout
        connectionProperties.setNetworkTimeout(10);
        connectionProperties.clearExceptionableMethod("getNetworkTimeout");
        connectionProperties.throwsExceptionWhenCallMethod("setNetworkTimeout");
        BeeDataSourceConfig config3 = createDefault();
        config3.setInitialSize(1);
        config3.setPrintRuntimeLogs(true);
        config3.setConnectionFactory(factory);

        logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config3)) {
            Assertions.assertNotNull(ds);
        }
        String logs3 = logCollector.endLogCollector();
        Assertions.assertTrue(logs3.contains("networkTimeout property tested failed on first connection object"));
    }

}
