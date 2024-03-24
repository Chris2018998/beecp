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
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.RawXaConnectionFactory;
import org.stone.beecp.config.customization.DummyJdbcLinkInfoDecoder;
import org.stone.beecp.config.customization.NullConnectionFactory;
import org.stone.beecp.mock.MockXaDataSource;
import org.stone.beecp.pool.FastConnectionPool;

import java.util.Properties;

public class JdbcDecoderExecuteTest extends TestCase {

    public void testJdbcDecoderOnDriver() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setUsername("user");
        config.setPassword("passwor");
        config.setUrl("jdbc:beecp://localhost/testdb");
        config.setDriverClassName("org.stone.beecp.mock.MockDriver");
        config.setJdbcLinkInfoDecoderClass(DummyJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig = config.check();
        Object factory = TestUtil.getFieldValue(checkedConfig, "connectionFactory");
        String url = (String) TestUtil.getFieldValue(factory, "url");
        Properties properties = (Properties) TestUtil.getFieldValue(factory, "properties");
        String user = properties.getProperty("user");
        String password = properties.getProperty("password");
        if (!url.endsWith("-Decoded")) throw new TestException();
        if (!user.endsWith("-Decoded")) throw new TestException();
        if (!password.endsWith("-Decoded")) throw new TestException();
    }

    public void testJdbcDecoderOnFactory() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setUsername("user");
        config.setPassword("passwor");
        config.setUrl("jdbc:beecp://localhost/testdb");
        config.setConnectionFactoryClass(NullConnectionFactory.class);
        config.setJdbcLinkInfoDecoderClass(DummyJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig = config.check();

        NullConnectionFactory factory = (NullConnectionFactory) TestUtil.getFieldValue(checkedConfig, "connectionFactory");

        String url = factory.getUrl();
        String user = factory.getUser();
        String password = factory.getPassword();
        if (!url.endsWith("-Decoded")) throw new TestException();
        if (!user.endsWith("-Decoded")) throw new TestException();
        if (!password.endsWith("-Decoded")) throw new TestException();
    }

    public void testJdbcDecoderOnXaDataSource() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl("jdbc:mock:test");
        config.setUsername("mock");
        config.setPassword("root");
        config.setConnectionFactoryClassName("org.stone.beecp.mock.MockXaDataSource");
        config.setJdbcLinkInfDecoderClassName("org.stone.beecp.config.customization.DummyJdbcLinkInfoDecoder");
        BeeDataSource ds = new BeeDataSource(config);

        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        RawXaConnectionFactory rawXaConnFactory = (RawXaConnectionFactory) TestUtil.getFieldValue(pool, "rawXaConnFactory");
        MockXaDataSource xaDs = (MockXaDataSource) TestUtil.getFieldValue(rawXaConnFactory, "dataSource");

        String url = xaDs.getURL();
        String user = xaDs.getUser();
        String password = xaDs.getPassword();
        if (!url.endsWith("-Decoded")) throw new TestException();
        if (!user.endsWith("-Decoded")) throw new TestException();
        if (!password.endsWith("-Decoded")) throw new TestException();
    }

    public void testJdbcDecoderOnXaDataSource2() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactoryClassName("org.stone.beecp.mock.MockXaDataSource");
        config.setJdbcLinkInfDecoderClassName("org.stone.beecp.config.customization.DummyJdbcLinkInfoDecoder");
        config.addConnectProperty("URL", "jdbc:mock:test");
        config.addConnectProperty("user", "mock");
        config.addConnectProperty("password", "root");
        BeeDataSource ds = new BeeDataSource(config);

        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        RawXaConnectionFactory rawXaConnFactory = (RawXaConnectionFactory) TestUtil.getFieldValue(pool, "rawXaConnFactory");
        MockXaDataSource xaDs = (MockXaDataSource) TestUtil.getFieldValue(rawXaConnFactory, "dataSource");

        String url = xaDs.getURL();
        String user = xaDs.getUser();
        String password = xaDs.getPassword();
        if (!url.endsWith("-Decoded")) throw new TestException();
        if (!user.endsWith("-Decoded")) throw new TestException();
        if (!password.endsWith("-Decoded")) throw new TestException();
    }
}