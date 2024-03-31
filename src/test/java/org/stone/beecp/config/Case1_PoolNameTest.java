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

import java.util.Properties;

public class Case1_PoolNameTest extends TestCase {

    public void testOnSetGet() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();

        config.setPoolName(null);
        if (config.getPoolName() != null) throw new TestException();

        config.setPoolName("pool1");
        if (!"pool1".equals(config.getPoolName())) throw new TestException();
    }

    public void testOnGeneration() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        BeeDataSourceConfig checkConfig = config.check();
        if (!checkConfig.getPoolName().startsWith("FastPool-")) throw new TestException();

        config.setPoolName("pool1");
        checkConfig = config.check();
        if (!"pool1".equals(checkConfig.getPoolName())) throw new TestException();
    }

    public void testInProperties() throws Exception {
        Properties prop = new Properties();
        prop.setProperty("poolName", "pool1");
        BeeDataSourceConfig config = ConfigFactory.createDefault();

        config.loadFromProperties(prop);
        if (!"pool1".equals(config.getPoolName())) throw new TestException();

        prop.clear();
        prop.setProperty("pool-name", "pool2");
        config.loadFromProperties(prop);
        if (!"pool2".equals(config.getPoolName())) throw new TestException();

        prop.clear();
        prop.setProperty("pool_name", "pool3");
        config.loadFromProperties(prop);
        if (!"pool3".equals(config.getPoolName())) throw new TestException();
    }
}
