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
public class Case15_ConfigLoadFromFileTest extends TestCase {
    //private final String ConfigedUrl="jdbc:mysql://localhost/test";
    //private final String ConfigedDriver="com.mysql.cj.jdbc.Driver";
    private final String ConfigUrl = "jdbc:beecp://localhost/testdb";
    private final String ConfigDriver = "org.stone.beecp.mock.MockDriver";
    private final String filename = "beecp/config2.properties";

    /********************************************Constructor**************************************************/
    public void testOnConstructor() throws Exception {
        check(new BeeDataSourceConfig(filename));
        check(new BeeDataSourceConfig(getPropertiesFile()));
        check(new BeeDataSourceConfig(getFileProperties()));
    }

    public void testOnLoadFromFile() throws Exception {
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.loadFromPropertiesFile(filename);
        check(config1);

        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.loadFromPropertiesFile(getPropertiesFile());
        check(config2);

        BeeDataSourceConfig config3 = new BeeDataSourceConfig();
        config3.loadFromProperties(getFileProperties());
        check(config3);
    }

    public void testOnInvalidFile() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        try {
            config.loadFromProperties(null);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        try {
            config.loadFromProperties(new Properties());
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }


        try {
            config.loadFromPropertiesFile((File) null);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        try {
            config.loadFromPropertiesFile((String) null);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }


        try {
            config.loadFromPropertiesFile(new File("dd/dd/dd/dd/config1"));
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        try {
            config.loadFromPropertiesFile(new File("dd/dd/dd/dd/config1.properties"));
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    private Properties getFileProperties() throws Exception {
        Class selfClass = Case15_ConfigLoadFromFileTest.class;
        InputStream propertiesStream = selfClass.getResourceAsStream(filename);
        if (propertiesStream == null) propertiesStream = selfClass.getClassLoader().getResourceAsStream(filename);

        Properties prop = new Properties();
        prop.load(propertiesStream);
        return prop;
    }

    private File getPropertiesFile() throws Exception {
        Class selfClass = Case15_ConfigLoadFromFileTest.class;
        URL fileUrl = selfClass.getResource(filename);
        if (fileUrl == null) fileUrl = selfClass.getClassLoader().getResource(filename);
        return new File(fileUrl.toURI());
    }

    private void check(BeeDataSourceConfig config) throws Exception {
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
        Assert.assertEquals(config.getSqlExceptionPredicationClassName(), "com.myProject.TestPredication");

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
