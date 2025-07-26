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

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;
import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0013ConfigPrintExclusionTest {

    @Test
    public void testOnSetAndGet() {
        BeeDataSourceConfig config = createEmpty();
        Assertions.assertTrue(config.existConfigPrintExclusion("username"));
        config.addConfigPrintExclusion("password");
        config.addConfigPrintExclusion("poolName");
        Assertions.assertTrue(config.existConfigPrintExclusion("poolName"));
        Assertions.assertTrue(config.removeConfigPrintExclusion("poolName"));
        Assertions.assertFalse(config.existConfigPrintExclusion("poolName"));
    }

    @Test
    public void testOnLoadFromProperties() {
        Properties prop = new Properties();
        prop.put("configPrintExclusionList", "username,password,poolName");

        BeeDataSourceConfig config = createDefault();
        config.loadFromProperties(prop);
        Assertions.assertTrue(config.existConfigPrintExclusion("username"));
        Assertions.assertTrue(config.existConfigPrintExclusion("password"));
        Assertions.assertTrue(config.existConfigPrintExclusion("poolName"));
    }

    @Test
    public void testOnConfigCopy() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.addConnectProperty("DB-Name", "MySQL");
        config.addConnectProperty("DB-URL", "jdbc:test");
        config.addConfigPrintExclusion("DB-Name");
        BeeDataSourceConfig config2 = config.check();
        Assertions.assertTrue(config2.existConfigPrintExclusion("DB-Name"));
    }
}
