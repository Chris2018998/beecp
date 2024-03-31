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

public class Case6_ConnectPropertiesTest extends TestCase {

    public void testOnAddProperty() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.addConnectProperty(null, null);
        config.addConnectProperty(null, "value");
        config.addConnectProperty("key", null);
        config.addConnectProperty("key", "value");
    }

    public void testOnRemoval() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.addConnectProperty("prop1", "value1");
        Assert.assertEquals(config.getConnectProperty("prop1"), "value1");
        config.removeConnectProperty("prop1");
        Assert.assertNull(config.getConnectProperty("prop1"));
    }

    //prop1=value&prop2=value2&prop3=value3
    public void testOnAddTextProperty1() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.addConnectProperty("prop1=value1&prop2=value2&prop3=value3");

        Assert.assertEquals("value1", config.getConnectProperty("prop1"));
        Assert.assertEquals("value2", config.getConnectProperty("prop2"));
        Assert.assertEquals("value3", config.getConnectProperty("prop3"));
    }

    //prop1:value&prop2:value2&prop3:value3
    public void testOnAddTextProperty2() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.addConnectProperty("prop1:value1&prop2:value2&prop3:value3");

        Assert.assertEquals("value1", config.getConnectProperty("prop1"));
        Assert.assertEquals("value2", config.getConnectProperty("prop2"));
        Assert.assertEquals("value3", config.getConnectProperty("prop3"));
    }


}
