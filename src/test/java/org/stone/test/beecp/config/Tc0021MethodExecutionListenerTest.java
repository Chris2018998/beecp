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
import org.stone.beecp.exception.BeeDataSourceConfigException;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;
import org.stone.test.beecp.objects.listener.MockMethodExecutionListener1;
import org.stone.test.beecp.objects.listener.MockMethodExecutionListener2;
import org.stone.tools.exception.BeanException;

/**
 * @author Chris Liao
 */

public class Tc0021MethodExecutionListenerTest {

    @Test
    public void testSetAndGet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Assertions.assertNull(config.getLogListener());//default check
        config.setLogListener(new MockMethodExecutionListener1());
        Assertions.assertNotNull(config.getLogListener());//default check
        config.setLogListener(null);
        Assertions.assertNull(config.getLogListener());//default check

        Assertions.assertNull(config.getLogListenerClass());//default check
        config.setLogListenerClass(MockMethodExecutionListener1.class);
        Assertions.assertNotNull(config.getLogListenerClass());
        config.setLogListenerClass(null);
        Assertions.assertNull(config.getLogListenerClass());

        Assertions.assertNull(config.getLogListenerClassName());//default check
        config.setLogListenerClassName(MockMethodExecutionListener1.class.getName());
        Assertions.assertNotNull(config.getLogListenerClassName());
        config.setLogListenerClassName(null);
        Assertions.assertNull(config.getLogListenerClassName());
    }

    @Test
    public void testCheckPassed() throws Exception {
        MockConnectionFactory connectionFactory = new MockConnectionFactory();

        //1: instance test
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.setConnectionFactory(connectionFactory);
        config1.setLogListener(new MockMethodExecutionListener1());
        try {
            BeeDataSourceConfig checkedOConfig = config1.check();
            Assertions.assertNotNull(checkedOConfig.getLogListener());
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }

        //2: class test
        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setConnectionFactory(connectionFactory);
        config2.setLogListenerClass(MockMethodExecutionListener1.class);
        try {
            BeeDataSourceConfig checkedOConfig = config2.check();
            Assertions.assertNotNull(checkedOConfig.getLogListener());
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }

        //3: class name test
        BeeDataSourceConfig config3 = new BeeDataSourceConfig();
        config3.setConnectionFactory(connectionFactory);
        config3.setLogListenerClassName(MockMethodExecutionListener1.class.getName());
        try {
            BeeDataSourceConfig checkedOConfig = config3.check();
            Assertions.assertNotNull(checkedOConfig.getLogListener());
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }
    }

    @Test
    public void testCheckFailed() throws Exception {
        MockConnectionFactory connectionFactory = new MockConnectionFactory();

        //1:Class test（No parameterized constructor）
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.setConnectionFactory(connectionFactory);
        config1.setLogListenerClass(MockMethodExecutionListener2.class);
        try {
            config1.check();
            Assertions.fail("[testCheckFailed]Test failed");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(BeanException.class, e.getCause());
            Assertions.assertEquals("Failed to create instance on class[" + MockMethodExecutionListener2.class.getName() + "]", e.getCause().getMessage());
        }

        //2.1:Class name test（No parameterized constructor）
        BeeDataSourceConfig config21 = new BeeDataSourceConfig();
        config21.setConnectionFactory(connectionFactory);
        config21.setLogListenerClassName(MockMethodExecutionListener2.class.getName());
        try {
            config21.check();
            Assertions.fail("[testCheckFailed]Test failed");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(BeanException.class, e.getCause());
            Assertions.assertEquals("Failed to create instance on class[" + MockMethodExecutionListener2.class.getName() + "]", e.getCause().getMessage());
        }

        //2.2 class not found
        BeeDataSourceConfig config22 = new BeeDataSourceConfig();
        config22.setConnectionFactory(connectionFactory);
        config22.setLogListenerClassName(MockMethodExecutionListener2.class.getName() + "_NotFound");
        try {
            config22.check();
            Assertions.fail("[testCheckFailed]Test failed");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(ClassNotFoundException.class, e.getCause());
            Assertions.assertEquals("Failed to create method execution listener with class[" + MockMethodExecutionListener2.class.getName() + "_NotFound" + "]", e.getMessage());
        }

        //2.3 class type check
        BeeDataSourceConfig config23 = new BeeDataSourceConfig();
        config23.setConnectionFactory(connectionFactory);
        config23.setLogListenerClassName(String.class.getName());//not implementation
        try {
            config23.check();
            Assertions.fail("[testCheckFailed]Test failed");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(BeanException.class, e.getCause());
            Assertions.assertTrue(e.getCause().getMessage().contains("which must extend from one of type"));
        }
    }
}
