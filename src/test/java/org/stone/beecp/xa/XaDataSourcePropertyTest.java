/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.xa;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.RawXaConnectionFactory;
import org.stone.beecp.mock.MockXaDataSource;
import org.stone.beecp.pool.FastConnectionPool;

import java.util.Properties;

public class XaDataSourcePropertyTest extends TestCase {
    private final String url = "jdbc:runnable:test";
    private final String user = "runnable";
    private final String password = "root";
    private final String property_Key = "key1";
    private final String property_Value = "value1";
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactoryClassName("org.stone.beecp.mock.MockXaDataSource");
        config.addConnectProperty("URL", url);
        config.addConnectProperty("user", user);
        config.addConnectProperty("password", password);
        Properties properties = new Properties();
        properties.setProperty(property_Key, property_Value);
        config.addConnectProperty("properties", properties);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws Exception {
        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        RawXaConnectionFactory rawXaConnFactory = (RawXaConnectionFactory) TestUtil.getFieldValue(pool, "rawXaConnFactory");
        MockXaDataSource xaDs = (MockXaDataSource) TestUtil.getFieldValue(rawXaConnFactory, "dataSource");

        TestUtil.assertError("user expect value:%s,actual value:%s", user, xaDs.getUser());
        TestUtil.assertError("password expect value:%s,actual value:%s", password, xaDs.getPassword());
        TestUtil.assertError("url expect value:%s,actual value:%s", url, xaDs.getURL());
        Properties properties = xaDs.getProperties();
        TestUtil.assertError("property expect value:%s,actual value:%s", property_Value, properties.getProperty(property_Key));
    }
}