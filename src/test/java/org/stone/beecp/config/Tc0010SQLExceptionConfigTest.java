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
import org.stone.beecp.BeeConnectionPredicate;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.objects.MockNotEvictConnectionPredicate1;

import java.util.List;
import java.util.Properties;

import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0010SQLExceptionConfigTest extends TestCase {

    public void testOnExceptionCodeAddRemove() {
        BeeDataSourceConfig config = createEmpty();

        Assert.assertNull(config.getSqlExceptionCodeList());
        config.removeSqlExceptionCode(500151);
        config.addSqlExceptionCode(500151);
        List<Integer> sqlExceptionCodeList = config.getSqlExceptionCodeList();
        Assert.assertNotNull(sqlExceptionCodeList);

        Assert.assertTrue(sqlExceptionCodeList.contains(500151));
        config.removeSqlExceptionCode(500151);
        Assert.assertFalse(sqlExceptionCodeList.contains(500151));
        Assert.assertTrue(sqlExceptionCodeList.isEmpty());

        config.addSqlExceptionCode(500152);
        config.addSqlExceptionCode(500152);//duplicated add
        Assert.assertEquals(1, sqlExceptionCodeList.size());
    }

    public void testOnExceptionStateAddRemove() {
        BeeDataSourceConfig config = createEmpty();

        Assert.assertNull(config.getSqlExceptionStateList());
        config.removeSqlExceptionState("0A000");
        config.addSqlExceptionState("0A000");
        List<String> sqlExceptionStateList = config.getSqlExceptionStateList();
        Assert.assertNotNull(sqlExceptionStateList);
        Assert.assertTrue(sqlExceptionStateList.contains("0A000"));

        config.removeSqlExceptionState("0A000");
        Assert.assertFalse(sqlExceptionStateList.contains("0A000"));
        Assert.assertTrue(sqlExceptionStateList.isEmpty());

        config.addSqlExceptionState("57P01");
        config.addSqlExceptionState("57P01");//duplicated add
        Assert.assertEquals(1, sqlExceptionStateList.size());
    }

    public void testOnPredicationSettingAndGetting() {
        BeeDataSourceConfig config = createEmpty();

        BeeConnectionPredicate predication = new MockNotEvictConnectionPredicate1();
        config.setEvictPredicate(predication);
        Assert.assertEquals(config.getEvictPredicate(), predication);

        Class<? extends BeeConnectionPredicate> predicationClass = MockNotEvictConnectionPredicate1.class;
        config.setEvictPredicateClass(predicationClass);
        Assert.assertEquals(predicationClass, config.getEvictPredicateClass());

        String predicationClassName = "org.stone.beecp.config.customization.DummySqlExceptionPredication";
        config.setEvictPredicateClassName(predicationClassName);
        Assert.assertEquals(predicationClassName, config.getEvictPredicateClassName());
    }

    public void testOnPredicationCreation() throws Exception {
        BeeConnectionPredicate predication = new MockNotEvictConnectionPredicate1();
        BeeDataSourceConfig config1 = createDefault();
        config1.setEvictPredicate(predication);
        BeeDataSourceConfig checkConfig1 = config1.check();
        Assert.assertEquals(checkConfig1.getEvictPredicate(), predication);

        BeeDataSourceConfig config2 = createDefault();
        Class<? extends BeeConnectionPredicate> predicationClass = MockNotEvictConnectionPredicate1.class;
        config2.setEvictPredicateClass(predicationClass);
        Assert.assertEquals(predicationClass, config2.getEvictPredicateClass());
        BeeDataSourceConfig checkConfig2 = config2.check();
        Assert.assertNotNull(checkConfig2.getEvictPredicate());

        BeeDataSourceConfig config3 = createDefault();
        String predicationClassName = "org.stone.beecp.objects.MockNotEvictConnectionPredicate1";
        config3.setEvictPredicateClassName(predicationClassName);
        Assert.assertEquals(predicationClassName, config3.getEvictPredicateClassName());
        BeeDataSourceConfig checkConfig3 = config3.check();
        Assert.assertNotNull(checkConfig3.getEvictPredicate());

        BeeDataSourceConfig config5 = createDefault();
        config5.setEvictPredicateClassName("String");
        try {
            config5.check();
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("predicate"));
        }
    }

    public void testLoadFromProperties() {
        BeeDataSourceConfig config = createEmpty();
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
        BeeDataSourceConfig config1 = createDefault();
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
