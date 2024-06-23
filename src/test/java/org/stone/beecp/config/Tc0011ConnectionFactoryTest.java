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
import org.stone.beecp.driver.MockDataSource;
import org.stone.beecp.driver.MockXaDataSource;
import org.stone.beecp.objects.MockCreateNullConnectionFactory;
import org.stone.beecp.objects.MockCreateNullXaConnectionFactory;

import java.sql.SQLException;

import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0011ConnectionFactoryTest extends TestCase {

    public void testOnSetGet() {
        BeeDataSourceConfig config = createEmpty();

        Class<? extends RawXaConnectionFactory> factClass = MockCreateNullXaConnectionFactory.class;
        config.setConnectionFactoryClass(factClass);
        Assert.assertEquals(config.getConnectionFactoryClass(), factClass);

        String factClassName = "org.stone.beecp.factory.NullConnectionFactory";
        config.setConnectionFactoryClassName(factClassName);
        Assert.assertEquals(config.getConnectionFactoryClassName(), factClassName);

        RawConnectionFactory factory1 = new MockCreateNullConnectionFactory();
        config.setRawConnectionFactory(factory1);
        Assert.assertEquals(config.getConnectionFactory(), factory1);

        RawXaConnectionFactory factory2 = new MockCreateNullXaConnectionFactory();
        config.setRawXaConnectionFactory(factory2);
        Assert.assertEquals(config.getConnectionFactory(), factory2);
    }

    public void testOnCreateFactory() {
        try {
            BeeDataSourceConfig config1 = createEmpty();
            config1.setConnectionFactoryClass(MockCreateNullConnectionFactory.class);

            config1.check();

            BeeDataSourceConfig config2 = createEmpty();
            config2.setConnectionFactoryClass(MockCreateNullXaConnectionFactory.class);
            config2.check();

            BeeDataSourceConfig config3 = createEmpty();
            config3.setConnectionFactoryClass(MockXaDataSource.class);
            config3.check();

            BeeDataSourceConfig config4 = createEmpty();
            config4.setConnectionFactoryClass(MockDataSource.class);
            config4.check();
        } catch (SQLException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("connection creation error"));
        }
    }

    public void testOnInvalidFactoryClass() throws SQLException {
        try {
            BeeDataSourceConfig config = createDefault();
            config.setConnectionFactoryClass(String.class);//invalid config
            config.check();
        } catch (BeeDataSourceConfigException e) {
            Throwable cause = e.getCause();
            Assert.assertNotNull(cause);
            String message = cause.getMessage();
            Assert.assertTrue(message != null && message.contains("which must extend from one of type"));
        }
    }

    public void testOnInvalidFactoryClassName() throws Exception {
        try {
            BeeDataSourceConfig config = createDefault();
            config.setConnectionFactoryClassName("java.lang.String");//invalid config
            config.check();
        } catch (BeeDataSourceConfigException e) {
            Throwable cause = e.getCause();
            Assert.assertNotNull(cause);
            String message = cause.getMessage();
            Assert.assertTrue(message != null && message.contains("which must extend from one of type"));
        }
    }

    public void testOnNotFoundFactoryClassName() throws Exception {
        try {
            BeeDataSourceConfig config = createDefault();
            config.setConnectionFactoryClassName("xx.xx.xx");//class not found
            config.check();
        } catch (BeeDataSourceConfigException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof ClassNotFoundException);
        }
    }
}
