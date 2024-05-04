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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class Case16_ConfigLoadFromFileTest extends TestCase {
    private final String filename = "beecp/config2.properties";

    /********************************************Constructor**************************************************/

    public void testOnCorrectFile() throws Exception {
        check(new BeeDataSourceConfig(filename));
        check(new BeeDataSourceConfig(getPropertiesFile()));
        check(new BeeDataSourceConfig(getFileProperties()));

        BeeDataSourceConfig config1 = ConfigFactory.createEmpty();
        config1.loadFromPropertiesFile(filename);
        check(config1);

        BeeDataSourceConfig config2 = ConfigFactory.createEmpty();
        config2.loadFromPropertiesFile(getPropertiesFile());
        check(config2);

        BeeDataSourceConfig config3 = ConfigFactory.createEmpty();
        config3.loadFromProperties(getFileProperties());
        check(config3);
    }


    public void testOnLoadByFileName() throws Exception {
        BeeDataSourceConfig config1 = ConfigFactory.createEmpty();
        try {
            config1.loadFromPropertiesFile("");
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("file name can't be null or empty"));
        }

        try {
            config1.loadFromPropertiesFile("D:\\beecp\\ds.properties1");
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("must end with '.properties'"));
        }

        try {
            config1.loadFromPropertiesFile("D:\\beecp\\ds.properties");
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Not found file"));
        }


//        try {
//            BeeDataSourceConfig config1 = ConfigFactory.createEmpty();
//            config1.loadFromPropertiesFile("ds.properties");
//        } catch (Exception e) {
//            String message = e.getMessage();
//            Assert.assertTrue(message != null && message.contains("Not found file"));
//        }

    }

    public void testOnLoadProperties() throws Exception {
        BeeDataSourceConfig config1 = ConfigFactory.createEmpty();
        try {
            config1.loadFromProperties(null);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Configuration properties can't be null or empty"));
        }

        try {
            config1.loadFromProperties(new Properties());
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Configuration properties can't be null or empty"));
        }
    }

    public void testOnLoadByFile() {
        BeeDataSourceConfig config1 = ConfigFactory.createEmpty();

        try {//null file test
            File configFile = null;
            config1.loadFromPropertiesFile(configFile);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("can't be null"));
        }

        try {//existence test
            File configFile = new File("c:\\beecp\\ds.properties");
            config1.loadFromPropertiesFile(configFile);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("file not found"));
        }

        try {//existence test
            File configFile = new File("c:\\beecp\\ds");
            config1.loadFromPropertiesFile(configFile);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("file not found"));
        }

        try {//valid file
            File configFile = new File("C:\\");
            config1.loadFromPropertiesFile(configFile);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("is not a valid file"));
        }

        try {//valid file
            Class selfClass = Case16_ConfigLoadFromFileTest.class;
            URL resource = selfClass.getClassLoader().getResource(filename);
            String path = resource.getPath();
            String filePath = path.substring(0, path.indexOf("config2.properties")) + "invalid.properties1";
            File configFile = new File(filePath);
            config1.loadFromPropertiesFile(configFile);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("not a properties file"));
        }


    }

    private Properties getFileProperties() throws Exception {
        Class selfClass = Case16_ConfigLoadFromFileTest.class;
        InputStream propertiesStream = selfClass.getResourceAsStream(filename);
        if (propertiesStream == null) propertiesStream = selfClass.getClassLoader().getResourceAsStream(filename);

        Properties prop = new Properties();
        prop.load(propertiesStream);
        return prop;
    }

    private File getPropertiesFile() throws Exception {
        Class selfClass = Case16_ConfigLoadFromFileTest.class;
        URL fileUrl = selfClass.getResource(filename);
        if (fileUrl == null) fileUrl = selfClass.getClassLoader().getResource(filename);
        return new File(fileUrl.toURI());
    }

    private void check(BeeDataSourceConfig config) throws Exception {
        final String ConfigUrl = "jdbc:beecp://localhost/testdb";
        final String ConfigDriver = "org.stone.beecp.mock.MockDriver";

        Assert.assertEquals(config.getUsername(), "root");
        Assert.assertEquals(config.getPassword(), "root");
        Assert.assertEquals(config.getJdbcUrl(), ConfigUrl);
        Assert.assertEquals(config.getDriverClassName(), ConfigDriver);
        Assert.assertEquals(config.getDefaultCatalog(), "test1");
        Assert.assertEquals(config.getDefaultSchema(), "test2");
        Assert.assertTrue(config.isDefaultReadOnly());
        Assert.assertTrue(config.isDefaultAutoCommit());
        Assert.assertEquals(config.getDefaultTransactionIsolationCode(), new Integer(1));
        Assert.assertEquals(config.getDefaultTransactionIsolationName(), "READ_UNCOMMITTED");
        Assert.assertEquals(config.getAliveTestSql(), "SELECT 1");
        Assert.assertEquals(config.getPoolName(), "Pool1");
        Assert.assertTrue(config.isFairMode());
        Assert.assertEquals(config.getInitialSize(), 1);
        Assert.assertEquals(config.getMaxActive(), 10);
        Assert.assertEquals(config.getMaxWait(), 8000L);
        Assert.assertEquals(config.getIdleTimeout(), 18000L);
        Assert.assertEquals(config.getHoldTimeout(), 30000L);
        Assert.assertEquals(config.getAliveTestTimeout(), 3);
        Assert.assertEquals(config.getAliveAssumeTime(), 500);
        Assert.assertEquals(config.getTimerCheckInterval(), 30000);
        Assert.assertTrue(config.isForceCloseUsingOnClear());
        Assert.assertEquals(config.getDelayTimeForNextClear(), 3000);
        Assert.assertEquals(config.getEvictPredicateClassName(), "com.myProject.TestPredication");

        List<Integer> sqlExceptionCodeList = (List<Integer>) TestUtil.getFieldValue(config, "sqlExceptionCodeList");
        List<String> sqlExceptionStateList = (List<String>) TestUtil.getFieldValue(config, "sqlExceptionStateList");
        for (Integer code : sqlExceptionCodeList)
            Assert.assertTrue(code == 500150 || code == 2399);
        for (String state : sqlExceptionStateList)
            Assert.assertTrue("0A000".equals(state) || "57P01".equals(state));

        Assert.assertEquals(config.getConnectionFactoryClassName(), "org.stone.beecp.pool.ConnectionFactoryByDriver");
        Assert.assertEquals(config.getPoolImplementClassName(), "org.stone.beecp.pool.RawConnectionPool");
        Assert.assertTrue(config.isEnableJmx());

        Assert.assertEquals(config.getConnectProperty("cachePrepStmts"), "true");
        Assert.assertEquals(config.getConnectProperty("prepStmtCacheSize"), "50");
        Assert.assertEquals(config.getConnectProperty("prepStmtCacheSqlLimit"), "2048");
        Assert.assertEquals(config.getConnectProperty("useServerPrepStmts"), "true");
    }
}
