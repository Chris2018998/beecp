/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test.password;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import cn.beecp.pool.ConnectionFactoryByDriver;
import cn.beecp.pool.FastConnectionPool;
import cn.beecp.test.JdbcConfig;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;

import java.util.Properties;

public class DataSourcePasswordDecodeTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword("123");
        config.setJdbcLinkInfDecoderClassName("cn.beecp.test.password.DatabasePasswordDecoder");
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws Exception {
        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        ConnectionFactoryByDriver rawConnFactory = (ConnectionFactoryByDriver) TestUtil.getFieldValue(pool, "rawConnFactory");
        Properties properties = (Properties) TestUtil.getFieldValue(rawConnFactory, "properties");

        if (!DatabasePasswordDecoder.password().equals(properties.getProperty("password")))
            TestUtil.assertError("property expect value:%s,actual value:%s", DatabasePasswordDecoder.password(), properties.getProperty("password"));
    }
}