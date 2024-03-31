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
import org.stone.beecp.JdbcConfig;
import org.stone.beecp.config.customization.DummyThreadFactory;

public class Case11_PoolThreadFactoryTest extends TestCase {

    public void testOnSetGet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();

        config.setThreadFactoryClassName(null);
        Assert.assertNull(config.getThreadFactoryClassName());

        Class factClass = DummyThreadFactory.class;
        config.setThreadFactoryClass(factClass);
        Assert.assertEquals(config.getThreadFactoryClass(), factClass);

        String factClassName = "org.stone.beecp.config.customization.DummyThreadFactory";
        config.setThreadFactoryClassName(factClassName);
        Assert.assertEquals(config.getThreadFactoryClassName(), factClassName);

        DummyThreadFactory threadFactory = new DummyThreadFactory();
        config.setThreadFactory(threadFactory);
        Assert.assertEquals(config.getThreadFactory(), threadFactory);
    }

    public void testOnInValidThreadFactClass() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setThreadFactoryClass(String.class);//invalid class

        try {
            config.check();
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("which must extend from one of type"));
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
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("which must extend from one of type"));
        }
    }
}
