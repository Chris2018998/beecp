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

public class Case1_PoolNameTest extends TestCase {

    public void testOnSetGet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();

        config.setPoolName(null);
        Assert.assertNull(config.getPoolName());

        config.setPoolName("pool1");
        Assert.assertEquals(config.getPoolName(), "pool1");
    }

    public void testOnGeneration() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        BeeDataSourceConfig checkConfig = config.check();
        Assert.assertTrue(checkConfig.getPoolName().contains("FastPool-"));

        config.setPoolName("pool1");
        checkConfig = config.check();
        Assert.assertEquals(checkConfig.getPoolName(), "pool1");
    }

    public void testInProperties() {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        Properties prop = new Properties();

        prop.setProperty("poolName", "pool1");
        config.loadFromProperties(prop);
        Assert.assertEquals(config.getPoolName(), "pool1");

        prop.clear();
        prop.setProperty("pool-name", "pool2");
        config.loadFromProperties(prop);
        Assert.assertEquals(config.getPoolName(), "pool2");

        prop.clear();
        prop.setProperty("pool_name", "pool3");
        config.loadFromProperties(prop);
        Assert.assertEquals(config.getPoolName(), "pool3");
    }
}
