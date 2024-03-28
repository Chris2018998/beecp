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
import org.stone.base.TestException;
import org.stone.beecp.BeeDataSourceConfig;

public class ConnectPropertiesTest extends TestCase {

    public void testOnAddProperty() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.addConnectProperty(null, null);
        config.addConnectProperty(null, "value");
        config.addConnectProperty("key", null);
        config.addConnectProperty("key", "value");
    }

    //prop1=value&prop2=value2&prop3=value3
    public void testOnAddTextProperty1() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.addConnectProperty("prop1=value1&prop2=value2&prop3=value3");
        if (!"value1".equals(config.getConnectProperty("prop1"))) throw new TestException();
        if (!"value2".equals(config.getConnectProperty("prop2"))) throw new TestException();
        if (!"value3".equals(config.getConnectProperty("prop3"))) throw new TestException();
    }

    //prop1:value&prop2:value2&prop3:value3
    public void testOnAddTextProperty2() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.addConnectProperty("prop1:value1&prop2:value2&prop3:value3");
        if (!"value1".equals(config.getConnectProperty("prop1"))) throw new TestException();
        if (!"value2".equals(config.getConnectProperty("prop2"))) throw new TestException();
        if (!"value3".equals(config.getConnectProperty("prop3"))) throw new TestException();
    }

    public void testOnRemoval() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.addConnectProperty("prop1", "value1");
        if (!"value1".equals(config.getConnectProperty("prop1"))) throw new TestException();
        if (!"value1".equals(config.removeConnectProperty("prop1"))) throw new TestException();
        if (config.getConnectProperty("prop1") != null) throw new TestException();
    }

}
