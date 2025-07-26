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

import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSourceConfig;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;
import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */

public class Tc0004PoolNameTest {

    @Test
    public void testNullValueSet() {
        BeeDataSourceConfig config = createEmpty();

        config.setPoolName(null);
        assertNull(config.getPoolName());

        config.setPoolName("pool1");
        assertEquals("pool1", config.getPoolName());
    }

    @Test
    public void testPoolNameGeneration() throws Exception {
        BeeDataSourceConfig config = createDefault();
        BeeDataSourceConfig checkConfig = config.check();
        assertTrue(checkConfig.getPoolName().contains("FastPool-"));

        config.setPoolName("pool1");
        checkConfig = config.check();
        assertEquals("pool1", checkConfig.getPoolName());
    }

    @Test
    public void testInProperties() {
        BeeDataSourceConfig config = createDefault();
        Properties prop = new Properties();

        prop.setProperty("poolName", "pool1");
        config.loadFromProperties(prop);
        assertEquals("pool1", config.getPoolName());

        prop.clear();
        prop.setProperty("pool-name", "pool2");
        config.loadFromProperties(prop);
        assertEquals("pool2", config.getPoolName());

        prop.clear();
        prop.setProperty("pool_name", "pool3");
        config.loadFromProperties(prop);
        assertEquals("pool3", config.getPoolName());
    }
}
