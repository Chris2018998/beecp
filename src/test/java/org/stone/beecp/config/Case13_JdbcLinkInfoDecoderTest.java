package org.stone.beecp.config;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.config.customization.DummyJdbcLinkInfoDecoder;
import org.stone.beecp.factory.NullConnectionFactory;

import java.util.Properties;

import static org.stone.beecp.config.ConfigFactory.clearBeeCPInfoFromSystemProperties;

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
        clearBeeCPInfoFromSystemProperties();
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.setConnectionFactoryClass(NullConnectionFactory.class);
        config1.setJdbcLinkInfoDecoderClass(DummyJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig1 = config1.check();
        NullConnectionFactory factory1 = (NullConnectionFactory) checkedConfig1.getConnectionFactory();
        Assert.assertNull(factory1.getUser());
        Assert.assertNull(factory1.getPassword());

        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setUsername(username);
        config2.setConnectionFactoryClass(NullConnectionFactory.class);
        config2.setJdbcLinkInfoDecoderClass(DummyJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig2 = config2.check();
        NullConnectionFactory factory2 = (NullConnectionFactory) checkedConfig2.getConnectionFactory();
        Assert.assertTrue(factory2.getUser().endsWith("-Decoded"));
        Assert.assertNull(factory2.getPassword());

        BeeDataSourceConfig config3 = new BeeDataSourceConfig();
        config3.setUrl(this.url);
        config3.setUsername(this.username);
        config3.setPassword(this.password);
        config3.setConnectionFactoryClass(NullConnectionFactory.class);
        config3.setJdbcLinkInfoDecoderClass(DummyJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig3 = config3.check();
        NullConnectionFactory factory3 = (NullConnectionFactory) checkedConfig3.getConnectionFactory();
        Assert.assertTrue(factory3.getUrl().endsWith("-Decoded"));
        Assert.assertTrue(factory3.getUser().endsWith("-Decoded"));
        Assert.assertTrue(factory3.getPassword().endsWith("-Decoded"));

        BeeDataSourceConfig config4 = new BeeDataSourceConfig();
        config4.addConnectProperty("url", url);
        config4.addConnectProperty("user", this.username);
        config4.addConnectProperty("password", this.password);
        config4.setConnectionFactoryClass(NullConnectionFactory.class);
        config4.setJdbcLinkInfoDecoderClass(DummyJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig4 = config4.check();
        NullConnectionFactory factory4 = (NullConnectionFactory) checkedConfig4.getConnectionFactory();
        Assert.assertTrue(factory4.getUrl().endsWith("-Decoded"));
        Assert.assertTrue(factory4.getUser().endsWith("-Decoded"));
        Assert.assertTrue(factory4.getPassword().endsWith("-Decoded"));

        BeeDataSourceConfig config5 = new BeeDataSourceConfig();
        clearBeeCPInfoFromSystemProperties();
        System.setProperty("beecp.url", url);
        System.setProperty("beecp.user", this.username);
        System.setProperty("beecp.password", this.password);
        config5.setConnectionFactoryClass(NullConnectionFactory.class);
        config5.setJdbcLinkInfoDecoderClass(DummyJdbcLinkInfoDecoder.class);
        BeeDataSourceConfig checkedConfig5 = config5.check();
        NullConnectionFactory factory5 = (NullConnectionFactory) checkedConfig5.getConnectionFactory();
        Assert.assertTrue(factory5.getUrl().endsWith("-Decoded"));
        Assert.assertTrue(factory5.getUser().endsWith("-Decoded"));
        Assert.assertTrue(factory5.getPassword().endsWith("-Decoded"));
    }
}
