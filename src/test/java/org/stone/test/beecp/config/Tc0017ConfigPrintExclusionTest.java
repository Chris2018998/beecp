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

import static org.stone.beecp.pool.ConnectionPoolStatics.CONFIG_EXCLUSION_LIST_OF_PRINT;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;
import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0017ConfigPrintExclusionTest {

    @Test
    public void testConfigurationSet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Assertions.assertFalse(config.isPrintRuntimeLogs());//default check
        config.setPrintRuntimeLogs(true);
        Assertions.assertTrue(config.isPrintRuntimeLogs());
        config.setPrintRuntimeLogs(false);
        Assertions.assertFalse(config.isPrintRuntimeLogs());
    }

    @Test
    public void testOnSetAndGet() {
        BeeDataSourceConfig config = createEmpty();
        Assertions.assertTrue(config.existExclusionNameOfPrint("username"));
        config.addExclusionNameOfPrint("password");
        config.addExclusionNameOfPrint("poolName");
        Assertions.assertTrue(config.existExclusionNameOfPrint("poolName"));
        Assertions.assertTrue(config.removeExclusionNameOfPrint("poolName"));
        Assertions.assertFalse(config.existExclusionNameOfPrint("poolName"));
    }

    @Test
    public void testOnLoadFromProperties() {
        Properties prop = new Properties();
        prop.put(CONFIG_EXCLUSION_LIST_OF_PRINT, "username,password,poolName");

        BeeDataSourceConfig config = createDefault();
        config.loadFromProperties(prop);
        Assertions.assertTrue(config.existExclusionNameOfPrint("username"));
        Assertions.assertTrue(config.existExclusionNameOfPrint("password"));
        Assertions.assertTrue(config.existExclusionNameOfPrint("poolName"));
    }

    @Test
    public void testOnConfigCopy() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.addConnectionFactoryProperty("DB-Name", "MySQL");
        config.addConnectionFactoryProperty("DB-URL", "jdbc:test");
        config.addExclusionNameOfPrint("DB-Name");
        BeeDataSourceConfig config2 = config.check();
        Assertions.assertTrue(config2.existExclusionNameOfPrint("DB-Name"));
    }
}
