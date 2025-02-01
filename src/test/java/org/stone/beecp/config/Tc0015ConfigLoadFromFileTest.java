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
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static org.stone.base.TestUtil.getClassPathFileAbsolutePath;
import static org.stone.beecp.config.DsConfigFactory.createEmpty;
import static org.stone.tools.CommonUtil.loadPropertiesFromClassPathFile;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class Tc0015ConfigLoadFromFileTest extends TestCase {
    private static final String filename = "beecp/config2.properties";

    /********************************************Constructor**************************************************/

    public void testOnCorrectFile() throws Exception {
        String classFilename = "cp:" + filename;
        Assert.assertTrue(check(new BeeDataSourceConfig(classFilename)));//classpath
        Assert.assertTrue(check(new BeeDataSourceConfig(getClassPathFileAbsolutePath(filename))));//from file
        Assert.assertTrue(check(new BeeDataSourceConfig(loadPropertiesFromClassPathFile(filename))));//from properties

        BeeDataSourceConfig config1 = createEmpty();
        config1.loadFromPropertiesFile(classFilename);
        Assert.assertTrue(check(config1));

        BeeDataSourceConfig config2 = createEmpty();
        config2.loadFromPropertiesFile(getClassPathFileAbsolutePath(filename));
        Assert.assertTrue(check(config2));

        BeeDataSourceConfig config3 = createEmpty();
        config3.loadFromProperties(loadPropertiesFromClassPathFile(filename));
        Assert.assertTrue(check(config3));
    }


    public void testOnLoadByFileName() {
        BeeDataSourceConfig config1 = createEmpty();
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
            Assert.assertTrue(message != null && message.contains("Configuration file name file must be end with '.properties'"));
        }

        try {
            config1.loadFromPropertiesFile("D:\\beecp\\ds.properties");//file not found
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Not found configuration file"));
        }

        try {//failure test
            String fullFilename = Objects.requireNonNull(getClassPathFileAbsolutePath(filename)).toString();
            String osFileName2 = "beecp/invalid.properties".replace("/", File.separator);
            int lasIndex = fullFilename.lastIndexOf(filename.replace("/", File.separator));
            String invalidFilePath = fullFilename.substring(0, lasIndex) + osFileName2;
            config1.loadFromPropertiesFile(invalidFilePath);//folder test
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Target object is a valid configuration file"));
        }

        try {//success test
            File path = getClassPathFileAbsolutePath(filename);
            assert path != null;
            config1.loadFromPropertiesFile(path.toString());
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Configuration properties can't be null or empty"));
        }

        try {//load from classpath
            config1.loadFromPropertiesFile("cp:" + filename);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Configuration properties can't be null or empty"));
        }

        try {//load from classpath
            config1.loadFromPropertiesFile("classpath:" + filename);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Configuration properties can't be null or empty"));
        }
    }

    public void testOnLoadProperties() {
        BeeDataSourceConfig config1 = createEmpty();
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

        try {
            Properties properties = new Properties();
            properties.put("maxActive", "oooo");
            config1.loadFromProperties(properties);
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Failed to convert value[oooo]to property type(maxActive:int)"));
        }
    }

    public void testOnLoadByFile() {
        BeeDataSourceConfig config1 = createEmpty();

        try {//null file test
            config1.loadFromPropertiesFile((File) null);
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
            String os = System.getProperty("os.name").toLowerCase();
            File configFile = os.contains("windows") ? new File("C:\\") : new File("//");
            config1.loadFromPropertiesFile(configFile);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("is not a valid file"));
        }

        try {//valid file
            Class<?> selfClass = Tc0015ConfigLoadFromFileTest.class;
            URL resource = selfClass.getClassLoader().getResource(filename);
            Assert.assertNotNull(resource);

            String path = resource.getPath();
            String filePath = path.substring(0, path.indexOf("config2.properties")) + "invalid.properties1";
            File configFile = new File(filePath);
            config1.loadFromPropertiesFile(configFile);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("not a properties file"));
        }
    }

    private Boolean check(BeeDataSourceConfig config) {
        final String ConfigUrl = "jdbc:beecp://localhost/testdb";
        final String ConfigDriver = "org.stone.beecp.mock.MockDriver";

        Assert.assertEquals("root", config.getUsername());
        Assert.assertEquals("root", config.getPassword());
        Assert.assertEquals(ConfigUrl, config.getJdbcUrl());
        Assert.assertEquals(ConfigDriver, config.getDriverClassName());
        Assert.assertEquals("test1", config.getDefaultCatalog());
        Assert.assertEquals("test2", config.getDefaultSchema());
        Assert.assertTrue(config.isDefaultReadOnly());
        Assert.assertTrue(config.isDefaultAutoCommit());
        Assert.assertEquals(config.getDefaultTransactionIsolationCode(), Integer.valueOf(1));
        Assert.assertEquals("READ_UNCOMMITTED", config.getDefaultTransactionIsolationName());
        Assert.assertEquals("SELECT 1", config.getAliveTestSql());
        Assert.assertEquals("Pool1", config.getPoolName());
        Assert.assertTrue(config.isFairMode());
        Assert.assertEquals(1, config.getInitialSize());
        Assert.assertEquals(10, config.getMaxActive());
        Assert.assertEquals(8000L, config.getMaxWait());
        Assert.assertEquals(18000L, config.getIdleTimeout());
        Assert.assertEquals(30000L, config.getHoldTimeout());
        Assert.assertEquals(3, config.getAliveTestTimeout());
        Assert.assertEquals(500, config.getAliveAssumeTime());
        Assert.assertEquals(30000, config.getTimerCheckInterval());
        Assert.assertTrue(config.isForceRecycleBorrowedOnClose());
        Assert.assertEquals(3000, config.getParkTimeForRetry());
        Assert.assertEquals("com.myProject.TestPredication", config.getEvictPredicateClassName());

        List<Integer> sqlExceptionCodeList = config.getSqlExceptionCodeList();
        List<String> sqlExceptionStateList = config.getSqlExceptionStateList();
        for (Integer code : sqlExceptionCodeList)
            Assert.assertTrue(code == 500150 || code == 2399);
        for (String state : sqlExceptionStateList)
            Assert.assertTrue("0A000".equals(state) || "57P01".equals(state));

        Assert.assertEquals("org.stone.beecp.pool.ConnectionFactoryByDriver", config.getConnectionFactoryClassName());
        Assert.assertEquals("org.stone.beecp.pool.RawConnectionPool", config.getPoolImplementClassName());
        Assert.assertTrue(config.isEnableJmx());

        Assert.assertEquals("true", config.getConnectProperty("cachePrepStmts"));
        Assert.assertEquals("50", config.getConnectProperty("prepStmtCacheSize"));
        Assert.assertEquals("2048", config.getConnectProperty("prepStmtCacheSqlLimit"));
        Assert.assertEquals("true", config.getConnectProperty("useServerPrepStmts"));
        return true;
    }
}
