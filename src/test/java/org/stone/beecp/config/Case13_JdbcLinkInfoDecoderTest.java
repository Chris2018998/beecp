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
        BeeDataSourceConfig config = new BeeDataSourceConfig();
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
        BeeDataSourceConfig config = new BeeDataSourceConfig(driver, url, username, password);
        config.setJdbcLinkInfoDecoderClass(DummyJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig = config.check();
        Object factory = TestUtil.getFieldValue(checkedConfig, "connectionFactory");
        String url = (String) TestUtil.getFieldValue(factory, "url");
        Properties properties = (Properties) TestUtil.getFieldValue(factory, "properties");
        String user = properties.getProperty("user");
        String password = properties.getProperty("password");

        Assert.assertTrue(url.endsWith("-Decoded"));
        Assert.assertTrue(user.endsWith("-Decoded"));
        Assert.assertTrue(password.endsWith("-Decoded"));
    }

    public void testJdbcDecoderOnFactory() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig(driver, url, username, password);
        config.setConnectionFactoryClass(NullConnectionFactory.class);
        config.setJdbcLinkInfoDecoderClass(DummyJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig = config.check();

        NullConnectionFactory factory = (NullConnectionFactory) checkedConfig.getConnectionFactory();

        String url = factory.getUrl();
        String user = factory.getUser();
        String password = factory.getPassword();
        Assert.assertTrue(url.endsWith("-Decoded"));
        Assert.assertTrue(user.endsWith("-Decoded"));
        Assert.assertTrue(password.endsWith("-Decoded"));
    }

    public void testJdbcDecoderOnXaDataSource() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig(driver, url, username, password);
        config.setConnectionFactoryClassName("org.stone.beecp.mock.MockXaDataSource");
        config.setJdbcLinkInfoDecoderClassName("org.stone.beecp.config.customization.DummyJdbcLinkInfoDecoder");
        BeeDataSource ds = new BeeDataSource(config);

        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        RawXaConnectionFactory rawXaConnFactory = (RawXaConnectionFactory) TestUtil.getFieldValue(pool, "rawXaConnFactory");
        MockXaDataSource xaDs = (MockXaDataSource) TestUtil.getFieldValue(rawXaConnFactory, "dataSource");

        String url = xaDs.getURL();
        String user = xaDs.getUser();
        String password = xaDs.getPassword();
        Assert.assertTrue(url.endsWith("-Decoded"));
        Assert.assertTrue(user.endsWith("-Decoded"));
        Assert.assertTrue(password.endsWith("-Decoded"));
    }

    public void testJdbcDecoderOnXaDataSource2() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactoryClassName("org.stone.beecp.mock.MockXaDataSource");
        config.setJdbcLinkInfoDecoderClassName("org.stone.beecp.config.customization.DummyJdbcLinkInfoDecoder");
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
        Assert.assertTrue(url.endsWith("-Decoded"));
        Assert.assertTrue(user.endsWith("-Decoded"));
        Assert.assertTrue(password.endsWith("-Decoded"));
    }
}
