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
import org.stone.beecp.RawConnectionFactory;
import org.stone.beecp.RawXaConnectionFactory;
import org.stone.beecp.factory.NullConnectionFactory;
import org.stone.beecp.factory.NullXaConnectionFactory;

public class Case12_ConnectionFactoryTest extends TestCase {

    public void testOnSetGet() {
        BeeDataSourceConfig config = ConfigFactory.createEmpty();

        Class factClass = NullXaConnectionFactory.class;
        config.setConnectionFactoryClass(factClass);
        Assert.assertEquals(config.getConnectionFactoryClass(), factClass);

        String factClassName = "org.stone.beecp.factory.NullConnectionFactory";
        config.setConnectionFactoryClassName(factClassName);
        Assert.assertEquals(config.getConnectionFactoryClassName(), factClassName);

        RawConnectionFactory factory1 = new NullConnectionFactory();
        config.setRawConnectionFactory(factory1);
        Assert.assertEquals(config.getConnectionFactory(), factory1);

        RawXaConnectionFactory factory2 = new NullXaConnectionFactory();
        config.setRawXaConnectionFactory(factory2);
        Assert.assertEquals(config.getConnectionFactory(), factory2);
    }


    public void testOnInvalidFactoryClass() throws Exception {
        try {
            BeeDataSourceConfig config = ConfigFactory.createDefault();
            config.setConnectionFactoryClass(String.class);//invalid config
            config.check();
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("which must extend from one of type"));
        }
    }

    public void testOnInvalidFactoryClassName() throws Exception {
        try {
            BeeDataSourceConfig config = ConfigFactory.createDefault();
            config.setConnectionFactoryClassName("java.lang.String");//invalid config
            config.check();
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("which must extend from one of type"));
        }
    }

    public void testOnNotFoundFactoryClassName() throws Exception {
        try {
            BeeDataSourceConfig config = ConfigFactory.createDefault();
            config.setConnectionFactoryClassName("xx.xx.xx");//class not found
            config.check();
        } catch (BeeDataSourceConfigException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof ClassNotFoundException);
        }
    }
}
