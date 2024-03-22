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

import java.util.Properties;

public class JdbcLinkInfoDecodeTest extends TestCase {

    public void testJdbcDecoderOnDriver() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setUsername("user");
        config.setPassword("passwor");
        config.setUrl("jdbc:beecp://localhost/testdb");
        config.setDriverClassName("org.stone.beecp.mock.MockDriver");
        config.setJdbcLinkInfoDecoderClass(org.stone.beecp.config.DummyJdbcLinkInfoDecoder.class);
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
        config.setConnectionFactoryClass(DummyRawConnectionFactory.class);
        config.setJdbcLinkInfoDecoderClass(org.stone.beecp.config.DummyJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig = config.check();

        Object factory = TestUtil.getFieldValue(checkedConfig, "connectionFactory");
        String url = (String) TestUtil.getFieldValue(factory, "url");
        String user = (String) TestUtil.getFieldValue(factory, "user");
        String password = (String) TestUtil.getFieldValue(factory, "password");
        if (!url.endsWith("-Decoded")) throw new TestException();
        if (!user.endsWith("-Decoded")) throw new TestException();
        if (!password.endsWith("-Decoded")) throw new TestException();
    }
}