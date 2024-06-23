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
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSourceConfig;

import java.util.Properties;

import static org.stone.beecp.config.DsConfigFactory.*;

/**
 * Jdbc link info test
 *
 * @author Chris Liao
 */
public class Tc0002JdbcLinkInfoTest extends TestCase {
    private final String user = "root";
    private final String password = "test";
    private final String url = MOCK_URL;
    private final String driver = MOCK_DRIVER;

    public void testOnSetAndGet() {
        //situation1: check null setting
        BeeDataSourceConfig config = createEmpty();
        config.setUsername(null);
        config.setPassword(null);
        config.setUrl(null);
        config.setDriverClassName(null);
        Assert.assertNull(config.getUsername());
        Assert.assertNull(config.getPassword());
        Assert.assertNull(config.getUrl());
        Assert.assertNull(config.getDriverClassName());
        config.setJdbcUrl(null);
        Assert.assertNull(config.getUrl());
        Assert.assertNull(config.getJdbcUrl());

        //situation1: check non-null setting
        BeeDataSourceConfig config2 = createEmpty();
        config2.setUsername(user);
        config2.setPassword(password);
        config2.setUrl(url);
        config2.setDriverClassName(driver);
        Assert.assertEquals(config2.getUsername(), user);
        Assert.assertEquals(config2.getPassword(), password);
        Assert.assertEquals(config2.getUrl(), url);
        Assert.assertEquals(config2.getDriverClassName(), driver);
        config2.setJdbcUrl(url);
        Assert.assertEquals(config2.getUrl(), url);
        Assert.assertEquals(config2.getJdbcUrl(), url);
    }

    public void testOnConstructor() {
        //situation1: check jdbc link info in constructor
        BeeDataSourceConfig config = new BeeDataSourceConfig(driver, url, user, password);
        Assert.assertEquals(config.getUsername(), user);
        Assert.assertEquals(config.getPassword(), password);
        Assert.assertEquals(config.getUrl(), url);
        Assert.assertEquals(config.getDriverClassName(), driver);

        //situation2: check properties in constructor
        Properties prop = new Properties();
        prop.setProperty("username", user);
        prop.setProperty("password", password);
        prop.setProperty("url", url);
        prop.setProperty("driverClassName", driver);
        BeeDataSourceConfig config2 = new BeeDataSourceConfig(prop);
        Assert.assertEquals(config2.getUsername(), user);
        Assert.assertEquals(config2.getPassword(), password);
        Assert.assertEquals(config2.getUrl(), url);
        Assert.assertEquals(config2.getDriverClassName(), driver);
    }

    public void testLoadFromProperties() {
        //situation1: load config from properties
        BeeDataSourceConfig config = createEmpty();
        Properties prop = new Properties();
        prop.setProperty("username", user);
        prop.setProperty("password", password);
        prop.setProperty("url", url);
        prop.setProperty("driverClassName", driver);
        config.loadFromProperties(prop);
        Assert.assertEquals(config.getUsername(), user);
        Assert.assertEquals(config.getPassword(), password);
        Assert.assertEquals(config.getUrl(), url);
        Assert.assertEquals(config.getDriverClassName(), driver);

        //situation2: config item name(contains middle line)
        prop.clear();
        prop.setProperty("jdbc-url", url);
        prop.setProperty("driver-class-name", driver);
        config = createEmpty();
        config.loadFromProperties(prop);
        Assert.assertEquals(config.getUrl(), url);
        Assert.assertEquals(config.getJdbcUrl(), url);
        Assert.assertEquals(config.getDriverClassName(), driver);

        //situation3: config item name(contains under line)
        prop.clear();
        prop.setProperty("jdbc_url", url);
        prop.setProperty("driver_class_name", driver);
        config = createEmpty();
        config.loadFromProperties(prop);
        Assert.assertEquals(config.getUrl(), url);
        Assert.assertEquals(config.getJdbcUrl(), url);
        Assert.assertEquals(config.getDriverClassName(), driver);
    }

    public void testOnJdbcUrl() throws Exception {
        BeeDataSourceConfig config1 = createDefault();

        //situation1: jdbc url check(can't be null)
        config1.setJdbcUrl(null);
        try {
            config1.check();
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("jdbcUrl can't be null"));
        }

        //situation2: load 'beecp.url' from system.properties
        clearBeeCPInfoFromSystemProperties();
        System.setProperty("beecp.url", url);
        System.setProperty("beecp.user", user);
        System.setProperty("beecp.password", password);
        BeeDataSourceConfig config2 = createEmpty();
        config2.setDriverClassName(driver);
        BeeDataSourceConfig checkConfig = config2.check();
        Object connectionFactory = checkConfig.getConnectionFactory();
        Assert.assertEquals(url, TestUtil.getFieldValue(connectionFactory, "url"));
        Properties configProperties = (Properties) TestUtil.getFieldValue(connectionFactory, "properties");
        Assert.assertEquals(configProperties.getProperty("user"), user);
        Assert.assertEquals(configProperties.getProperty("password"), password);

        //situation3: load 'beecp.jdbcUrl' from system.properties
        clearBeeCPInfoFromSystemProperties();
        System.setProperty("beecp.jdbcUrl", url);
        BeeDataSourceConfig config3 = createEmpty();
        config3.setDriverClassName(driver);
        connectionFactory = checkConfig.getConnectionFactory();
        Assert.assertEquals(url, TestUtil.getFieldValue(connectionFactory, "url"));
    }

    public void testLoadFromConnectProperties() throws Exception {
        BeeDataSourceConfig config = createEmpty();
        config.setUrl(url);
        config.setDriverClassName(driver);
        config.addConnectProperty("user", user);
        config.addConnectProperty("password", password);

        BeeDataSourceConfig checkConfig = config.check();
        Object connectionFactory = checkConfig.getConnectionFactory();
        String fact_url = (String) TestUtil.getFieldValue(connectionFactory, "url");
        Properties configProperties = (Properties) TestUtil.getFieldValue(connectionFactory, "properties");

        Assert.assertEquals(configProperties.getProperty("user"), user);
        Assert.assertEquals(configProperties.getProperty("password"), password);
        Assert.assertEquals(fact_url, url);
    }
}
