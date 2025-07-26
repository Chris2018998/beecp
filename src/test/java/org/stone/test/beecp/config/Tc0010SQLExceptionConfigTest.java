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
import org.stone.beecp.BeeConnectionPredicate;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.test.beecp.objects.MockNotEvictConnectionPredicate1;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;
import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0010SQLExceptionConfigTest {

    @Test
    public void testOnExceptionCodeAddRemove() {
        BeeDataSourceConfig config = createEmpty();

        Assertions.assertNull(config.getSqlExceptionCodeList());
        config.removeSqlExceptionCode(500151);
        config.addSqlExceptionCode(500151);
        List<Integer> sqlExceptionCodeList = config.getSqlExceptionCodeList();
        Assertions.assertNotNull(sqlExceptionCodeList);

        Assertions.assertTrue(sqlExceptionCodeList.contains(500151));
        config.removeSqlExceptionCode(500151);
        Assertions.assertFalse(sqlExceptionCodeList.contains(500151));
        Assertions.assertTrue(sqlExceptionCodeList.isEmpty());

        config.addSqlExceptionCode(500152);
        config.addSqlExceptionCode(500152);//duplicated add
        Assertions.assertEquals(1, sqlExceptionCodeList.size());
    }

    @Test
    public void testOnExceptionStateAddRemove() {
        BeeDataSourceConfig config = createEmpty();

        Assertions.assertNull(config.getSqlExceptionStateList());
        config.removeSqlExceptionState("0A000");
        config.addSqlExceptionState("0A000");
        List<String> sqlExceptionStateList = config.getSqlExceptionStateList();
        Assertions.assertNotNull(sqlExceptionStateList);
        Assertions.assertTrue(sqlExceptionStateList.contains("0A000"));

        config.removeSqlExceptionState("0A000");
        Assertions.assertFalse(sqlExceptionStateList.contains("0A000"));
        Assertions.assertTrue(sqlExceptionStateList.isEmpty());

        config.addSqlExceptionState("57P01");
        config.addSqlExceptionState("57P01");//duplicated add
        Assertions.assertEquals(1, sqlExceptionStateList.size());
    }

    @Test
    public void testOnPredicationSettingAndGetting() {
        BeeDataSourceConfig config = createEmpty();

        BeeConnectionPredicate predication = new MockNotEvictConnectionPredicate1();
        config.setEvictPredicate(predication);
        Assertions.assertEquals(config.getEvictPredicate(), predication);

        Class<? extends BeeConnectionPredicate> predicationClass = MockNotEvictConnectionPredicate1.class;
        config.setEvictPredicateClass(predicationClass);
        Assertions.assertEquals(predicationClass, config.getEvictPredicateClass());

        String predicationClassName = "org.stone.beecp.config.customization.DummySqlExceptionPredication";
        config.setEvictPredicateClassName(predicationClassName);
        Assertions.assertEquals(predicationClassName, config.getEvictPredicateClassName());
    }

    @Test
    public void testOnPredicationCreation() throws Exception {
        BeeConnectionPredicate predication = new MockNotEvictConnectionPredicate1();
        BeeDataSourceConfig config1 = createDefault();
        config1.setEvictPredicate(predication);
        BeeDataSourceConfig checkConfig1 = config1.check();
        Assertions.assertEquals(checkConfig1.getEvictPredicate(), predication);

        BeeDataSourceConfig config2 = createDefault();
        Class<? extends BeeConnectionPredicate> predicationClass = MockNotEvictConnectionPredicate1.class;
        config2.setEvictPredicateClass(predicationClass);
        Assertions.assertEquals(predicationClass, config2.getEvictPredicateClass());
        BeeDataSourceConfig checkConfig2 = config2.check();
        Assertions.assertNotNull(checkConfig2.getEvictPredicate());

        BeeDataSourceConfig config3 = createDefault();
        String predicationClassName = "org.stone.test.beecp.objects.MockNotEvictConnectionPredicate1";
        config3.setEvictPredicateClassName(predicationClassName);
        Assertions.assertEquals(predicationClassName, config3.getEvictPredicateClassName());
        BeeDataSourceConfig checkConfig3 = config3.check();
        Assertions.assertNotNull(checkConfig3.getEvictPredicate());

        BeeDataSourceConfig config5 = createDefault();
        config5.setEvictPredicateClassName("String");
        try {
            config5.check();
            fail("[testOnPredicationCreation]not threw exception when set invalid predicate class name");
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("predicate"));
        }
    }

    @Test
    public void testLoadFromProperties() {
        BeeDataSourceConfig config = createEmpty();
        Properties prop = new Properties();
        prop.setProperty("sqlExceptionCodeList", "123");
        prop.setProperty("sqlExceptionStateList", "A");
        config.loadFromProperties(prop);
        Assertions.assertTrue(config.getSqlExceptionCodeList().contains(123));
        Assertions.assertTrue(config.getSqlExceptionStateList().contains("A"));

        prop.put("sqlExceptionCodeList", "1,A,C");//contains invalid error code
        try {
            config.loadFromProperties(prop);
            fail("[testLoadFromProperties]not threw exception when set invalid sql-exception-code");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("SQLException error code"));
        }
    }

    @Test
    public void testOnCheckCopy() throws Exception {
        BeeDataSourceConfig config1 = createDefault();
        config1.addSqlExceptionCode(500151);
        config1.addSqlExceptionState("0A000");
        BeeDataSourceConfig checkConfig = config1.check();
        Assertions.assertTrue(checkConfig.getSqlExceptionCodeList().contains(500151));
        Assertions.assertTrue(checkConfig.getSqlExceptionStateList().contains("0A000"));

        config1.removeSqlExceptionCode(500151);
        config1.removeSqlExceptionState("0A000");
        BeeDataSourceConfig checkConfig2 = config1.check();
        Assertions.assertNull(checkConfig2.getSqlExceptionCodeList());
        Assertions.assertNull(checkConfig2.getSqlExceptionStateList());
    }
}
