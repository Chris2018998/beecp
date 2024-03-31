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
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.SQLExceptionPredication;
import org.stone.beecp.config.customization.DummySqlExceptionPredication;

import java.util.Properties;

public class Case10_SQLExceptionConfigTest extends TestCase {

    public void testOnExceptionCode() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.removeSqlExceptionCode(500150);
        config.addSqlExceptionCode(500150);
        config.addSqlExceptionCode(500150);

        config.addSqlExceptionCode(2399);
        config.removeSqlExceptionCode(500150);
        config.check();
    }

    public void testOnExceptionState() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.removeSqlExceptionState("0A000");// FEATURE UNSUPPORTED
        config.addSqlExceptionState("0A000");// ADMIN SHUTDOWN
        config.addSqlExceptionState("0A000");// ADMIN SHUTDOWN
        config.addSqlExceptionState("57P01");
        config.removeSqlExceptionState("0A000");
        config.check();
    }

    public void testOnExceptionPredication() {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setPrintConfigInfo(true);

        Class predicationClass = DummySqlExceptionPredication.class;
        config.setSqlExceptionPredicationClass(predicationClass);
        Assert.assertEquals(config.getSqlExceptionPredicationClass(), predicationClass);

        String predicationClassName = "org.stone.beecp.config.customization.DummySqlExceptionPredication";
        config.setSqlExceptionPredicationClassName(predicationClassName);
        Assert.assertEquals(config.getSqlExceptionPredicationClassName(), predicationClassName);

        SQLExceptionPredication predication = new DummySqlExceptionPredication();
        config.setSqlExceptionPredication(predication);
        Assert.assertEquals(config.getSqlExceptionPredication(), predication);
    }

    public void testOnErrorPredicationClass() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setSqlExceptionPredicationClass(String.class);
        try {
            config.check();
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("predication"));
        }
    }

    public void testOnErrorPredicationClassName() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setSqlExceptionPredicationClassName("String");
        try {
            config.check();
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("predication"));
        }
    }

    public void testLoadFromProperties() {
        BeeDataSourceConfig config = ConfigFactory.createDefault();

        Properties prop = new Properties();
        prop.setProperty("sqlExceptionCodeList", "123");
        prop.setProperty("sqlExceptionStateList", "A");
        config.loadFromProperties(prop);

        Assert.assertTrue(config.getSqlExceptionCodeList().contains(123));
        Assert.assertTrue(config.getSqlExceptionStateList().contains("A"));
    }
}
