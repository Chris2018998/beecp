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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSourceConfig;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class Tc0018ConfigLoadFromFileTest {
    //classes/beecp/file1.
    private static String beecpResourceAbsolutePath;

    @BeforeAll
    public static void resourcePath() throws Exception {
        String filename = "beecp/config1.properties";//must be existed
        URL resourceURL = Tc0018ConfigLoadFromFileTest.class.getClassLoader().getResource(filename);
        Assertions.assertNotNull(resourceURL);
        File resourceFile = new File(resourceURL.toURI());
        beecpResourceAbsolutePath = resourceFile.getParent();
    }

    /********************************************Constructor**************************************************/
    @Test
    public void testCorrectFile() throws Exception {
        String filename = "config1.properties";
        String classPathFilename1 = "cp:beecp/" + filename;
        String classPathFilename2 = "classpath:beecp/" + filename;
        String absolutePathFileName = beecpResourceAbsolutePath + File.separator + filename;
        File fileFile = new File(absolutePathFileName);

        //1: load file in constructor
        //1.1: load file from class path
        Assertions.assertTrue(check(new BeeDataSourceConfig(classPathFilename1)));//classpath
        Assertions.assertTrue(check(new BeeDataSourceConfig(classPathFilename2)));//classpath

        //1.2: load file from absolution path
        Assertions.assertTrue(check(new BeeDataSourceConfig(fileFile)));//from file
        Assertions.assertTrue(check(new BeeDataSourceConfig(absolutePathFileName)));//from file


        //1.3: load from Properties
        Properties properties = new Properties();
        try (FileInputStream fileStream = new FileInputStream(fileFile)) {
            properties.load(fileStream);
        }
        Assertions.assertTrue(check(new BeeDataSourceConfig(properties)));//from properties

        //2: load configuration by methods
        //2.1: load file from class path
        BeeDataSourceConfig config1 = createEmpty();
        config1.load(classPathFilename1);
        Assertions.assertTrue(check(config1));
        config1 = createEmpty();
        config1.load(classPathFilename1);
        Assertions.assertTrue(check(config1));
        //2.2: load file from absolution path
        BeeDataSourceConfig config2 = createEmpty();
        config2.load(fileFile);
        Assertions.assertTrue(check(config2));
        config2 = createEmpty();
        config2.load(absolutePathFileName);
        Assertions.assertTrue(check(config2));

        //2.3: load file from absolution path
        BeeDataSourceConfig config3 = createEmpty();
        config3.load(properties);
        Assertions.assertTrue(check(config3));
    }

    @Test
    public void testInvalidFileName() {
        BeeDataSourceConfig config = createEmpty();
        try {//null filename
            config.load((String) null);
            fail("[testInvalidFileName]failed");
        } catch (Exception e) {
            Assertions.assertEquals("Load file name cannot be null or empty", e.getMessage());
        }

        try {//blank filename
            config.load("");
            fail("[testInvalidFileName]failed");
        } catch (Exception e) {
            Assertions.assertEquals("Load file name cannot be null or empty", e.getMessage());
        }

        try {//file extension name test
            config.load(beecpResourceAbsolutePath + File.separator + "invalidProperties");
            fail("[testInvalidFileName]failed");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Load file extension name must be 'properties':"));
        }
    }

    @Test
    public void testInvalidFile() {
        BeeDataSourceConfig config = createEmpty();
        try {//null filename
            config.load((File) null);
            fail("[testInvalidFile]failed");
        } catch (Exception e) {
            Assertions.assertEquals("Load file cannot be null", e.getMessage());
        }

        try {//file not found test
            config.load(new File(beecpResourceAbsolutePath + File.separator + "not_found.properties"));
            fail("[testInvalidFile]failed");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Load file not found"));
        }

        try {//test file is a folder
            config.load(new File(beecpResourceAbsolutePath + File.separator + "empty"));
            fail("[testInvalidFile]failed");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Load file cannot be a folder"), message);
        }

        try {//file extension name test
            config.load(new File(beecpResourceAbsolutePath + File.separator + "invalidProperties"));
            fail("[testInvalidFile]failed");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Load file extension name must be 'properties':"), message);
        }
    }


    private boolean check(BeeDataSourceConfig config) {
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
        Assertions.assertEquals(config.getDefaultTransactionIsolation(), Integer.valueOf(1));
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
        Assertions.assertEquals(30000, config.getIntervalOfClearTimeout());
        Assertions.assertTrue(config.isForceRecycleBorrowedOnClose());
        Assertions.assertEquals(3000, config.getParkTimeForRetry());
        Assertions.assertEquals("com.myProject.TestPredication", config.getPredicateClassName());

        List<Integer> sqlExceptionCodeList = config.getSqlExceptionCodeList();
        List<String> sqlExceptionStateList = config.getSqlExceptionStateList();
        for (Integer code : sqlExceptionCodeList)
            Assertions.assertTrue(code == 500150 || code == 2399);
        for (String state : sqlExceptionStateList)
            Assertions.assertTrue("0A000".equals(state) || "57P01".equals(state));

        Assertions.assertEquals("org.stone.beecp.pool.ConnectionFactoryByDriver", config.getConnectionFactoryClassName());
        Assertions.assertEquals("org.stone.test.beecp.objects.pool.MockRawConnectionPool", config.getPoolImplementClassName());
        Assertions.assertTrue(config.isRegisterMbeans());

        Assertions.assertEquals("true", config.getConnectionFactoryProperty("cachePrepStmts"));
        Assertions.assertEquals("50", config.getConnectionFactoryProperty("prepStmtCacheSize"));
        Assertions.assertEquals("2048", config.getConnectionFactoryProperty("prepStmtCacheSqlLimit"));
        Assertions.assertEquals("true", config.getConnectionFactoryProperty("useServerPrepStmts"));
        return Boolean.TRUE;
    }
}
