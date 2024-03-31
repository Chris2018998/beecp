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
import org.stone.beecp.JdbcConfig;
import org.stone.beecp.config.customization.DummyThreadFactory;

public class Case11_PoolThreadFactoryTest extends TestCase {

    public void testOnSetGet() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();

        config.setThreadFactoryClassName(null);
        if (config.getThreadFactoryClassName() == null) throw new TestException();

        Class factClass = DummyThreadFactory.class;
        config.setThreadFactoryClass(factClass);
        if (!factClass.equals(config.getThreadFactoryClass())) throw new TestException();

        String factClassName = "org.stone.beecp.config.customization.DummyThreadFactory";
        config.setThreadFactoryClassName(factClassName);

        if (!factClassName.equals(config.getThreadFactoryClassName())) throw new TestException();
        DummyThreadFactory threadFactory = new DummyThreadFactory();
        config.setThreadFactory(threadFactory);
        if (threadFactory != config.getThreadFactory()) throw new TestException();
    }

    public void testOnInValidThreadFactClass() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setThreadFactoryClass(String.class);//invalid class

        try {
            config.check();
        } catch (Exception e) {
            String msg = e.getMessage();
            boolean matched = msg != null && msg.contains("which must extend from one of type");
            if (!matched) throw new TestException();
        }
    }

    public void testOnInValidThreadFactClassName() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setThreadFactoryClassName("java.lang.String");//invalid class

        try {
            config.check();
        } catch (Exception e) {
            String msg = e.getMessage();
            boolean matched = msg != null && msg.contains("which must extend from one of type");
            if (!matched) throw new TestException();
        }
    }
}
