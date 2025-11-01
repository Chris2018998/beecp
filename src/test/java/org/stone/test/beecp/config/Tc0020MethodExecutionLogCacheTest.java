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
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Chris Liao
 */

public class Tc0020MethodExecutionLogCacheTest {

    @Test
    public void testSetAndGet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Assertions.assertFalse(config.isEnableMethodExecutionLogCache());//default check
        config.setEnableMethodExecutionLogCache(true);
        Assertions.assertTrue(config.isEnableMethodExecutionLogCache());//default check

        //jdbcCallLogCacheSize
        Assertions.assertEquals(1000, config.getMethodExecutionLogCacheSize());//default check
        config.setMethodExecutionLogCacheSize(500);
        Assertions.assertEquals(500, config.getMethodExecutionLogCacheSize());
        try {
            config.setMethodExecutionLogCacheSize(0);
            fail("[testSetAndGet]Setting test failed on configuration item[log-cache-size]");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertEquals("The given value for configuration item 'method-execution-log-cache-size' must be greater than zero", e.getMessage());
        }
        try {
            config.setMethodExecutionLogCacheSize(-1);
            fail("[testSetAndGet]Setting test failed on configuration item[log-cache-size]");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertEquals("The given value for configuration item 'method-execution-log-cache-size' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(500, config.getMethodExecutionLogCacheSize());//not changed check

        //slowConnectionGetThreshold
        Assertions.assertEquals(30000L, config.getSlowConnectionThreshold());//default check
        config.setSlowConnectionThreshold(5000L);
        Assertions.assertEquals(5000L, config.getSlowConnectionThreshold());
        config.setSlowConnectionThreshold(0L);
        Assertions.assertEquals(0L, config.getSlowConnectionThreshold());
        try {
            config.setSlowConnectionThreshold(-1L);
            fail("[testSetAndGet]Setting test failed on configuration item[slow-connection-get-threshold]");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertEquals("The given value for configuration item 'slow-connection-threshold' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(0L, config.getSlowConnectionThreshold());//not changed check

        //slowSQLExecutionThreshold
        Assertions.assertEquals(30000L, config.getSlowSQLThreshold());//default check
        config.setSlowSQLThreshold(5000L);
        Assertions.assertEquals(5000L, config.getSlowSQLThreshold());
        config.setSlowSQLThreshold(0L);
        Assertions.assertEquals(0L, config.getSlowSQLThreshold());
        try {
            config.setSlowSQLThreshold(-1L);
            fail("[testSetAndGet]Setting test failed on configuration item[slow-SQL-execution-threshold]");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertEquals("The given value for configuration item 'slow-SQL-threshold' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(0L, config.getSlowSQLThreshold());//not changed check

        //jdbcCallLogTimeout
        Assertions.assertEquals(180000L, config.getMethodExecutionLogTimeout());//default check
        config.setMethodExecutionLogTimeout(5000L);
        Assertions.assertEquals(5000L, config.getMethodExecutionLogTimeout());
        try {
            config.setMethodExecutionLogTimeout(0L);
            fail("[testSetAndGet]Setting test failed on configuration item[log-timeout]");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertEquals("The given value for configuration item 'method-execution-log-timeout' must be greater than zero", e.getMessage());
        }
        try {
            config.setMethodExecutionLogTimeout(-1L);
            fail("[testSetAndGet]Setting test failed on configuration item[log-timeout]");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertEquals("The given value for configuration item 'method-execution-log-timeout' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(5000L, config.getMethodExecutionLogTimeout());//not changed check


        //jdbcCallLogTimeoutInterval
        Assertions.assertEquals(180000L, config.getIntervalOfClearTimeoutExecutionLogs());//default check
        config.setIntervalOfClearTimeoutExecutionLogs(5000L);
        Assertions.assertEquals(5000L, config.getIntervalOfClearTimeoutExecutionLogs());
        try {
            config.setIntervalOfClearTimeoutExecutionLogs(0L);
            fail("[testSetAndGet]Setting test failed on configuration item[log-clear-interval]");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertEquals("The given value for configuration item 'interval-of-clear-timeout-execution-logs' must be greater than zero", e.getMessage());
        }
        try {
            config.setIntervalOfClearTimeoutExecutionLogs(-1L);
            fail("[testSetAndGet]Setting test failed on configuration item[log-clear-interval]");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertEquals("The given value for configuration item 'interval-of-clear-timeout-execution-logs' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(5000L, config.getIntervalOfClearTimeoutExecutionLogs());//not changed check
    }

//    @Test
//    public void testCheckFailed() throws Exception {
//        MockConnectionFactory connectionFactory = new MockConnectionFactory();
//        BeeDataSourceConfig config1 = createEmpty();
//        config1.setConnectionFactory(connectionFactory);
//        config1.setEventLogManagerClassName(MockJdbcEventLogManager2.class.getName());//class can not be instantiated
//        try {
//            config1.check();
//            Assertions.fail("[testCheckFailed]Test failed");
//        } catch (BeeDataSourceConfigException e) {
//            Throwable cause1 = e.getCause();
//            Assertions.assertInstanceOf(BeanException.class, cause1);
//            Assertions.assertInstanceOf(NoSuchMethodException.class, cause1.getCause());
//        }
//
//        BeeDataSourceConfig config2 = createEmpty();
//        config2.setConnectionFactory(connectionFactory);
//        config2.setEventLogManagerClassName(MockJdbcEventLogManager2.class.getName() + "_NOT");//class not found
//        try {
//            config2.check();
//            Assertions.fail("[testCheckFailed]Test failed");
//        } catch (BeeDataSourceConfigException e) {
//            Assertions.assertInstanceOf(ClassNotFoundException.class, e.getCause());
//        }
//    }

//    @Test
//    public void testCheckPassed() throws Exception {
//        MockConnectionFactory connectionFactory = new MockConnectionFactory();
//
//        //1: instance
//        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
//        config1.setConnectionFactory(connectionFactory);
//        MockJdbcEventLogManager manager = new MockJdbcEventLogManager();
//        config1.setEventLogManager(manager);
//        try {
//            BeeDataSourceConfig checkedConfig = config1.check();
//            Assertions.assertEquals(manager, checkedConfig.getEventLogManager());
//        } catch (BeeDataSourceConfigException e) {
//            Assertions.fail("[testCheckPassed]Test failed");
//        }
//
//        //2: class
//        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
//        config2.setConnectionFactory(connectionFactory);
//        config2.setEventLogManagerClass(MockJdbcEventLogManager.class);
//        try {
//            config2.check();
//        } catch (BeeDataSourceConfigException e) {
//            Assertions.fail("[testCheckPassed]Test failed");
//        }
//
//        //3: class name
//        BeeDataSourceConfig config3 = new BeeDataSourceConfig();
//        config3.setConnectionFactory(connectionFactory);
//        config3.setEventLogManagerClassName(MockJdbcEventLogManager.class.getName());
//        try {
//            config3.check();
//        } catch (BeeDataSourceConfigException e) {
//            Assertions.fail("[testCheckPassed]Test failed");
//        }
//    }
}
