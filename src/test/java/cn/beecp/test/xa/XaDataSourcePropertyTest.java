/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test.xa;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import cn.beecp.RawXaConnectionFactory;
import cn.beecp.pool.FastConnectionPool;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;
import cn.beecp.test.mock.MockXaDataSource;

import java.util.Properties;

public class XaDataSourcePropertyTest extends TestCase {
    private final String url = "jdbc:mock:test";
    private final String user = "mock";
    private final String password = "root";
    private final String property_Key = "key1";
    private final String property_Value = "value1";
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactoryClassName("cn.beecp.test.mock.MockXaDataSource");
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

        if (!user.equals(xaDs.getUser()))
            TestUtil.assertError("user expect value:%s,actual value:%s", user, xaDs.getUser());
        if (!password.equals(xaDs.getPassword()))
            TestUtil.assertError("password expect value:%s,actual value:%s", password, xaDs.getPassword());
        if (!url.equals(xaDs.getURL()))
            TestUtil.assertError("url expect value:%s,actual value:%s", url, xaDs.getURL());
        Properties properties = xaDs.getProperties();
        if (!property_Value.equals(properties.getProperty(property_Key)))
            TestUtil.assertError("property expect value:%s,actual value:%s", property_Value, properties.getProperty(property_Key));
    }
}