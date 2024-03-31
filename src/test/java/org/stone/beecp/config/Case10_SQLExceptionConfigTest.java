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
        config.addSqlExceptionCode(500151);
        Assert.assertTrue(config.getSqlExceptionCodeList().contains(500151));
        config.removeSqlExceptionCode(500151);
        Assert.assertFalse(config.getSqlExceptionCodeList().contains(500151));

        config.addSqlExceptionCode(500151);
        BeeDataSourceConfig checkConfig = config.check();
        Assert.assertTrue(checkConfig.getSqlExceptionCodeList().contains(500151));
    }

    public void testOnExceptionState() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.addSqlExceptionState("0A000");
        Assert.assertTrue(config.getSqlExceptionStateList().contains("0A000"));
        config.removeSqlExceptionState("0A000");
        Assert.assertFalse(config.getSqlExceptionStateList().contains("0A000"));

        config.addSqlExceptionState("0A000");
        BeeDataSourceConfig checkConfig = config.check();
        Assert.assertTrue(checkConfig.getSqlExceptionStateList().contains("0A000"));
    }

    public void testOnSetAndGetPredication() {
        BeeDataSourceConfig config = ConfigFactory.createDefault();

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

    public void testOnPredicationCreationByClass() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        Class predicationClass = DummySqlExceptionPredication.class;
        config.setSqlExceptionPredicationClass(predicationClass);
        Assert.assertEquals(config.getSqlExceptionPredicationClass(), predicationClass);
        BeeDataSourceConfig checkConfig = config.check();
        Assert.assertNotNull(checkConfig.getSqlExceptionPredication());
    }

    public void testOnPredicationCreationByClassName() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        String predicationClassName = "org.stone.beecp.config.customization.DummySqlExceptionPredication";
        config.setSqlExceptionPredicationClassName(predicationClassName);
        Assert.assertEquals(config.getSqlExceptionPredicationClassName(), predicationClassName);
        BeeDataSourceConfig checkConfig = config.check();
        Assert.assertNotNull(checkConfig.getSqlExceptionPredication());
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

    public void testLoadInvalidErrorCodeFromProperties() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        try {
            Properties configProperties = new Properties();
            configProperties.put("sqlExceptionCodeList", "1,A,C");//test on invalid error code
            config.loadFromProperties(configProperties);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("SQLException error code"));
        }
    }
}
