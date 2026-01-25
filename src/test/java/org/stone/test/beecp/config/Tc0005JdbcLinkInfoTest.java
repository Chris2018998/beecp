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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.exception.BeeDataSourceConfigException;
import org.stone.test.base.TestUtil;

import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.stone.test.beecp.config.DsConfigFactory.*;

/**
 * Jdbc link info test
 *
 * @author Chris Liao
 */
public class Tc0005JdbcLinkInfoTest {
    private static final String user = "root";
    private static final String password = "test";
    private static final String url = MOCK_URL;
    private static final String driver = MOCK_DRIVER;

    @Test
    public void testSetAndGet() {
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        assertNull(config1.getUsername());
        assertNull(config1.getPassword());
        assertNull(config1.getUrl());
        assertNull(config1.getJdbcUrl());
        assertNull(config1.getDriverClassName());

        config1.setUsername(user);
        config1.setPassword(password);
        config1.setUrl(url);
        config1.setDriverClassName(driver);
        assertEquals(user, config1.getUsername());
        assertEquals(password, config1.getPassword());
        assertEquals(driver, config1.getDriverClassName());
        assertEquals(url, config1.getUrl());
        assertEquals(url, config1.getJdbcUrl());
        String newUrl = "jdbc:beecp://localhost/testdb2";
        config1.setJdbcUrl(newUrl);
        assertEquals(newUrl, config1.getUrl());
        assertEquals(newUrl, config1.getJdbcUrl());

        config1.setUsername(null);
        config1.setPassword(null);
        config1.setUrl(null);
        config1.setDriverClassName(null);
        assertNull(config1.getUsername());
        assertNull(config1.getPassword());
        assertNull(config1.getUrl());
        assertNull(config1.getJdbcUrl());
        assertNull(config1.getDriverClassName());

        BeeDataSourceConfig config2 = new BeeDataSourceConfig(driver, url, user, password);
        assertEquals(user, config2.getUsername());
        assertEquals(password, config2.getPassword());
        assertEquals(driver, config2.getDriverClassName());
        assertEquals(url, config2.getUrl());
        assertEquals(url, config2.getJdbcUrl());
    }

    @Test
    public void testLoadFromProperties() {
        Properties prop = new Properties();
        prop.setProperty("username", user);
        prop.setProperty("password", password);
        prop.setProperty("url", url);
        prop.setProperty("driverClassName", driver);

        //situation1: config item name(hump)
        BeeDataSourceConfig config = createEmpty();
        config.loadFromProperties(prop);
        assertEquals(user, config.getUsername());
        assertEquals(password, config.getPassword());
        assertEquals(url, config.getUrl());
        assertEquals(driver, config.getDriverClassName());

        //situation2: config item name(middle line)
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
    //priority order: Base Set ---> connectProperties ---> System.Properties
    public void testOrderCopyToConnectionFactory() throws Exception {

        try {
            //for set to addConnectProperty
            String newUser = "newUser1";
            String newPassword = "newPassword1";
            String newUrl = "jdbc:beecp://localhost/New_testdb";

            //for set to System.Properties
            String sysPropUser = "propUser";
            String sysPropPassword = "propPassword";
            String sysPropUrl1 = "jdbc:beecp://localhost/testdb";
            String sysPropUrl2 = "jdbc:beecp://localhost/testdb";
            String sysPropUrl3 = "jdbc:beecp://localhost/testdb";

            BeeDataSourceConfig config = new BeeDataSourceConfig();

            //1: base setting
            config.setDriverClassName(driver);
            config.setUrl(url);
            config.setUsername(user);
            config.setPassword(password);

            //2: add jdbc link info to connectProperties
            config.addConnectionFactoryProperty("user", newUser);
            config.addConnectionFactoryProperty("password", newPassword);
            config.addConnectionFactoryProperty("url", newUrl);

            //3: set to System.Properties
            clearBeeCPInfoFromSystemProperties();
            System.setProperty("beecp.user", sysPropUser);
            System.setProperty("beecp.password", sysPropPassword);
            System.setProperty("beecp.url", sysPropUrl1);
            System.setProperty("beecp.URL", sysPropUrl2);
            System.setProperty("beecp.jdbcUrl", sysPropUrl3);

            BeeDataSourceConfig checkConfig1 = config.check();
            Object connectionFactory = checkConfig1.getConnectionFactory();
            String fact_url = (String) TestUtil.getFieldValue(connectionFactory, "url");
            Properties configProperties = (Properties) TestUtil.getFieldValue(connectionFactory, "properties");
            assertEquals(user, configProperties.getProperty("user"));
            assertEquals(password, configProperties.getProperty("password"));
            assertEquals(url, fact_url);

            //copy2 test(priority from ‘addConnectProperty’)
            config.setUrl(null);
            config.setUsername(null);
            config.setPassword(null);
            BeeDataSourceConfig checkConfig2 = config.check();
            Object connectionFactory2 = checkConfig2.getConnectionFactory();
            String fact_url2 = (String) TestUtil.getFieldValue(connectionFactory2, "url");
            Properties configProperties2 = (Properties) TestUtil.getFieldValue(connectionFactory2, "properties");
            assertEquals(newUser, configProperties2.getProperty("user"));
            assertEquals(newPassword, configProperties2.getProperty("password"));
            assertEquals(newUrl, fact_url2);

            //copy31: test(priority from ‘System.property’)
            config.removeConnectionFactoryProperty("user");
            config.removeConnectionFactoryProperty("password");
            config.removeConnectionFactoryProperty("url");
            BeeDataSourceConfig checkConfig31 = config.check();
            Object connectionFactory31 = checkConfig31.getConnectionFactory();
            String fact_url31 = (String) TestUtil.getFieldValue(connectionFactory31, "url");
            Properties configProperties31 = (Properties) TestUtil.getFieldValue(connectionFactory31, "properties");
            assertEquals(sysPropUser, configProperties31.getProperty("user"));
            assertEquals(sysPropPassword, configProperties31.getProperty("password"));
            assertEquals(sysPropUrl1, fact_url31);

            //copy32: test(priority from ‘System.property’)
            System.clearProperty("beecp.url");
            BeeDataSourceConfig checkConfig32 = config.check();
            Object connectionFactory32 = checkConfig32.getConnectionFactory();
            String fact_url32 = (String) TestUtil.getFieldValue(connectionFactory32, "url");
            Properties configProperties32 = (Properties) TestUtil.getFieldValue(connectionFactory32, "properties");
            assertEquals(sysPropUser, configProperties32.getProperty("user"));
            assertEquals(sysPropPassword, configProperties32.getProperty("password"));
            assertEquals(sysPropUrl2, fact_url32);

            //copy33: test(priority from ‘System.property’)
            System.clearProperty("beecp.URL");
            BeeDataSourceConfig checkConfig33 = config.check();
            Object connectionFactory33 = checkConfig33.getConnectionFactory();
            String fact_url33 = (String) TestUtil.getFieldValue(connectionFactory33, "url");
            Properties configProperties33 = (Properties) TestUtil.getFieldValue(connectionFactory33, "properties");
            assertEquals(sysPropUser, configProperties33.getProperty("user"));
            assertEquals(sysPropPassword, configProperties33.getProperty("password"));
            assertEquals(sysPropUrl3, fact_url33);
        } finally {
            System.clearProperty("beecp.user");
            System.clearProperty("beecp.password");
            System.clearProperty("beecp.url");
            System.clearProperty("beecp.URL");
            System.clearProperty("beecp.jdbcUrl");
        }
    }

    @Test
    public void testCheckNotSettingBeeDataSourceConfig() throws SQLException {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        try {
            config.check();
            Assertions.fail("[testCheckNotSettingBeeDataSourceConfig]test failed");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertEquals("jdbcUrl must not be null or blank", e.getMessage());
        }
    }
}
