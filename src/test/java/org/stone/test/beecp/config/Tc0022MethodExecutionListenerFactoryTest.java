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
import org.stone.test.beecp.objects.factory.MockConnectionFactory;
import org.stone.test.beecp.objects.listener.MockMethodExecutionListenerFactory1;
import org.stone.test.beecp.objects.listener.MockMethodExecutionListenerFactory2;
import org.stone.test.beecp.objects.listener.MockMethodExecutionListenerFactory3;
import org.stone.tools.exception.BeanException;

/**
 * @author Chris Liao
 */
public class Tc0022MethodExecutionListenerFactoryTest {

    @Test
    public void testSetAndGet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Assertions.assertNull(config.getMethodExecutionListener());//default check
        config.setMethodExecutionListenerFactory(new MockMethodExecutionListenerFactory1());
        Assertions.assertNotNull(config.getMethodExecutionListenerFactory());//default check
        config.setMethodExecutionListenerFactory(null);
        Assertions.assertNull(config.getMethodExecutionListenerFactory());//default check

        Assertions.assertNull(config.getMethodExecutionListenerFactoryClass());//default check
        config.setMethodExecutionListenerFactoryClass(MockMethodExecutionListenerFactory1.class);
        Assertions.assertNotNull(config.getMethodExecutionListenerFactoryClass());
        config.setMethodExecutionListenerFactoryClass(null);
        Assertions.assertNull(config.getMethodExecutionListenerFactoryClass());

        Assertions.assertNull(config.getMethodExecutionListenerFactoryClassName());//default check
        config.setMethodExecutionListenerFactoryClassName(MockMethodExecutionListenerFactory1.class.getName());
        Assertions.assertNotNull(config.getMethodExecutionListenerFactoryClassName());
        config.setMethodExecutionListenerFactoryClassName(null);
        Assertions.assertNull(config.getMethodExecutionListenerFactoryClassName());
    }

    @Test
    public void testCheckPassed() throws Exception {
        MockConnectionFactory connectionFactory = new MockConnectionFactory();

        //1: instance test
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.setConnectionFactory(connectionFactory);
        config1.setMethodExecutionListenerFactory(new MockMethodExecutionListenerFactory1());
        try {
            BeeDataSourceConfig checkedOConfig = config1.check();
            Assertions.assertNotNull(checkedOConfig.getMethodExecutionListener());
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }

        //2: class test
        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setConnectionFactory(connectionFactory);
        config2.setMethodExecutionListenerFactoryClass(MockMethodExecutionListenerFactory1.class);
        try {
            BeeDataSourceConfig checkedOConfig = config2.check();
            Assertions.assertNotNull(checkedOConfig.getMethodExecutionListener());
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }

        //3: class name test
        BeeDataSourceConfig config3 = new BeeDataSourceConfig();
        config3.setConnectionFactory(connectionFactory);
        config3.setMethodExecutionListenerFactoryClassName(MockMethodExecutionListenerFactory1.class.getName());
        try {
            BeeDataSourceConfig checkedOConfig = config3.check();
            Assertions.assertNotNull(checkedOConfig.getMethodExecutionListener());
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
        config1.setMethodExecutionListenerFactoryClass(MockMethodExecutionListenerFactory2.class);
        try {
            config1.check();
            Assertions.fail("[testCheckFailed]Test failed");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(BeanException.class, e.getCause());
            Assertions.assertEquals("Failed to create instance on class[" + MockMethodExecutionListenerFactory2.class.getName() + "]", e.getCause().getMessage());
        }

        //2.1:Class name test（No parameterized constructor）
        BeeDataSourceConfig config21 = new BeeDataSourceConfig();
        config21.setConnectionFactory(connectionFactory);
        config21.setMethodExecutionListenerFactoryClassName(MockMethodExecutionListenerFactory2.class.getName());
        try {
            config21.check();
            Assertions.fail("[testCheckFailed]Test failed");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(BeanException.class, e.getCause());
            Assertions.assertEquals("Failed to create instance on class[" + MockMethodExecutionListenerFactory2.class.getName() + "]", e.getCause().getMessage());
        }

        //2.2 class not found
        BeeDataSourceConfig config22 = new BeeDataSourceConfig();
        config22.setConnectionFactory(connectionFactory);
        config22.setMethodExecutionListenerFactoryClassName(MockMethodExecutionListenerFactory2.class.getName() + "_NotFound");
        try {
            config22.check();
            Assertions.fail("[testCheckFailed]Test failed");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(ClassNotFoundException.class, e.getCause());
            Assertions.assertEquals("Failed to create method execution listener factory with class[" + MockMethodExecutionListenerFactory2.class.getName() + "_NotFound" + "]", e.getMessage());
        }

        //2.3 class type check
        BeeDataSourceConfig config23 = new BeeDataSourceConfig();
        config23.setConnectionFactory(connectionFactory);
        config23.setMethodExecutionListenerFactoryClassName(String.class.getName());//not implementation
        try {
            config23.check();
            Assertions.fail("[testCheckFailed]Test failed");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(BeanException.class, e.getCause());
            Assertions.assertTrue(e.getCause().getMessage().contains("which must extend from one of type"));
        }
    }

    @Test
    public void testCreateListenerFailed() throws Exception {
        MockConnectionFactory connectionFactory = new MockConnectionFactory();
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.setConnectionFactory(connectionFactory);
        config1.setMethodExecutionListenerFactory(new MockMethodExecutionListenerFactory3());
        try {
            config1.check();
            Assertions.fail("[testCheckFailed]Test failed");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertEquals("Failed to create method execution listener by listener factory", e.getMessage());
        }

        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setConnectionFactory(connectionFactory);
        config2.setMethodExecutionListenerFactoryClass(MockMethodExecutionListenerFactory3.class);
        try {
            config2.check();
            Assertions.fail("[testCheckFailed]Test failed");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertEquals("Failed to create method execution listener by listener factory", e.getMessage());
        }
    }
}
