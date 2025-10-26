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

import java.security.InvalidParameterException;
import java.util.Properties;

import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0008ProviderPropertiesTest {

    @Test
    public void testOnAddProperty() {
        BeeDataSourceConfig config = createEmpty();
        try {
            config.addConnectionProviderProperty(null, null);
            Assertions.fail("[testOnAddProperty]Test failed");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given key cannot be null or blank", e.getMessage());
        }
        try {
            config.addConnectionProviderProperty("", "value");
            Assertions.fail("[testOnAddProperty]Test failed");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given key cannot be null or blank", e.getMessage());
        }
        try {
            config.addConnectionProviderProperty(" ", "value");
            Assertions.fail("[testOnAddProperty]Test failed");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given key cannot be null or blank", e.getMessage());
        }

        config.addConnectionProviderProperty("key", null);
        Assertions.assertNull(config.getConnectionProviderProperty("key"));
        config.addConnectionProviderProperty("key", "value");
        Assertions.assertNotNull(config.getConnectionProviderProperty("key"));
    }

    @Test
    public void testOnRemoval() {
        BeeDataSourceConfig config = createEmpty();
        config.addConnectionProviderProperty("prop1", "value1");
        Assertions.assertEquals("value1", config.getConnectionProviderProperty("prop1"));
        Assertions.assertEquals("value1", config.removeConnectionProviderProperty("prop1"));
        Assertions.assertNull(config.getConnectionProviderProperty("prop1"));
    }

    @Test
    //prop1=value&prop2=value2&prop3=value3
    public void testOnAddTextProperty1() {
        BeeDataSourceConfig config = createEmpty();
        config.addConnectionProviderProperty("prop1=value1&prop2=value2&prop3=value3");

        Assertions.assertEquals("value1", config.getConnectionProviderProperty("prop1"));
        Assertions.assertEquals("value2", config.getConnectionProviderProperty("prop2"));
        Assertions.assertEquals("value3", config.getConnectionProviderProperty("prop3"));
    }

    @Test
    //prop1:value&prop2:value2&prop3:value3
    public void testOnAddTextProperty2() {
        BeeDataSourceConfig config = createEmpty();
        config.addConnectionProviderProperty("prop1:value1&prop2:value2&prop3:value3&prop4:value4:value5");

        Assertions.assertEquals("value1", config.getConnectionProviderProperty("prop1"));
        Assertions.assertEquals("value2", config.getConnectionProviderProperty("prop2"));
        Assertions.assertEquals("value3", config.getConnectionProviderProperty("prop3"));
        Assertions.assertNull(config.getConnectionProviderProperty("prop4"));
    }

    @Test
    public void testLoadFromProperties() {
        BeeDataSourceConfig config1 = createEmpty();
        Properties prop1 = new Properties();
        prop1.setProperty("connectionProviderProperties", "prop1=value1&prop2=value2&prop3=value3");
        config1.loadFromProperties(prop1);
        Assertions.assertEquals("value1", config1.getConnectionProviderProperty("prop1"));
        Assertions.assertEquals("value2", config1.getConnectionProviderProperty("prop2"));
        Assertions.assertEquals("value3", config1.getConnectionProviderProperty("prop3"));

        BeeDataSourceConfig config2 = createEmpty();
        Properties prop2 = new Properties();
        prop2.setProperty("connectionProviderProperties", "prop1:value1&prop2:value2&prop3:value3");
        config2.loadFromProperties(prop2);
        Assertions.assertEquals("value1", config2.getConnectionProviderProperty("prop1"));
        Assertions.assertEquals("value2", config2.getConnectionProviderProperty("prop2"));
        Assertions.assertEquals("value3", config2.getConnectionProviderProperty("prop3"));

        BeeDataSourceConfig config3 = createEmpty();
        Properties prop3 = new Properties();
        prop3.setProperty("connectionProviderProperties.size", "3");
        prop3.setProperty("connectionProviderProperties.1", "prop1=value1");
        prop3.setProperty("connectionProviderProperties.2", "prop2:value2");
        prop3.setProperty("connectionProviderProperties.3", "prop3=value3");
        config3.loadFromProperties(prop3);
        Assertions.assertEquals("value1", config3.getConnectionProviderProperty("prop1"));
        Assertions.assertEquals("value2", config3.getConnectionProviderProperty("prop2"));
        Assertions.assertEquals("value3", config3.getConnectionProviderProperty("prop3"));
    }
}
