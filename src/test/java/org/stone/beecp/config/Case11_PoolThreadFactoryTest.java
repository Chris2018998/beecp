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
import org.stone.beecp.config.customization.DummyThreadFactory;

public class Case11_PoolThreadFactoryTest extends TestCase {

    public void testOnSetGet() {
        BeeDataSourceConfig config = ConfigFactory.createEmpty();

        config.setThreadFactoryClassName(null);
        Assert.assertNotNull(config.getThreadFactoryClassName());

        Class<? extends DummyThreadFactory> factClass = DummyThreadFactory.class;
        config.setThreadFactoryClass(factClass);
        Assert.assertEquals(config.getThreadFactoryClass(), factClass);

        String factClassName = "org.stone.beecp.config.customization.DummyThreadFactory";
        config.setThreadFactoryClassName(factClassName);
        Assert.assertEquals(config.getThreadFactoryClassName(), factClassName);

        DummyThreadFactory threadFactory = new DummyThreadFactory();
        config.setThreadFactory(threadFactory);
        Assert.assertEquals(config.getThreadFactory(), threadFactory);
    }

    public void testOnCreation() throws Exception {
        BeeDataSourceConfig config1 = ConfigFactory.createDefault();
        config1.setThreadFactory(new DummyThreadFactory());
        BeeDataSourceConfig checkConfig = config1.check();
        Assert.assertNotNull(checkConfig.getThreadFactory());

        BeeDataSourceConfig config2 = ConfigFactory.createDefault();
        config2.setThreadFactoryClass(org.stone.beecp.config.customization.DummyThreadFactory.class);
        checkConfig = config2.check();
        Assert.assertNotNull(checkConfig.getThreadFactory());

        BeeDataSourceConfig config3 = ConfigFactory.createDefault();
        config3.setThreadFactoryClassName("org.stone.beecp.config.customization.DummyThreadFactory");
        checkConfig = config3.check();
        Assert.assertNotNull(checkConfig.getThreadFactory());
    }

    public void testOnInValidThreadFactClassName() {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setThreadFactoryClassName("java.lang.String");//invalid class

        try {
            config.check();
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("which must extend from one of type"));
        }
    }

    public void testOnNotFoundThreadFactClassName() {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setThreadFactoryClassName("org.stone.beecp.BeeConnectionPoolThreadFactory.ConnectionPoolThreadFactory22");//invalid class

        try {
            config.check();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof ClassNotFoundException);
        }
    }
}
