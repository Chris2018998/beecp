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

public class Case14_ConfigPrintExclusionTest extends TestCase {

    public void testOnSetAndGet() {
        BeeDataSourceConfig config = ConfigFactory.createEmpty();
        Assert.assertTrue(config.existConfigPrintExclusion("username"));
        config.addConfigPrintExclusion("password");
        config.addConfigPrintExclusion("poolName");
        Assert.assertTrue(config.existConfigPrintExclusion("poolName"));
        Assert.assertTrue(config.removeConfigPrintExclusion("poolName"));
        Assert.assertFalse(config.existConfigPrintExclusion("poolName"));
    }

    public void testOnLoadFromProperties() throws Exception {
        Properties prop = new Properties();
        prop.put("configPrintExclusionList", "username,password,poolName");

        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.loadFromProperties(prop);
        Assert.assertTrue(config.existConfigPrintExclusion("username"));
        Assert.assertTrue(config.existConfigPrintExclusion("password"));
        Assert.assertTrue(config.existConfigPrintExclusion("poolName"));
    }

    public void testOnConfigCopy() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setPrintConfigInfo(true);
        config.addConnectProperty("DB-Name", "MySQL");
        config.addConnectProperty("DB-URL", "jdbc:test");
        config.addConfigPrintExclusion("DB-Name");
        BeeDataSourceConfig config2 = config.check();
        Assert.assertTrue(config2.existConfigPrintExclusion("DB-Name"));
    }
}
