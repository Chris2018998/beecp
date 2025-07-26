/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.config;

import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.test.base.TestUtil;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.stone.test.beecp.config.DsConfigFactory.*;

/**
 * Jdbc link info test
 *
 * @author Chris Liao
 */
public class Tc0003JdbcLinkInfoTest {
    private static final String user = "root";
    private static final String password = "test";
    private static final String url = MOCK_URL;
    private static final String driver = MOCK_DRIVER;

    @Test
    public void testNullSetAndGetOnConfig() {
        //situation1: check null setting
        BeeDataSourceConfig config1 = createEmpty();
        config1.setUsername(null);
        config1.setPassword(null);
        config1.setUrl(null);
        config1.setDriverClassName(null);
        assertNull(config1.getUsername());
        assertNull(config1.getPassword());
        assertNull(config1.getUrl());
        assertNull(config1.getDriverClassName());
        config1.setJdbcUrl(null);
        assertNull(config1.getUrl());
        assertNull(config1.getJdbcUrl());

        //situation2: check non-null setting
        BeeDataSourceConfig config2 = createEmpty();
        config2.setUsername(user);
        config2.setPassword(password);
        config2.setUrl(url);
        config2.setDriverClassName(driver);
        assertEquals(user, config2.getUsername());
        assertEquals(password, config2.getPassword());
        assertEquals(url, config2.getUrl());
        assertEquals(driver, config2.getDriverClassName());
        config2.setJdbcUrl(url);
        assertEquals(url, config2.getUrl());
        assertEquals(url, config2.getJdbcUrl());
    }

    @Test
    public void testValueSetAndGetOnConfig() throws Exception {
        BeeDataSourceConfig config = createEmpty();
        config.setUrl(url);
        config.setDriverClassName(driver);
        config.addConnectProperty("user", user);
        config.addConnectProperty("password", password);

        BeeDataSourceConfig checkConfig = config.check();
        Object connectionFactory = checkConfig.getConnectionFactory();
        String fact_url = (String) TestUtil.getFieldValue(connectionFactory, "url");
        Properties configProperties = (Properties) TestUtil.getFieldValue(connectionFactory, "properties");

        assertEquals(user, configProperties.getProperty("user"));
        assertEquals(password, configProperties.getProperty("password"));
        assertEquals(url, fact_url);
    }


    @Test
    public void testValueFromFromConstructor() {
        //situation1: check jdbc link info in constructor
        BeeDataSourceConfig config = new BeeDataSourceConfig(driver, url, user, password);
        assertEquals(user, config.getUsername());
        assertEquals(password, config.getPassword());
        assertEquals(url, config.getUrl());
        assertEquals(driver, config.getDriverClassName());

        //situation2: check properties in constructor
        Properties prop = new Properties();
        prop.setProperty("username", user);
        prop.setProperty("password", password);
        prop.setProperty("url", url);
        prop.setProperty("driverClassName", driver);
        BeeDataSourceConfig config2 = new BeeDataSourceConfig(prop);
        assertEquals(user, config2.getUsername());
        assertEquals(password, config2.getPassword());
        assertEquals(url, config2.getUrl());
        assertEquals(driver, config2.getDriverClassName());
    }

    @Test
    public void testValueLoadingFromProperties() {
        //situation1: load config from properties
        BeeDataSourceConfig config = createEmpty();
        Properties prop = new Properties();
        prop.setProperty("username", user);
        prop.setProperty("password", password);
        prop.setProperty("url", url);
        prop.setProperty("driverClassName", driver);
        config.loadFromProperties(prop);
        assertEquals(user, config.getUsername());
        assertEquals(password, config.getPassword());
        assertEquals(url, config.getUrl());
        assertEquals(driver, config.getDriverClassName());

        //situation2: config item name(contains middle line)
        prop.clear();
        prop.setProperty("jdbc-url", url);
        prop.setProperty("driver-class-name", driver);
        config = createEmpty();
        config.loadFromProperties(prop);
        assertEquals(url, config.getUrl());
        assertEquals(url, config.getJdbcUrl());
        assertEquals(driver, config.getDriverClassName());

        //situation3: config item name(contains under line)
        prop.clear();
        prop.setProperty("jdbc_url", url);
        prop.setProperty("driver_class_name", driver);
        config = createEmpty();
        config.loadFromProperties(prop);
        assertEquals(url, config.getUrl());
        assertEquals(url, config.getJdbcUrl());
        assertEquals(driver, config.getDriverClassName());
    }

    @Test
    public void testJdbcUrlMatchToDriver() throws Exception {
        BeeDataSourceConfig config1 = createDefault();

        //situation1: jdbc url check(can't be null)
        config1.setJdbcUrl(null);
        try {
            config1.check();
            fail("[testJdbcUrlMatchToDriver]Not threw exception when jdbc-url is null");
        } catch (Exception e) {
            String message = e.getMessage();
            assertTrue(message != null && message.contains("jdbcUrl can't be null"));
        }

        //situation2: load 'beecp.url' from system.properties
        clearBeeCPInfoFromSystemProperties();
        System.setProperty("beecp.url", url);
        System.setProperty("beecp.user", user);
        System.setProperty("beecp.password", password);

        try {
            BeeDataSourceConfig config2 = createEmpty();
            config2.setDriverClassName(driver);
            BeeDataSourceConfig checkConfig = config2.check();
            Object connectionFactory = checkConfig.getConnectionFactory();
            assertEquals(url, TestUtil.getFieldValue(connectionFactory, "url"));
            Properties configProperties = (Properties) TestUtil.getFieldValue(connectionFactory, "properties");
            assertEquals(user, configProperties.getProperty("user"));
            assertEquals(password, configProperties.getProperty("password"));

            //situation3: load 'beecp.jdbcUrl' from system.properties
            clearBeeCPInfoFromSystemProperties();
            System.setProperty("beecp.jdbcUrl", url);
            BeeDataSourceConfig config3 = createEmpty();
            config3.setDriverClassName(driver);
            connectionFactory = checkConfig.getConnectionFactory();
            assertEquals(url, TestUtil.getFieldValue(connectionFactory, "url"));
        } finally {
            System.clearProperty("beecp.url");
            System.clearProperty("beecp.user");
            System.clearProperty("beecp.password");
            System.clearProperty("beecp.jdbcUrl");
        }
    }
}
