/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.password;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;
import org.stone.beecp.pool.ConnectionFactoryByDriver;
import org.stone.beecp.pool.FastConnectionPool;

import java.util.Properties;

public class DataSourcePasswordDecodeTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword("123");
        config.setJdbcLinkInfDecoderClassName("org.stone.beecp.password.DatabasePasswordDecoder");
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