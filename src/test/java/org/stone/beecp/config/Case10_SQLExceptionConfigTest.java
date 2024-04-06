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

import java.util.List;
import java.util.Properties;

public class Case10_SQLExceptionConfigTest extends TestCase {

    public void testOnExceptionCodeAddRemove() {
        BeeDataSourceConfig config = ConfigFactory.createEmpty();

        Assert.assertNull(config.getSqlExceptionCodeList());
        config.addSqlExceptionCode(500151);
        List<Integer> sqlExceptionCodeList = config.getSqlExceptionCodeList();
        Assert.assertNotNull(sqlExceptionCodeList);

        Assert.assertTrue(sqlExceptionCodeList.contains(500151));
        config.removeSqlExceptionCode(500151);
        Assert.assertFalse(sqlExceptionCodeList.contains(500151));
        Assert.assertTrue(sqlExceptionCodeList.isEmpty());

        config.addSqlExceptionCode(500152);
        config.addSqlExceptionCode(500152);//duplicated add
        Assert.assertEquals(sqlExceptionCodeList.size(), 1);
    }

    public void testOnExceptionStateAddRemove() {
        BeeDataSourceConfig config = ConfigFactory.createEmpty();

        Assert.assertNull(config.getSqlExceptionStateList());
        config.addSqlExceptionState("0A000");
        List<String> sqlExceptionStateList = config.getSqlExceptionStateList();
        Assert.assertNotNull(sqlExceptionStateList);
        Assert.assertTrue(sqlExceptionStateList.contains("0A000"));

        config.removeSqlExceptionState("0A000");
        Assert.assertFalse(sqlExceptionStateList.contains("0A000"));
        Assert.assertTrue(sqlExceptionStateList.isEmpty());

        config.addSqlExceptionState("57P01");
        config.addSqlExceptionState("57P01");//duplicated add
        Assert.assertEquals(sqlExceptionStateList.size(), 1);
    }

    public void testOnPredicationSettingAndGetting() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createEmpty();

        SQLExceptionPredication predication = new DummySqlExceptionPredication();
        config.setSqlExceptionPredication(predication);
        Assert.assertEquals(config.getSqlExceptionPredication(), predication);

        Class predicationClass = DummySqlExceptionPredication.class;
        config.setSqlExceptionPredicationClass(predicationClass);
        Assert.assertEquals(config.getSqlExceptionPredicationClass(), predicationClass);

        String predicationClassName = "org.stone.beecp.config.customization.DummySqlExceptionPredication";
        config.setSqlExceptionPredicationClassName(predicationClassName);
        Assert.assertEquals(config.getSqlExceptionPredicationClassName(), predicationClassName);
    }

    public void testOnPredicationCreation() throws Exception {
        SQLExceptionPredication predication = new DummySqlExceptionPredication();
        BeeDataSourceConfig config1 = ConfigFactory.createDefault();
        config1.setSqlExceptionPredication(predication);
        BeeDataSourceConfig checkConfig1 = config1.check();
        Assert.assertEquals(checkConfig1.getSqlExceptionPredication(), predication);

        BeeDataSourceConfig config2 = ConfigFactory.createDefault();
        Class predicationClass = DummySqlExceptionPredication.class;
        config2.setSqlExceptionPredicationClass(predicationClass);
        Assert.assertEquals(config2.getSqlExceptionPredicationClass(), predicationClass);
        BeeDataSourceConfig checkConfig2 = config2.check();
        Assert.assertNotNull(checkConfig2.getSqlExceptionPredication());

        BeeDataSourceConfig config3 = ConfigFactory.createDefault();
        String predicationClassName = "org.stone.beecp.config.customization.DummySqlExceptionPredication";
        config3.setSqlExceptionPredicationClassName(predicationClassName);
        Assert.assertEquals(config3.getSqlExceptionPredicationClassName(), predicationClassName);
        BeeDataSourceConfig checkConfig3 = config3.check();
        Assert.assertNotNull(checkConfig3.getSqlExceptionPredication());


        //failure test on creation
        BeeDataSourceConfig config4 = ConfigFactory.createDefault();
        config4.setSqlExceptionPredicationClass(String.class);//invalid exception predication class
        try {
            config4.check();
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("predication"));
        }

        BeeDataSourceConfig config5 = ConfigFactory.createDefault();
        config5.setSqlExceptionPredicationClassName("String");
        try {
            config5.check();
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("predication"));
        }
    }

    public void testLoadFromProperties() {
        BeeDataSourceConfig config = ConfigFactory.createEmpty();
        Properties prop = new Properties();
        prop.setProperty("sqlExceptionCodeList", "123");
        prop.setProperty("sqlExceptionStateList", "A");
        config.loadFromProperties(prop);
        Assert.assertTrue(config.getSqlExceptionCodeList().contains(123));
        Assert.assertTrue(config.getSqlExceptionStateList().contains("A"));

        prop.put("sqlExceptionCodeList", "1,A,C");//contains invalid error code
        try {
            config.loadFromProperties(prop);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("SQLException error code"));
        }
    }

    public void testOnCheckCopy() throws Exception {
        BeeDataSourceConfig config1 = ConfigFactory.createDefault();
        config1.addSqlExceptionCode(500151);
        config1.addSqlExceptionState("0A000");
        BeeDataSourceConfig checkConfig = config1.check();
        Assert.assertTrue(checkConfig.getSqlExceptionCodeList().contains(500151));
        Assert.assertTrue(checkConfig.getSqlExceptionStateList().contains("0A000"));

        config1.removeSqlExceptionCode(500151);
        config1.removeSqlExceptionState("0A000");
        BeeDataSourceConfig checkConfig2 = config1.check();
        Assert.assertNull(checkConfig2.getSqlExceptionCodeList());
        Assert.assertNull(checkConfig2.getSqlExceptionStateList());
    }
}
