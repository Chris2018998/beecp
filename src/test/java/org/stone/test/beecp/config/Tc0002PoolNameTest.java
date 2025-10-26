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

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */

public class Tc0002PoolNameTest {

    @Test
    public void testSetAndGet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Assertions.assertNull(config.getPoolName());//default check
        config.setPoolName("Fast-pool");
        Assertions.assertEquals("Fast-pool", config.getPoolName());
        config.setPoolName(null);
        Assertions.assertNull(config.getPoolName());
    }

    @Test
    public void testPoolNameGeneration() throws Exception {
        BeeDataSourceConfig config = createDefault();
        Assertions.assertNull(config.getPoolName());
        BeeDataSourceConfig checkConfig = config.check();
        assertTrue(checkConfig.getPoolName().contains("FastPool-"));
    }

    @Test
    public void testPoolNameLoadFromProperties() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
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
