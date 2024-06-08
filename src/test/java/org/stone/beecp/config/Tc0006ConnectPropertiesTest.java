/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.config;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSourceConfig;

import java.util.Properties;

/**
 * @author Chris Liao
 */
public class Tc0006ConnectPropertiesTest extends TestCase {

    public void testOnAddProperty() {
        BeeDataSourceConfig config = DsConfigFactory.createEmpty();
        config.addConnectProperty(null, null);
        config.addConnectProperty(null, "value");
        config.addConnectProperty("key", null);
        config.addConnectProperty("key", "value");
    }

    public void testOnRemoval() {
        BeeDataSourceConfig config = DsConfigFactory.createEmpty();
        config.addConnectProperty("prop1", "value1");
        Assert.assertEquals(config.getConnectProperty("prop1"), "value1");
        Assert.assertEquals(config.removeConnectProperty("prop1"), "value1");
        Assert.assertNull(config.getConnectProperty("prop1"));
    }

    //prop1=value&prop2=value2&prop3=value3
    public void testOnAddTextProperty1() {
        BeeDataSourceConfig config = DsConfigFactory.createEmpty();
        config.addConnectProperty("prop1=value1&prop2=value2&prop3=value3");

        Assert.assertEquals("value1", config.getConnectProperty("prop1"));
        Assert.assertEquals("value2", config.getConnectProperty("prop2"));
        Assert.assertEquals("value3", config.getConnectProperty("prop3"));
    }

    //prop1:value&prop2:value2&prop3:value3
    public void testOnAddTextProperty2() {
        BeeDataSourceConfig config = DsConfigFactory.createEmpty();
        config.addConnectProperty("prop1:value1&prop2:value2&prop3:value3&prop4:value4:value5");

        Assert.assertEquals("value1", config.getConnectProperty("prop1"));
        Assert.assertEquals("value2", config.getConnectProperty("prop2"));
        Assert.assertEquals("value3", config.getConnectProperty("prop3"));
        Assert.assertNull("value4", config.getConnectProperty("prop4"));
    }

    public void testLoadFromProperties() {
        BeeDataSourceConfig config1 = DsConfigFactory.createEmpty();
        Properties prop1 = new Properties();
        prop1.setProperty("connectProperties", "prop1=value1&prop2=value2&prop3=value3");
        config1.loadFromProperties(prop1);
        Assert.assertEquals("value1", config1.getConnectProperty("prop1"));
        Assert.assertEquals("value2", config1.getConnectProperty("prop2"));
        Assert.assertEquals("value3", config1.getConnectProperty("prop3"));

        BeeDataSourceConfig config2 = DsConfigFactory.createEmpty();
        Properties prop2 = new Properties();
        prop2.setProperty("connectProperties", "prop1:value1&prop2:value2&prop3:value3");
        config2.loadFromProperties(prop2);
        Assert.assertEquals("value1", config2.getConnectProperty("prop1"));
        Assert.assertEquals("value2", config2.getConnectProperty("prop2"));
        Assert.assertEquals("value3", config2.getConnectProperty("prop3"));

        BeeDataSourceConfig config3 = DsConfigFactory.createEmpty();
        Properties prop3 = new Properties();
        prop3.setProperty("connectProperties.size", "3");
        prop3.setProperty("connectProperties.1", "prop1=value1");
        prop3.setProperty("connectProperties.2", "prop2:value2");
        prop3.setProperty("connectProperties.3", "prop3=value3");
        config3.loadFromProperties(prop3);
        Assert.assertEquals("value1", config3.getConnectProperty("prop1"));
        Assert.assertEquals("value2", config3.getConnectProperty("prop2"));
        Assert.assertEquals("value3", config3.getConnectProperty("prop3"));
    }
}
