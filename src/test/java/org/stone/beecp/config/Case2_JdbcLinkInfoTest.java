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
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.SQLException;
import java.util.Properties;

public class Case2_JdbcLinkInfoTest extends TestCase {
    private final String user = "root";
    private final String password = "test";
    private final String url = "jdbc:beecp://localhost/testdb";
    private final String driver = "org.stone.beecp.mock.MockDriver";

    public void testNullSet() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setUsername(null);
        if (config.getUsername() != null) throw new TestException();
        config.setPassword(null);
        if (config.getPassword() != null) throw new TestException();
        config.setUrl(null);
        if (config.getUrl() != null) throw new TestException();
        config.setJdbcUrl(null);
        if (config.getJdbcUrl() != null) throw new TestException();
        config.setDriverClassName(null);
        if (config.getDriverClassName() != null) throw new TestException();
    }

    public void testNotNullSet() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setUsername(user);
        if (!user.equals(config.getUsername())) throw new TestException();
        config.setPassword(password);
        if (!password.equals(config.getPassword())) throw new TestException();

        config.setUrl(url);
        if (!url.equals(config.getUrl())) throw new TestException();
        if (!url.equals(config.getJdbcUrl())) throw new TestException();
        config.setJdbcUrl(url);
        if (!url.equals(config.getUrl())) throw new TestException();
        if (!url.equals(config.getJdbcUrl())) throw new TestException();

        config.setDriverClassName(driver);
        if (!driver.equals(config.getDriverClassName())) throw new TestException();
    }

    public void testOnConstructor() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig(driver, url, user, password);
        if (!user.equals(config.getUsername())) throw new TestException();
        if (!password.equals(config.getPassword())) throw new TestException();
        if (!url.equals(config.getUrl())) throw new TestException();
        if (!url.equals(config.getJdbcUrl())) throw new TestException();
        if (!driver.equals(config.getDriverClassName())) throw new TestException();

        Properties prop = new Properties();
        prop.setProperty("username", user);
        prop.setProperty("password", password);
        prop.setProperty("url", url);
        prop.setProperty("driverClassName", driver);
        config = new BeeDataSourceConfig(prop);
        if (!user.equals(config.getUsername())) throw new TestException();
        if (!password.equals(config.getPassword())) throw new TestException();
        if (!url.equals(config.getUrl())) throw new TestException();
        if (!url.equals(config.getJdbcUrl())) throw new TestException();
        if (!driver.equals(config.getDriverClassName())) throw new TestException();
    }

    public void testLoadFromProperties() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Properties prop = new Properties();
        prop.setProperty("username", user);
        prop.setProperty("password", password);
        prop.setProperty("url", url);
        prop.setProperty("driverClassName", driver);
        config.loadFromProperties(prop);
        if (!user.equals(config.getUsername())) throw new TestException();
        if (!password.equals(config.getPassword())) throw new TestException();
        if (!url.equals(config.getUrl())) throw new TestException();
        if (!url.equals(config.getJdbcUrl())) throw new TestException();
        if (!driver.equals(config.getDriverClassName())) throw new TestException();

        //middle line test
        prop.clear();
        prop.setProperty("jdbc-url", url);
        prop.setProperty("driver-class-name", driver);
        config = new BeeDataSourceConfig();
        config.loadFromProperties(prop);
        if (!url.equals(config.getUrl())) throw new TestException();
        if (!url.equals(config.getJdbcUrl())) throw new TestException();
        if (!driver.equals(config.getDriverClassName())) throw new TestException();

        //under line test
        prop.clear();
        prop.setProperty("jdbc_url", url);
        prop.setProperty("driver_class_name", driver);
        config = new BeeDataSourceConfig();
        config.loadFromProperties(prop);
        if (!url.equals(config.getUrl())) throw new TestException();
        if (!url.equals(config.getJdbcUrl())) throw new TestException();
        if (!driver.equals(config.getDriverClassName())) throw new TestException();
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

        if (!user.equals(configProperties.getProperty("user"))) throw new TestException();
        if (!password.equals(configProperties.getProperty("password"))) throw new TestException();
        if (!url.equals(fact_url)) throw new TestException();

        System.clearProperty("beecp.url");
        System.setProperty("beecp.jdbcUrl", url);
        checkConfig = config.check();
        connectionFactory = checkConfig.getConnectionFactory();
        fact_url = (String) TestUtil.getFieldValue(connectionFactory, "url");
        configProperties = (Properties) TestUtil.getFieldValue(connectionFactory, "properties");

        if (!user.equals(configProperties.getProperty("user"))) throw new TestException();
        if (!password.equals(configProperties.getProperty("password"))) throw new TestException();
        if (!url.equals(fact_url)) throw new TestException();
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

        if (!user.equals(configProperties.getProperty("user"))) throw new TestException();
        if (!password.equals(configProperties.getProperty("password"))) throw new TestException();
        if (!url.equals(fact_url)) throw new TestException();
    }

    public void testCheckOnNullJdbcUrl() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        try {
            config.check();
        } catch (SQLException e) {
            if (!TestUtil.containsMessage(e, "jdbcUrl can't be null")) throw new TestException();
        }
    }
}
