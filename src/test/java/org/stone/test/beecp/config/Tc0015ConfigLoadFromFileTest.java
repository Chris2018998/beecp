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

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.base.TestUtil.getClassPathFileAbsolutePath;
import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;
import static org.stone.tools.CommonUtil.loadPropertiesFromClassPathFile;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class Tc0015ConfigLoadFromFileTest {
    private static final String filename = "file/beecp/config2.properties";

    /********************************************Constructor**************************************************/
    @Test
    public void testOnCorrectFile() throws Exception {
        String classFilename = "cp:" + filename;
        Assertions.assertTrue(check(new BeeDataSourceConfig(classFilename)));//classpath
        Assertions.assertTrue(check(new BeeDataSourceConfig(getClassPathFileAbsolutePath(filename))));//from file
        Assertions.assertTrue(check(new BeeDataSourceConfig(loadPropertiesFromClassPathFile(filename))));//from properties

        BeeDataSourceConfig config1 = createEmpty();
        config1.loadFromPropertiesFile(classFilename);
        Assertions.assertTrue(check(config1));

        BeeDataSourceConfig config2 = createEmpty();
        config2.loadFromPropertiesFile(getClassPathFileAbsolutePath(filename));
        Assertions.assertTrue(check(config2));

        BeeDataSourceConfig config3 = createEmpty();
        config3.loadFromProperties(loadPropertiesFromClassPathFile(filename));
        Assertions.assertTrue(check(config3));
    }

    @Test
    public void testOnLoadByFileName() {
        BeeDataSourceConfig config1 = createEmpty();
        try {
            config1.loadFromPropertiesFile("");
            fail("[testOnLoadByFileName]not threw exception when load blank filename");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("file name can't be null or empty"));
        }


        try {
            config1.loadFromPropertiesFile("D:\\beecp\\ds.properties1");
            fail("[testOnLoadByFileName]not threw exception when properties file not existed");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Configuration file name file must be end with '.properties'"));
        }

        try {
            config1.loadFromPropertiesFile("D:\\beecp\\ds.properties");//file not found
            fail("[testOnLoadByFileName]not threw exception when properties file not existed");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Not found configuration file"));
        }

        try {//failure test
            String fullFilename = Objects.requireNonNull(getClassPathFileAbsolutePath(filename)).toString();
            String osFileName2 = "/file/beecp/invalid.properties".replace("/", File.separator);
            int lasIndex = fullFilename.lastIndexOf(filename.replace("/", File.separator));
            String invalidFilePath = fullFilename.substring(0, lasIndex) + osFileName2;
            config1.loadFromPropertiesFile(invalidFilePath);//folder test
            fail("[testOnLoadByFileName]not threw exception when properties file object is folder");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Target object is a valid configuration file"));
        }

        try {//success test
            File path = getClassPathFileAbsolutePath(filename);
            assert path != null;
            config1.loadFromPropertiesFile(path.toString());
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Configuration properties can't be null or empty"));
        }

        try {//success test:load from classpath
            config1.loadFromPropertiesFile("cp:" + filename);
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Configuration properties can't be null or empty"));
        }

        try {//success test: load from classpath
            config1.loadFromPropertiesFile("classpath:" + filename);
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Configuration properties can't be null or empty"));
        }
    }

    @Test
    public void testOnLoadProperties() {
        BeeDataSourceConfig config1 = createEmpty();
        try {
            config1.loadFromProperties(null);
            fail("[testOnLoadProperties]not threw exception when loading null properties file");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Configuration properties can't be null or empty"));
        }

        try {//correct
            config1.loadFromProperties(new Properties());
            fail("[testOnLoadProperties]not threw exception when loading empty properties");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Configuration properties can't be null or empty"));
        }

        try {//correct
            Properties properties = new Properties();
            properties.put("maxActive", "oooo");
            config1.loadFromProperties(properties);
            fail("[testOnLoadProperties]not threw exception when loading invalid properties item");
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Failed to convert value[oooo]to property type(maxActive:int)"));
        }
    }

    @Test
    public void testOnLoadByFile() {
        BeeDataSourceConfig config1 = createEmpty();

        try {//null file test
            config1.loadFromPropertiesFile((File) null);
            fail("[testOnLoadByFile]not threw exception when loading null properties file");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("can't be null"));
        }

        try {//existence test
            File configFile = new File("c:\\beecp\\ds.properties");
            config1.loadFromPropertiesFile(configFile);
            fail("[testOnLoadByFile]not threw exception when loading null properties file");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("file not found"));
        }

        try {//existence test
            File configFile = new File("c:\\beecp\\ds");
            config1.loadFromPropertiesFile(configFile);
            fail("[testOnLoadByFile]not threw exception when load invalid file");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("file not found"));
        }

        try {//valid file
            String os = System.getProperty("os.name").toLowerCase();
            File configFile = os.contains("windows") ? new File("C:\\") : new File("//");
            config1.loadFromPropertiesFile(configFile);
            fail("[testOnLoadByFile]not threw exception when loading null properties file");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("is not a valid file"));
        }

        try {//valid file
            Class<?> selfClass = Tc0015ConfigLoadFromFileTest.class;
            URL resource = selfClass.getClassLoader().getResource(filename);
            Assertions.assertNotNull(resource);

            String path = resource.getPath();
            String filePath = path.substring(0, path.indexOf("config2.properties")) + "invalid.properties1";
            File configFile = new File(filePath);
            config1.loadFromPropertiesFile(configFile);
            fail("testOnLoadByFile");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("not a properties file"));
        }
    }

    private Boolean check(BeeDataSourceConfig config) {
        final String ConfigUrl = "jdbc:beecp://localhost/testdb";
        final String ConfigDriver = "org.stone.beecp.mock.MockDriver";

        Assertions.assertEquals("root", config.getUsername());
        Assertions.assertEquals("root", config.getPassword());
        Assertions.assertEquals(ConfigUrl, config.getJdbcUrl());
        Assertions.assertEquals(ConfigDriver, config.getDriverClassName());
        Assertions.assertEquals("test1", config.getDefaultCatalog());
        Assertions.assertEquals("test2", config.getDefaultSchema());
        Assertions.assertTrue(config.isDefaultReadOnly());
        Assertions.assertTrue(config.isDefaultAutoCommit());
        Assertions.assertEquals(config.getDefaultTransactionIsolationCode(), Integer.valueOf(1));
        Assertions.assertEquals("READ_UNCOMMITTED", config.getDefaultTransactionIsolationName());
        Assertions.assertEquals("SELECT 1", config.getAliveTestSql());
        Assertions.assertEquals("Pool1", config.getPoolName());
        Assertions.assertTrue(config.isFairMode());
        Assertions.assertEquals(1, config.getInitialSize());
        Assertions.assertEquals(10, config.getMaxActive());
        Assertions.assertEquals(8000L, config.getMaxWait());
        Assertions.assertEquals(18000L, config.getIdleTimeout());
        Assertions.assertEquals(30000L, config.getHoldTimeout());
        Assertions.assertEquals(3, config.getAliveTestTimeout());
        Assertions.assertEquals(500, config.getAliveAssumeTime());
        Assertions.assertEquals(30000, config.getTimerCheckInterval());
        Assertions.assertTrue(config.isForceRecycleBorrowedOnClose());
        Assertions.assertEquals(3000, config.getParkTimeForRetry());
        Assertions.assertEquals("com.myProject.TestPredication", config.getEvictPredicateClassName());

        List<Integer> sqlExceptionCodeList = config.getSqlExceptionCodeList();
        List<String> sqlExceptionStateList = config.getSqlExceptionStateList();
        for (Integer code : sqlExceptionCodeList)
            Assertions.assertTrue(code == 500150 || code == 2399);
        for (String state : sqlExceptionStateList)
            Assertions.assertTrue("0A000".equals(state) || "57P01".equals(state));

        Assertions.assertEquals("org.stone.beecp.pool.ConnectionFactoryByDriver", config.getConnectionFactoryClassName());
        Assertions.assertEquals("org.stone.test.beecp.objects.MockRawConnectionPool", config.getPoolImplementClassName());
        Assertions.assertTrue(config.isEnableJmx());

        Assertions.assertEquals("true", config.getConnectProperty("cachePrepStmts"));
        Assertions.assertEquals("50", config.getConnectProperty("prepStmtCacheSize"));
        Assertions.assertEquals("2048", config.getConnectProperty("prepStmtCacheSqlLimit"));
        Assertions.assertEquals("true", config.getConnectProperty("useServerPrepStmts"));
        return true;
    }
}
