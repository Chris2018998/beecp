package org.stone.beecp.config;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.RawXaConnectionFactory;
import org.stone.beecp.config.customization.DummyJdbcLinkInfoDecoder;
import org.stone.beecp.factory.NullConnectionFactory;
import org.stone.beecp.mock.MockXaDataSource;
import org.stone.beecp.pool.FastConnectionPool;

import java.util.Properties;

public class Case13_JdbcLinkInfoDecoderTest extends TestCase {
    private final String driver = "org.stone.beecp.mock.MockDriver";
    private final String url = "jdbc:beecp://localhost/testdb";
    private final String username = "user";
    private final String password = "passwor";

    public void testOnSetGet() {
        BeeDataSourceConfig config = ConfigFactory.createEmpty();
        Class decodeClass = DummyJdbcLinkInfoDecoder.class;
        config.setJdbcLinkInfoDecoderClass(decodeClass);
        Assert.assertEquals(config.getJdbcLinkInfoDecoderClass(), decodeClass);

        String decodeClassName = "org.stone.beecp.config.customization.DummyJdbcLinkInfoDecoder";
        config.setJdbcLinkInfoDecoderClassName(decodeClassName);
        Assert.assertEquals(config.getJdbcLinkInfoDecoderClassName(), decodeClassName);

        DummyJdbcLinkInfoDecoder decoder = new DummyJdbcLinkInfoDecoder();
        config.setJdbcLinkInfoDecoder(decoder);
        Assert.assertEquals(config.getJdbcLinkInfoDecoder(), decoder);
    }

    public void testOnCreation() throws Exception {
        BeeDataSourceConfig config1 = new BeeDataSourceConfig(driver, url, username, password);
        config1.setJdbcLinkInfoDecoder(new DummyJdbcLinkInfoDecoder());
        config1.setConnectionFactoryClass(NullConnectionFactory.class);
        BeeDataSourceConfig checkConfig = config1.check();

        NullConnectionFactory factory = (NullConnectionFactory) checkConfig.getConnectionFactory();
        String url = factory.getUrl();
        String user = factory.getUser();
        String password = factory.getPassword();
        Assert.assertTrue(url.endsWith("-Decoded"));
        Assert.assertTrue(user.endsWith("-Decoded"));
        Assert.assertTrue(password.endsWith("-Decoded"));

        BeeDataSourceConfig config2 = new BeeDataSourceConfig(driver, url, username, password);
        config2.setConnectionFactoryClass(NullConnectionFactory.class);
        config2.setJdbcLinkInfoDecoderClass(org.stone.beecp.config.customization.DummyJdbcLinkInfoDecoder.class);
        checkConfig = config2.check();
        factory = (NullConnectionFactory) checkConfig.getConnectionFactory();

        url = factory.getUrl();
        user = factory.getUser();
        password = factory.getPassword();
        Assert.assertTrue(url.endsWith("-Decoded"));
        Assert.assertTrue(user.endsWith("-Decoded"));
        Assert.assertTrue(password.endsWith("-Decoded"));

        BeeDataSourceConfig config3 = new BeeDataSourceConfig(driver, url, username, password);
        config3.setConnectionFactoryClass(NullConnectionFactory.class);
        config3.setJdbcLinkInfoDecoderClassName("org.stone.beecp.config.customization.DummyJdbcLinkInfoDecoder");
        checkConfig = config3.check();
        factory = (NullConnectionFactory) checkConfig.getConnectionFactory();
        url = factory.getUrl();
        user = factory.getUser();
        password = factory.getPassword();
        Assert.assertTrue(url.endsWith("-Decoded"));
        Assert.assertTrue(user.endsWith("-Decoded"));
        Assert.assertTrue(password.endsWith("-Decoded"));
    }

    public void testOnErrorClass() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig(driver, url, username, password);
        config.setJdbcLinkInfoDecoderClass(String.class);//error config
        try {
            config.check();
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
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
        config1.setJdbcLinkInfoDecoderClass(DummyJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig1 = config1.check();
        Object factory1 = TestUtil.getFieldValue(checkedConfig1, "connectionFactory");
        Properties properties1 = (Properties) TestUtil.getFieldValue(factory1, "properties");
        Assert.assertFalse(properties1.contains("user"));
        Assert.assertFalse(properties1.contains("password"));


        BeeDataSourceConfig config2 = new BeeDataSourceConfig(driver, url, username, password);
        config2.setJdbcLinkInfoDecoderClass(DummyJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig2 = config2.check();
        Object factory2 = TestUtil.getFieldValue(checkedConfig2, "connectionFactory");
        String url2 = (String) TestUtil.getFieldValue(factory2, "url");
        Properties properties = (Properties) TestUtil.getFieldValue(factory2, "properties");
        String user2 = properties.getProperty("user");
        String password2 = properties.getProperty("password");
        Assert.assertTrue(url2.endsWith("-Decoded"));
        Assert.assertTrue(user2.endsWith("-Decoded"));
        Assert.assertTrue(password2.endsWith("-Decoded"));
    }

    public void testJdbcDecoderOnFactory() throws Exception {
        BeeDataSourceConfig config1 = new BeeDataSourceConfig(driver, url, null, null);
        config1.setConnectionFactoryClass(NullConnectionFactory.class);
        config1.setJdbcLinkInfoDecoderClass(DummyJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig1 = config1.check();
        NullConnectionFactory factory1 = (NullConnectionFactory) checkedConfig1.getConnectionFactory();
        Assert.assertNull(factory1.getUser());
        Assert.assertNull(factory1.getPassword());

        BeeDataSourceConfig config2 = new BeeDataSourceConfig(driver, url, username, password);
        config2.setConnectionFactoryClass(NullConnectionFactory.class);
        config2.setJdbcLinkInfoDecoderClass(DummyJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig2 = config2.check();
        NullConnectionFactory factory2 = (NullConnectionFactory) checkedConfig2.getConnectionFactory();
        Assert.assertTrue(factory2.getUrl().endsWith("-Decoded"));
        Assert.assertTrue(factory2.getUser().endsWith("-Decoded"));
        Assert.assertTrue(factory2.getPassword().endsWith("-Decoded"));
    }

    public void testJdbcDecoderOnXaDataSource() throws Exception {
        BeeDataSourceConfig config1 = new BeeDataSourceConfig(driver, null, null, null);
        config1.setConnectionFactoryClassName("org.stone.beecp.mock.MockXaDataSource");
        config1.setJdbcLinkInfoDecoderClassName("org.stone.beecp.config.customization.DummyJdbcLinkInfoDecoder");
        BeeDataSource ds = new BeeDataSource(config1);
        FastConnectionPool pool1 = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        RawXaConnectionFactory rawXaConnFactory1 = (RawXaConnectionFactory) TestUtil.getFieldValue(pool1, "rawXaConnFactory");
        MockXaDataSource xaDs1 = (MockXaDataSource) TestUtil.getFieldValue(rawXaConnFactory1, "dataSource");
        Assert.assertNull(xaDs1.getUser());
        Assert.assertNull(xaDs1.getURL());
        Assert.assertNull(xaDs1.getPassword());

        BeeDataSourceConfig config2 = new BeeDataSourceConfig(driver, url, username, password);
        config2.setConnectionFactoryClassName("org.stone.beecp.mock.MockXaDataSource");
        config2.setJdbcLinkInfoDecoderClassName("org.stone.beecp.config.customization.DummyJdbcLinkInfoDecoder");
        BeeDataSource ds2 = new BeeDataSource(config2);
        FastConnectionPool pool2 = (FastConnectionPool) TestUtil.getFieldValue(ds2, "pool");
        RawXaConnectionFactory rawXaConnFactory2 = (RawXaConnectionFactory) TestUtil.getFieldValue(pool2, "rawXaConnFactory");
        MockXaDataSource xaDs2 = (MockXaDataSource) TestUtil.getFieldValue(rawXaConnFactory2, "dataSource");
        Assert.assertTrue(xaDs2.getURL().endsWith("-Decoded"));
        Assert.assertTrue(xaDs2.getUser().endsWith("-Decoded"));
        Assert.assertTrue(xaDs2.getPassword().endsWith("-Decoded"));
    }

    public void testJdbcDecoderOnXaDataSource2() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createEmpty();
        config.setConnectionFactoryClassName("org.stone.beecp.mock.MockXaDataSource");
        config.setJdbcLinkInfoDecoderClassName("org.stone.beecp.config.customization.DummyJdbcLinkInfoDecoder");
        config.addConnectProperty("URL", "jdbc:mock:test");
        config.addConnectProperty("user", "mock");
        config.addConnectProperty("password", "root");
        BeeDataSource ds = new BeeDataSource(config);

        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        RawXaConnectionFactory rawXaConnFactory = (RawXaConnectionFactory) TestUtil.getFieldValue(pool, "rawXaConnFactory");
        MockXaDataSource xaDs = (MockXaDataSource) TestUtil.getFieldValue(rawXaConnFactory, "dataSource");

        Assert.assertTrue(xaDs.getURL().endsWith("-Decoded"));
        Assert.assertTrue(xaDs.getUser().endsWith("-Decoded"));
        Assert.assertTrue(xaDs.getPassword().endsWith("-Decoded"));
    }
}
