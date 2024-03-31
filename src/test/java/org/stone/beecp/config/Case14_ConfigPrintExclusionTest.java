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

public class Case14_ConfigPrintExclusionTest extends TestCase {

    public void test1() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Assert.assertTrue(config.existConfigPrintExclusion("username"));

        config.addConfigPrintExclusion("poolName");
        config.addConfigPrintExclusion("poolName");
        Assert.assertTrue(config.existConfigPrintExclusion("poolName"));

        config.removeConfigPrintExclusion("poolName");
        Assert.assertFalse(config.existConfigPrintExclusion("poolName"));

        config.addConfigPrintExclusion("poolName");
        config.clearAllConfigPrintExclusion();
        Assert.assertFalse(config.existConfigPrintExclusion("poolName"));
    }

    public void testOnConnectProperties() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setPrintConfigInfo(true);
        config.addConnectProperty("DB-Name", "MySQL");
        config.addConnectProperty("DB-URL", "jdbc:test");
        config.addConfigPrintExclusion("DB-Name");
        config.check();
    }

    public void testOnConnectProperties2() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setPrintConfigInfo(true);
        config.check();

        config.addConfigPrintExclusion("connectProperties");
        config.addConfigPrintExclusion("sqlExceptionCodeList");
        config.addConfigPrintExclusion("sqlExceptionStateList");
        config.check();

        config.addSqlExceptionCode(500);
        config.addSqlExceptionState("test");
        config.check();

        config.removeSqlExceptionCode(500);
        config.removeSqlExceptionState("test");
        config.check();
    }
}
