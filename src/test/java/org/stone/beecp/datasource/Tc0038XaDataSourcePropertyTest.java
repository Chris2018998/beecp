/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.datasource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeXaConnectionFactory;
import org.stone.beecp.driver.MockXaDataSource;
import org.stone.beecp.pool.FastConnectionPool;

import java.util.Properties;

/**
 * @author Chris Liao
 */
public class Tc0038XaDataSourcePropertyTest extends TestCase {
    private static final String url = "jdbc:runnable:test";
    private static final String user = "runnable";
    private static final String password = "root";
    private static final String property_Key = "key1";
    private static final String property_Value = "value1";
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactoryClassName("org.stone.beecp.driver.MockXaDataSource");
        config.addConnectProperty("URL", url);
        config.addConnectProperty("user", user);
        config.addConnectProperty("password", password);
        Properties properties = new Properties();
        properties.setProperty(property_Key, property_Value);
        config.addConnectProperty("properties", properties);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testDsProperty() throws Exception {
        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        BeeXaConnectionFactory rawXaConnFactory = (BeeXaConnectionFactory) TestUtil.getFieldValue(pool, "rawXaConnFactory");
        MockXaDataSource xaDs = (MockXaDataSource) TestUtil.getFieldValue(rawXaConnFactory, "dataSource");

        Assert.assertEquals(user, xaDs.getUser());
        Assert.assertEquals(password, xaDs.getPassword());
        Assert.assertEquals(url, xaDs.getURL());
        Properties properties = xaDs.getProperties();
        Assert.assertEquals(property_Value, properties.getProperty(property_Key));
    }
}