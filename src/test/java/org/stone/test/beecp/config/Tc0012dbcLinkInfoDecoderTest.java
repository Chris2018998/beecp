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
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.BeeJdbcLinkInfoDecoder;
import org.stone.test.base.TestUtil;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;
import org.stone.test.beecp.objects.SampleMockJdbcLinkInfoDecoder;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.*;

/**
 * @author Chris Liao
 */
public class Tc0012dbcLinkInfoDecoderTest {
    private static final String username = "user";
    private static final String password = "password";
    private static final String url = MOCK_URL;
    private static final String driver = MOCK_DRIVER;

    @Test
    public void testOnSetGet() {
        BeeDataSourceConfig config = createEmpty();
        Class<? extends BeeJdbcLinkInfoDecoder> decodeClass = SampleMockJdbcLinkInfoDecoder.class;
        config.setJdbcLinkInfoDecoderClass(decodeClass);
        Assertions.assertEquals(decodeClass, config.getJdbcLinkInfoDecoderClass());

        String decodeClassName = "org.stone.beecp.objects.SampleJdbcLinkInfoDecoder";
        config.setJdbcLinkInfoDecoderClassName(decodeClassName);
        Assertions.assertEquals(decodeClassName, config.getJdbcLinkInfoDecoderClassName());

        SampleMockJdbcLinkInfoDecoder decoder = new SampleMockJdbcLinkInfoDecoder();
        config.setJdbcLinkInfoDecoder(decoder);
        Assertions.assertEquals(config.getJdbcLinkInfoDecoder(), decoder);
    }

    @Test
    public void testOnCreation() throws Exception {
        BeeDataSourceConfig config1 = new BeeDataSourceConfig(driver, url, username, password);
        config1.setJdbcLinkInfoDecoder(new SampleMockJdbcLinkInfoDecoder());
        config1.setConnectionFactoryClass(MockCommonConnectionFactory.class);
        BeeDataSourceConfig checkConfig = config1.check();

        MockCommonConnectionFactory factory = (MockCommonConnectionFactory) checkConfig.getConnectionFactory();
        String url = factory.getUrl();
        String user = factory.getUser();
        String password = factory.getPassword();
        Assertions.assertTrue(url.endsWith("-Decoded"));
        Assertions.assertTrue(user.endsWith("-Decoded"));
        Assertions.assertTrue(password.endsWith("-Decoded"));

        BeeDataSourceConfig config2 = new BeeDataSourceConfig(driver, url, username, password);
        config2.setConnectionFactoryClass(MockCommonConnectionFactory.class);
        checkConfig = config2.check();
        factory = (MockCommonConnectionFactory) checkConfig.getConnectionFactory();

        String decodedUrl = factory.getUrl();
        String decodedUser = factory.getUser();
        String decodedPassword = factory.getPassword();
        Assertions.assertEquals(url, decodedUrl);
        Assertions.assertEquals(username, decodedUser);
        Assertions.assertEquals(password, decodedPassword);

        BeeDataSourceConfig config3 = new BeeDataSourceConfig(driver, url, username, password);
        config3.setConnectionFactoryClass(MockCommonConnectionFactory.class);
        config3.setJdbcLinkInfoDecoderClassName("org.stone.test.beecp.objects.SampleMockJdbcLinkInfoDecoder");
        checkConfig = config3.check();
        factory = (MockCommonConnectionFactory) checkConfig.getConnectionFactory();
        url = factory.getUrl();
        user = factory.getUser();
        password = factory.getPassword();
        Assertions.assertTrue(url.endsWith("-Decoded"));
        Assertions.assertTrue(user.endsWith("-Decoded"));
        Assertions.assertTrue(password.endsWith("-Decoded"));

        BeeDataSourceConfig config4 = new BeeDataSourceConfig(driver, url, username, password);
        config4.setConnectionFactoryClass(MockCommonConnectionFactory.class);
        config4.setJdbcLinkInfoDecoderClassName("org.stone.beecp.BeeJdbcLinkInfoDecoder");
        checkConfig = config4.check();
        factory = (MockCommonConnectionFactory) checkConfig.getConnectionFactory();
        String factoryUrl = factory.getUrl();
        String factoryUser = factory.getUser();
        String factoryPassword = factory.getPassword();
        Assertions.assertEquals(url, factoryUrl);
        Assertions.assertEquals(username, factoryUser);
        Assertions.assertEquals(password, factoryPassword);
    }

    @Test
    public void testOnErrorClass() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig(driver, url, username, password);
        config.setJdbcLinkInfoDecoderClassName("java.lang.String");//error config
        try {
            config.check();
            fail("[testOnErrorClass]not threw exception when check invalid className of Jdbc-linkInfo-decoder");
        } catch (BeeDataSourceConfigException e) {
            Throwable cause = e.getCause();
            Assertions.assertNotNull(cause);
            String message = cause.getMessage();
            Assertions.assertTrue(message != null && message.contains("decoder"));
        }
    }

    @Test
    public void testOnErrorClassName() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig(driver, url, username, password);

        config.setJdbcLinkInfoDecoderClassName("String");//error config
        try {
            config.check();
            fail("[testOnErrorClassName]not threw exception when check invalid className of Jdbc-linkInfo-decoder");
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("decoder"));
        }
    }

    /****************************************************Decode execution Test ****************************************/
    @Test
    public void testJdbcDecoderOnDriver() throws Exception {
        BeeDataSourceConfig config1 = new BeeDataSourceConfig(driver, url, null, null);
        config1.setJdbcLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig1 = config1.check();
        Object factory1 = TestUtil.getFieldValue(checkedConfig1, "connectionFactory");
        Properties properties1 = (Properties) TestUtil.getFieldValue(factory1, "properties");
        Assertions.assertFalse(properties1.contains("user"));
        Assertions.assertFalse(properties1.contains("password"));

        BeeDataSourceConfig config2 = new BeeDataSourceConfig(driver, url, username, password);
        config2.setJdbcLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig2 = config2.check();
        Object factory2 = TestUtil.getFieldValue(checkedConfig2, "connectionFactory");
        String url2 = (String) TestUtil.getFieldValue(factory2, "url");
        Properties properties = (Properties) TestUtil.getFieldValue(factory2, "properties");
        String user2 = properties.getProperty("user");
        String password2 = properties.getProperty("password");
        Assertions.assertTrue(url2.endsWith("-Decoded"));
        Assertions.assertTrue(user2.endsWith("-Decoded"));
        Assertions.assertTrue(password2.endsWith("-Decoded"));

        BeeDataSourceConfig config3 = new BeeDataSourceConfig(driver, url, username, null);
        config3.setJdbcLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig3 = config3.check();
        Object factory3 = TestUtil.getFieldValue(checkedConfig3, "connectionFactory");
        String url3 = (String) TestUtil.getFieldValue(factory3, "url");
        Properties properties3 = (Properties) TestUtil.getFieldValue(factory3, "properties");
        String user3 = properties3.getProperty("user");
        String password3 = properties3.getProperty("password");

        Assertions.assertTrue(url3.endsWith("-Decoded"));
        Assertions.assertTrue(user3.endsWith("-Decoded"));
        Assertions.assertNull(password3);
    }

    @Test
    public void testJdbcDecoderOnFactory() throws Exception {
        clearBeeCPInfoFromSystemProperties();
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.setConnectionFactoryClass(MockCommonConnectionFactory.class);
        config1.setJdbcLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig1 = config1.check();
        MockCommonConnectionFactory factory1 = (MockCommonConnectionFactory) checkedConfig1.getConnectionFactory();
        Assertions.assertNull(factory1.getUser());
        Assertions.assertNull(factory1.getPassword());

        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setUsername(username);
        config2.setConnectionFactoryClass(MockCommonConnectionFactory.class);
        config2.setJdbcLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig2 = config2.check();
        MockCommonConnectionFactory factory2 = (MockCommonConnectionFactory) checkedConfig2.getConnectionFactory();
        Assertions.assertTrue(factory2.getUser().endsWith("-Decoded"));
        Assertions.assertNull(factory2.getPassword());

        BeeDataSourceConfig config3 = new BeeDataSourceConfig();
        config3.setUrl(url);
        config3.setUsername(username);
        config3.setPassword(password);
        config3.setConnectionFactoryClass(MockCommonConnectionFactory.class);
        config3.setJdbcLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig3 = config3.check();
        MockCommonConnectionFactory factory3 = (MockCommonConnectionFactory) checkedConfig3.getConnectionFactory();
        Assertions.assertTrue(factory3.getUrl().endsWith("-Decoded"));
        Assertions.assertTrue(factory3.getUser().endsWith("-Decoded"));
        Assertions.assertTrue(factory3.getPassword().endsWith("-Decoded"));

        BeeDataSourceConfig config4 = new BeeDataSourceConfig();
        config4.addConnectProperty("url", url);
        config4.addConnectProperty("user", username);
        config4.addConnectProperty("password", password);
        config4.setConnectionFactoryClass(MockCommonConnectionFactory.class);
        config4.setJdbcLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig4 = config4.check();
        MockCommonConnectionFactory factory4 = (MockCommonConnectionFactory) checkedConfig4.getConnectionFactory();
        Assertions.assertTrue(factory4.getUrl().endsWith("-Decoded"));
        Assertions.assertTrue(factory4.getUser().endsWith("-Decoded"));
        Assertions.assertTrue(factory4.getPassword().endsWith("-Decoded"));

        try {
            BeeDataSourceConfig config5 = new BeeDataSourceConfig();
            clearBeeCPInfoFromSystemProperties();
            System.setProperty("beecp.url", url);
            System.setProperty("beecp.user", username);
            System.setProperty("beecp.password", password);
            config5.setConnectionFactoryClass(MockCommonConnectionFactory.class);
            config5.setJdbcLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
            BeeDataSourceConfig checkedConfig5 = config5.check();
            MockCommonConnectionFactory factory5 = (MockCommonConnectionFactory) checkedConfig5.getConnectionFactory();
            Assertions.assertTrue(factory5.getUrl().endsWith("-Decoded"));
            Assertions.assertTrue(factory5.getUser().endsWith("-Decoded"));
            Assertions.assertTrue(factory5.getPassword().endsWith("-Decoded"));
        } finally {
            System.clearProperty("beecp.url");
            System.clearProperty("beecp.user");
            System.clearProperty("beecp.password");
        }
    }
}
