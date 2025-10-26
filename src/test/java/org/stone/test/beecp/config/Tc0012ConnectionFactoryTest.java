/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeConnectionFactory;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.test.base.LogCollector;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;
import org.stone.test.beecp.objects.factory.MockXaConnectionFactory;

import java.sql.SQLException;

import static org.stone.test.base.LogCollector.startLogCollector;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;
import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0012ConnectionFactoryTest {

    @Test
    public void testSetAndGet() {
        BeeDataSourceConfig config = createEmpty();
        Class<? extends BeeConnectionFactory> factClass = MockConnectionFactory.class;
        config.setConnectionFactoryClass(factClass);
        Assertions.assertEquals(factClass, config.getConnectionFactoryClass());

        String factClassName = MockConnectionFactory.class.getName();
        config.setConnectionFactoryClassName(factClassName);
        Assertions.assertEquals(config.getConnectionFactoryClassName(), factClassName);

        BeeConnectionFactory connectionFactory = new MockConnectionFactory();
        config.setConnectionFactory(connectionFactory);
        Assertions.assertEquals(config.getConnectionFactory(), connectionFactory);

        MockXaConnectionFactory xaConnectionFactory = new MockXaConnectionFactory();
        config.setXaConnectionFactory(xaConnectionFactory);
        Assertions.assertEquals(config.getConnectionFactory(), xaConnectionFactory);
    }

    @Test
    public void testCheckFailed() throws SQLException {
        try {//error factory class
            BeeDataSourceConfig config = createDefault();
            config.setConnectionFactoryClass(String.class);//invalid config
            config.check();
            Assertions.fail("[testLoadFromProperties]not threw exception when check invalid connection factory class");
        } catch (BeeDataSourceConfigException e) {
            Throwable cause = e.getCause();
            Assertions.assertNotNull(cause);
            String message = cause.getMessage();
            Assertions.assertTrue(message != null && message.contains("which must extend from one of type"));
        }

        try {//error factory class name
            BeeDataSourceConfig config = createDefault();
            config.setConnectionFactoryClassName("java.lang.String");//invalid config
            config.check();
            Assertions.fail("[testLoadFromProperties]not threw exception when check invalid connection factory class name");
        } catch (BeeDataSourceConfigException e) {
            Throwable cause = e.getCause();
            Assertions.assertNotNull(cause);
            String message = cause.getMessage();
            Assertions.assertTrue(message != null && message.contains("which must extend from one of type"));
        }

        try {//not found factory class
            BeeDataSourceConfig config = createDefault();
            config.setConnectionFactoryClassName("xx.xx.xx");//class not found
            config.check();
            Assertions.fail("[testLoadFromProperties]not threw exception when not found class name of connection factory");
        } catch (BeeDataSourceConfigException e) {
            Throwable cause = e.getCause();
            Assertions.assertInstanceOf(ClassNotFoundException.class, cause);
        }
    }

    @Test
    public void testCheckPassed() throws Exception {
        //1: instance
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        MockConnectionFactory connectionFactory = new MockConnectionFactory();
        config1.setConnectionFactory(connectionFactory);
        try {
            BeeDataSourceConfig checkedConfig = config1.check();
            Assertions.assertEquals(connectionFactory, checkedConfig.getConnectionFactory());
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }

        //2: class
        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setConnectionFactoryClass(MockConnectionFactory.class);
        try {
            config2.check();
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }

        //3: class name
        BeeDataSourceConfig config3 = new BeeDataSourceConfig();
        config3.setConnectionFactoryClassName(MockConnectionFactory.class.getName());
        try {
            config3.check();
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }
    }

    @Test
    public void testAbandoningJdbcLinkInfo() throws SQLException {
        MockConnectionFactory connectionFactory = new MockConnectionFactory();
        BeeDataSourceConfig config1 = createEmpty();
        config1.setUsername(DsConfigFactory.JDBC_USER);
        config1.setConnectionFactory(connectionFactory);
        LogCollector logCollector = startLogCollector();
        BeeDataSourceConfig checkedConfig = config1.check();
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("configured jdbc link info abandoned according that a connection factory has been existed"));
        Assertions.assertNull(checkedConfig.getUsername());

        BeeDataSourceConfig config2 = createEmpty();
        config2.setPassword(DsConfigFactory.JDBC_PASSWORD);
        config2.setConnectionFactory(connectionFactory);

        logCollector = startLogCollector();
        checkedConfig = config2.check();
        logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("configured jdbc link info abandoned according that a connection factory has been existed"));
        Assertions.assertNull(checkedConfig.getPassword());

        BeeDataSourceConfig config3 = createEmpty();
        config3.setUrl(DsConfigFactory.MOCK_URL);
        config3.setConnectionFactory(connectionFactory);

        logCollector = startLogCollector();
        checkedConfig = config3.check();
        logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("configured jdbc link info abandoned according that a connection factory has been existed"));
        Assertions.assertNull(checkedConfig.getUrl());

        BeeDataSourceConfig config4 = createEmpty();
        config4.setDriverClassName(DsConfigFactory.MOCK_DRIVER);
        config4.setConnectionFactory(connectionFactory);

        logCollector = startLogCollector();
        checkedConfig = config4.check();
        logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("configured jdbc link info abandoned according that a connection factory has been existed"));
        Assertions.assertNull(checkedConfig.getDriverClassName());
    }
}
