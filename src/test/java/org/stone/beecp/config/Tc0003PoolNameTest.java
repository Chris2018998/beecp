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

import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */

public class Tc0003PoolNameTest extends TestCase {

    public void testOnSetGet() {
        BeeDataSourceConfig config = createEmpty();

        config.setPoolName(null);
        Assert.assertNull(config.getPoolName());

        config.setPoolName("pool1");
        Assert.assertEquals("pool1", config.getPoolName());
    }

    public void testOnGeneration() throws Exception {
        BeeDataSourceConfig config = createDefault();
        BeeDataSourceConfig checkConfig = config.check();
        Assert.assertTrue(checkConfig.getPoolName().contains("FastPool-"));

        config.setPoolName("pool1");
        checkConfig = config.check();
        Assert.assertEquals("pool1", checkConfig.getPoolName());
    }

    public void testInProperties() {
        BeeDataSourceConfig config = createDefault();
        Properties prop = new Properties();

        prop.setProperty("poolName", "pool1");
        config.loadFromProperties(prop);
        Assert.assertEquals("pool1", config.getPoolName());

        prop.clear();
        prop.setProperty("pool-name", "pool2");
        config.loadFromProperties(prop);
        Assert.assertEquals("pool2", config.getPoolName());

        prop.clear();
        prop.setProperty("pool_name", "pool3");
        config.loadFromProperties(prop);
        Assert.assertEquals("pool3", config.getPoolName());
    }
}
