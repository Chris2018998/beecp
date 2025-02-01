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
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.BeeJdbcLinkInfoDecoder;
import org.stone.beecp.objects.MockCommonConnectionFactory;
import org.stone.beecp.objects.SampleMockJdbcLinkInfoDecoder;

import java.util.Properties;

import static org.stone.beecp.config.DsConfigFactory.*;

/**
 * @author Chris Liao
 */
public class Tc0012dbcLinkInfoDecoderTest extends TestCase {
    private static final String username = "user";
    private static final String password = "password";
    private static final String url = MOCK_URL;
    private static final String driver = MOCK_DRIVER;


    public void testOnSetGet() {
        BeeDataSourceConfig config = createEmpty();
        Class<? extends BeeJdbcLinkInfoDecoder> decodeClass = SampleMockJdbcLinkInfoDecoder.class;
        config.setJdbcLinkInfoDecoderClass(decodeClass);
        Assert.assertEquals(decodeClass, config.getJdbcLinkInfoDecoderClass());

        String decodeClassName = "org.stone.beecp.objects.SampleJdbcLinkInfoDecoder";
        config.setJdbcLinkInfoDecoderClassName(decodeClassName);
        Assert.assertEquals(decodeClassName, config.getJdbcLinkInfoDecoderClassName());

        SampleMockJdbcLinkInfoDecoder decoder = new SampleMockJdbcLinkInfoDecoder();
        config.setJdbcLinkInfoDecoder(decoder);
        Assert.assertEquals(config.getJdbcLinkInfoDecoder(), decoder);
    }

    public void testOnCreation() throws Exception {
        BeeDataSourceConfig config1 = new BeeDataSourceConfig(driver, url, username, password);
        config1.setJdbcLinkInfoDecoder(new SampleMockJdbcLinkInfoDecoder());
        config1.setConnectionFactoryClass(MockCommonConnectionFactory.class);
        BeeDataSourceConfig checkConfig = config1.check();

        MockCommonConnectionFactory factory = (MockCommonConnectionFactory) checkConfig.getConnectionFactory();
        String url = factory.getUrl();
        String user = factory.getUser();
        String password = factory.getPassword();
        Assert.assertTrue(url.endsWith("-Decoded"));
        Assert.assertTrue(user.endsWith("-Decoded"));
        Assert.assertTrue(password.endsWith("-Decoded"));

        BeeDataSourceConfig config2 = new BeeDataSourceConfig(driver, url, username, password);
        config2.setConnectionFactoryClass(MockCommonConnectionFactory.class);
        checkConfig = config2.check();
        factory = (MockCommonConnectionFactory) checkConfig.getConnectionFactory();

        String decodedUrl = factory.getUrl();
        String decodedUser = factory.getUser();
        String decodedPassword = factory.getPassword();
        Assert.assertEquals(url, decodedUrl);
        Assert.assertEquals(username, decodedUser);
        Assert.assertEquals(password, decodedPassword);

        BeeDataSourceConfig config3 = new BeeDataSourceConfig(driver, url, username, password);
        config3.setConnectionFactoryClass(MockCommonConnectionFactory.class);
        config3.setJdbcLinkInfoDecoderClassName("org.stone.beecp.objects.SampleMockJdbcLinkInfoDecoder");
        checkConfig = config3.check();
        factory = (MockCommonConnectionFactory) checkConfig.getConnectionFactory();
        url = factory.getUrl();
        user = factory.getUser();
        password = factory.getPassword();
        Assert.assertTrue(url.endsWith("-Decoded"));
        Assert.assertTrue(user.endsWith("-Decoded"));
        Assert.assertTrue(password.endsWith("-Decoded"));

        BeeDataSourceConfig config4 = new BeeDataSourceConfig(driver, url, username, password);
        config4.setConnectionFactoryClass(MockCommonConnectionFactory.class);
        config4.setJdbcLinkInfoDecoderClassName("org.stone.beecp.BeeJdbcLinkInfoDecoder");
        checkConfig = config4.check();
        factory = (MockCommonConnectionFactory) checkConfig.getConnectionFactory();
        String factoryUrl = factory.getUrl();
        String factoryUser = factory.getUser();
        String factoryPassword = factory.getPassword();
        Assert.assertEquals(url, factoryUrl);
        Assert.assertEquals(username, factoryUser);
        Assert.assertEquals(password, factoryPassword);
    }

    public void testOnErrorClass() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig(driver, url, username, password);
        config.setJdbcLinkInfoDecoderClassName("java.lang.String");//error config
        try {
            config.check();
        } catch (BeeDataSourceConfigException e) {
            Throwable cause = e.getCause();
            Assert.assertNotNull(cause);
            String message = cause.getMessage();
            Assert.assertTrue(message != null && message.contains("decoder"));
        }
    }

    public void testOnErrorClassName() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig(driver, url, username, password);

        config.setJdbcLinkInfoDecoderClassName("String");//error config
        try {
            config.check();
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("decoder"));
        }
    }

    /****************************************************Decode execution Test ****************************************/
    public void testJdbcDecoderOnDriver() throws Exception {
        BeeDataSourceConfig config1 = new BeeDataSourceConfig(driver, url, null, null);
        config1.setJdbcLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig1 = config1.check();
        Object factory1 = TestUtil.getFieldValue(checkedConfig1, "connectionFactory");
        Properties properties1 = (Properties) TestUtil.getFieldValue(factory1, "properties");
        Assert.assertFalse(properties1.contains("user"));
        Assert.assertFalse(properties1.contains("password"));

        BeeDataSourceConfig config2 = new BeeDataSourceConfig(driver, url, username, password);
        config2.setJdbcLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig2 = config2.check();
        Object factory2 = TestUtil.getFieldValue(checkedConfig2, "connectionFactory");
        String url2 = (String) TestUtil.getFieldValue(factory2, "url");
        Properties properties = (Properties) TestUtil.getFieldValue(factory2, "properties");
        String user2 = properties.getProperty("user");
        String password2 = properties.getProperty("password");
        Assert.assertTrue(url2.endsWith("-Decoded"));
        Assert.assertTrue(user2.endsWith("-Decoded"));
        Assert.assertTrue(password2.endsWith("-Decoded"));

        BeeDataSourceConfig config3 = new BeeDataSourceConfig(driver, url, username, null);
        config3.setJdbcLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig3 = config3.check();
        Object factory3 = TestUtil.getFieldValue(checkedConfig3, "connectionFactory");
        String url3 = (String) TestUtil.getFieldValue(factory3, "url");
        Properties properties3 = (Properties) TestUtil.getFieldValue(factory3, "properties");
        String user3 = properties3.getProperty("user");
        String password3 = properties3.getProperty("password");

        Assert.assertTrue(url3.endsWith("-Decoded"));
        Assert.assertTrue(user3.endsWith("-Decoded"));
        Assert.assertNull(password3);
    }

    public void testJdbcDecoderOnFactory() throws Exception {
        clearBeeCPInfoFromSystemProperties();
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.setConnectionFactoryClass(MockCommonConnectionFactory.class);
        config1.setJdbcLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig1 = config1.check();
        MockCommonConnectionFactory factory1 = (MockCommonConnectionFactory) checkedConfig1.getConnectionFactory();
        Assert.assertNull(factory1.getUser());
        Assert.assertNull(factory1.getPassword());

        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setUsername(username);
        config2.setConnectionFactoryClass(MockCommonConnectionFactory.class);
        config2.setJdbcLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig2 = config2.check();
        MockCommonConnectionFactory factory2 = (MockCommonConnectionFactory) checkedConfig2.getConnectionFactory();
        Assert.assertTrue(factory2.getUser().endsWith("-Decoded"));
        Assert.assertNull(factory2.getPassword());

        BeeDataSourceConfig config3 = new BeeDataSourceConfig();
        config3.setUrl(url);
        config3.setUsername(username);
        config3.setPassword(password);
        config3.setConnectionFactoryClass(MockCommonConnectionFactory.class);
        config3.setJdbcLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig3 = config3.check();
        MockCommonConnectionFactory factory3 = (MockCommonConnectionFactory) checkedConfig3.getConnectionFactory();
        Assert.assertTrue(factory3.getUrl().endsWith("-Decoded"));
        Assert.assertTrue(factory3.getUser().endsWith("-Decoded"));
        Assert.assertTrue(factory3.getPassword().endsWith("-Decoded"));

        BeeDataSourceConfig config4 = new BeeDataSourceConfig();
        config4.addConnectProperty("url", url);
        config4.addConnectProperty("user", username);
        config4.addConnectProperty("password", password);
        config4.setConnectionFactoryClass(MockCommonConnectionFactory.class);
        config4.setJdbcLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig4 = config4.check();
        MockCommonConnectionFactory factory4 = (MockCommonConnectionFactory) checkedConfig4.getConnectionFactory();
        Assert.assertTrue(factory4.getUrl().endsWith("-Decoded"));
        Assert.assertTrue(factory4.getUser().endsWith("-Decoded"));
        Assert.assertTrue(factory4.getPassword().endsWith("-Decoded"));

        BeeDataSourceConfig config5 = new BeeDataSourceConfig();
        clearBeeCPInfoFromSystemProperties();
        System.setProperty("beecp.url", url);
        System.setProperty("beecp.user", username);
        System.setProperty("beecp.password", password);
        config5.setConnectionFactoryClass(MockCommonConnectionFactory.class);
        config5.setJdbcLinkInfoDecoderClass(SampleMockJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig5 = config5.check();
        MockCommonConnectionFactory factory5 = (MockCommonConnectionFactory) checkedConfig5.getConnectionFactory();
        Assert.assertTrue(factory5.getUrl().endsWith("-Decoded"));
        Assert.assertTrue(factory5.getUser().endsWith("-Decoded"));
        Assert.assertTrue(factory5.getPassword().endsWith("-Decoded"));
    }
}
