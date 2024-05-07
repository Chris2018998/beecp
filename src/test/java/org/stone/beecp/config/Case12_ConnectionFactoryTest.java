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
import org.stone.beecp.mock.MockDataSource;
import org.stone.beecp.mock.MockXaDataSource;

import java.sql.SQLException;

public class Case12_ConnectionFactoryTest extends TestCase {

    public void testOnSetGet() {
        BeeDataSourceConfig config = ConfigFactory.createEmpty();

        Class<? extends RawXaConnectionFactory> factClass = NullXaConnectionFactory.class;
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

    public void testOnCreateFactory() {
        try {
            BeeDataSourceConfig config1 = ConfigFactory.createEmpty();
            config1.setConnectionFactoryClass(NullConnectionFactory.class);
            config1.check();

            BeeDataSourceConfig config2 = ConfigFactory.createEmpty();
            config2.setConnectionFactoryClass(NullXaConnectionFactory.class);
            config2.check();

            BeeDataSourceConfig config3 = ConfigFactory.createEmpty();
            config3.setConnectionFactoryClass(MockXaDataSource.class);
            config3.check();

            BeeDataSourceConfig config4 = ConfigFactory.createEmpty();
            config4.setConnectionFactoryClass(MockDataSource.class);
            config4.check();
        } catch (SQLException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("connection creation error"));
        }
    }

    public void testOnInvalidFactoryClass() throws SQLException {
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
