/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeXaConnectionFactory;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.test.base.TestUtil;
import org.stone.test.beecp.driver.MockXaDataSource;

import java.util.Properties;

/**
 * @author Chris Liao
 */
public class Tc0038XaDataSourcePropertyTest {
    private static final String url = "jdbc:runnable:test";
    private static final String user = "runnable";
    private static final String password = "root";
    private static final String property_Key = "key1";
    private static final String property_Value = "value1";
    private BeeDataSource ds;

    @BeforeEach
    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactoryClassName("org.stone.test.beecp.driver.MockXaDataSource");
        config.addConnectProperty("URL", url);
        config.addConnectProperty("user", user);
        config.addConnectProperty("password", password);
        Properties properties = new Properties();
        properties.setProperty(property_Key, property_Value);
        config.addConnectProperty("properties", properties);
        ds = new BeeDataSource(config);
    }

    @AfterEach
    public void tearDown() {
        ds.close();
    }

    @Test
    public void testDsProperty() throws Exception {
        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        BeeXaConnectionFactory rawXaConnFactory = (BeeXaConnectionFactory) TestUtil.getFieldValue(pool, "rawXaConnFactory");
        MockXaDataSource xaDs = (MockXaDataSource) TestUtil.getFieldValue(rawXaConnFactory, "dataSource");

        Assertions.assertEquals(user, xaDs.getUser());
        Assertions.assertEquals(password, xaDs.getPassword());
        Assertions.assertEquals(url, xaDs.getURL());
        Properties properties = xaDs.getProperties();
        Assertions.assertEquals(property_Value, properties.getProperty(property_Key));
    }
}