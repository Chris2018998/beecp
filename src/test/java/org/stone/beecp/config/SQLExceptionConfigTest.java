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
import org.stone.base.TestException;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.SQLExceptionPredication;
import org.stone.beecp.config.customization.DummySqlExceptionPredication;

import java.util.Properties;

public class SQLExceptionConfigTest extends TestCase {

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

    public void testOnExceptionPredication() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setPrintConfigInfo(true);

        Class predicationClass = DummySqlExceptionPredication.class;
        config.setSqlExceptionPredicationClass(predicationClass);
        if (!predicationClass.equals(config.getSqlExceptionPredicationClass())) throw new TestException();

        String predicationClassName = "org.stone.beecp.config.customization.DummySqlExceptionPredication";
        config.setSqlExceptionPredicationClassName(predicationClassName);
        if (!predicationClassName.equals(config.getSqlExceptionPredicationClassName())) throw new TestException();

        SQLExceptionPredication predication = new DummySqlExceptionPredication();
        config.setSqlExceptionPredication(predication);
        if (predication != config.getSqlExceptionPredication()) throw new TestException();
        config.check();
    }

    public void testOnErrorPredicationClass() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setSqlExceptionPredicationClass(String.class);
        try {
            config.check();
        } catch (BeeDataSourceConfigException e) {
            String msg = e.getMessage();
            if (!(msg != null && msg.contains("predication"))) throw new TestException();
        }
    }

    public void testOnErrorPredicationClassName() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setSqlExceptionPredicationClassName("String");
        try {
            config.check();
        } catch (BeeDataSourceConfigException e) {
            String msg = e.getMessage();
            if (!(msg != null && msg.contains("predication"))) throw new TestException();
        }
    }

    public void testLoadFromProperties() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();

        Properties prop =new Properties();
        prop.setProperty("sqlExceptionCodeList","123");
        prop.setProperty("sqlExceptionStateList","A");
        prop.setProperty("configPrintExclusionList","driverClassName");

        config.loadFromProperties(prop);
    }

}
