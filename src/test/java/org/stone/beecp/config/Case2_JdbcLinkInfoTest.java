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

import java.sql.SQLException;
import java.util.Properties;

public class Case2_JdbcLinkInfoTest extends TestCase {
    private final String user = "root";
    private final String password = "test";
    private final String url = "jdbc:beecp://localhost/testdb";
    private final String driver = "org.stone.beecp.mock.MockDriver";

    public void testNullSet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
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
    }

    public void testNotNullSet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setUsername(user);
        config.setPassword(password);
        config.setUrl(url);
        config.setDriverClassName(driver);

        Assert.assertEquals(config.getUsername(), user);
        Assert.assertEquals(config.getPassword(), password);
        Assert.assertEquals(config.getUrl(), url);
        Assert.assertEquals(config.getDriverClassName(), driver);

        config.setJdbcUrl(url);
        Assert.assertEquals(config.getUrl(), url);
        Assert.assertEquals(config.getJdbcUrl(), url);
    }

    public void testOnConstructor() {
        BeeDataSourceConfig config = new BeeDataSourceConfig(driver, url, user, password);
        Assert.assertEquals(config.getUsername(), user);
        Assert.assertEquals(config.getPassword(), password);
        Assert.assertEquals(config.getUrl(), url);
        Assert.assertEquals(config.getDriverClassName(), driver);

        Properties prop = new Properties();
        prop.setProperty("username", user);
        prop.setProperty("password", password);
        prop.setProperty("url", url);
        prop.setProperty("driverClassName", driver);
        config = new BeeDataSourceConfig(prop);
        Assert.assertEquals(config.getUsername(), user);
        Assert.assertEquals(config.getPassword(), password);
        Assert.assertEquals(config.getUrl(), url);
        Assert.assertEquals(config.getDriverClassName(), driver);
    }

    public void testLoadFromProperties() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
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

        //middle line test
        prop.clear();
        prop.setProperty("jdbc-url", url);
        prop.setProperty("driver-class-name", driver);
        config = new BeeDataSourceConfig();
        config.loadFromProperties(prop);
        Assert.assertEquals(config.getUrl(), url);
        Assert.assertEquals(config.getJdbcUrl(), url);
        Assert.assertEquals(config.getDriverClassName(), driver);


        //under line test
        prop.clear();
        prop.setProperty("jdbc_url", url);
        prop.setProperty("driver_class_name", driver);
        config = new BeeDataSourceConfig();
        config.loadFromProperties(prop);
        Assert.assertEquals(config.getUrl(), url);
        Assert.assertEquals(config.getJdbcUrl(), url);
        Assert.assertEquals(config.getDriverClassName(), driver);
    }

    public void testLoadFromSystemProperties() throws Exception {
        System.setProperty("beecp.url", url);
        System.setProperty("beecp.user", user);
        System.setProperty("beecp.password", password);
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setDriverClassName(driver);

        BeeDataSourceConfig checkConfig = config.check();
        Object connectionFactory = checkConfig.getConnectionFactory();
        String fact_url = (String) TestUtil.getFieldValue(connectionFactory, "url");
        Properties configProperties = (Properties) TestUtil.getFieldValue(connectionFactory, "properties");

        Assert.assertEquals(configProperties.getProperty("user"), user);
        Assert.assertEquals(configProperties.getProperty("password"), password);
        Assert.assertEquals(fact_url, url);

        System.clearProperty("beecp.url");
        System.setProperty("beecp.jdbcUrl", url);
        checkConfig = config.check();
        connectionFactory = checkConfig.getConnectionFactory();
        fact_url = (String) TestUtil.getFieldValue(connectionFactory, "url");
        configProperties = (Properties) TestUtil.getFieldValue(connectionFactory, "properties");

        Assert.assertEquals(configProperties.getProperty("user"), user);
        Assert.assertEquals(configProperties.getProperty("password"), password);
        Assert.assertEquals(fact_url, url);
    }

    public void testLoadFromConnectProperties() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
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

    public void testCheckOnNullJdbcUrl() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        try {
            config.check();
        } catch (SQLException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("jdbcUrl can't be null"));
        }
    }
}
