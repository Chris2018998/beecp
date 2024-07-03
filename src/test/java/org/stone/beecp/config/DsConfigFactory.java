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

import org.stone.beecp.BeeDataSourceConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.stone.tools.CommonUtil.isBlank;

/**
 * Config factory
 *
 * @author Chris Liao
 */

public class DsConfigFactory {
    public static String JDBC_USER;
    public static String JDBC_PASSWORD;
    public static String JDBC_DRIVER;
    public static String JDBC_URL;
    public static String TEST_TABLE = "BEECP_TEST";
    public static String TEST_PROCEDURE = "BEECP_HELLO()";
    public static String MOCK_URL = "jdbc:beecp://localhost/testdb";
    public static String MOCK_DRIVER = "org.stone.beecp.driver.MockDriver";

    static int POOL_MAX_ACTIVE;
    static int POOL_INIT_SIZE;
    static int REQUEST_TIMEOUT = 8000;
    static String CONFIG_FILE = "beecp/jdbc.properties";

    static {
        try {
            loadConfig();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static BeeDataSourceConfig createEmpty() {
        return new BeeDataSourceConfig();
    }

    public static BeeDataSourceConfig createDefault() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(DsConfigFactory.JDBC_URL);
        config.setDriverClassName(DsConfigFactory.JDBC_DRIVER);
        return config;
    }

    static void clearBeeCPInfoFromSystemProperties() {
        Properties properties = System.getProperties();

        properties.keySet().removeIf(key -> key.toString().startsWith("beecp."));
    }

    private static void loadConfig() throws Exception {
        InputStream fileStream = null;

        try {
            fileStream = DsConfigFactory.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
            if (fileStream == null) fileStream = DsConfigFactory.class.getResourceAsStream(CONFIG_FILE);
            if (fileStream == null) throw new IOException("Can't find file:'JdbcConfig.properties' in classpath");
            Properties prop = new Properties();
            prop.load(fileStream);

            JDBC_USER = prop.getProperty("JDBC_USER");
            JDBC_PASSWORD = prop.getProperty("JDBC_PASSWORD");
            JDBC_DRIVER = prop.getProperty("JDBC_DRIVER");
            JDBC_URL = prop.getProperty("JDBC_URL");

            TEST_TABLE = prop.getProperty("TEST_TABLE");
            TEST_PROCEDURE = prop.getProperty("TEST_PROCEDURE");

            POOL_MAX_ACTIVE = Integer.parseInt(prop.getProperty("POOL_MAX_ACTIVE"));
            POOL_INIT_SIZE = Integer.parseInt(prop.getProperty("POOL_INIT_SIZE"));
            try {
                REQUEST_TIMEOUT = Integer.parseInt(prop.getProperty("REQUEST_TIMEOUT"));
            } catch (Exception e) {
            }

            if (isBlank(JDBC_USER))
                throw new IllegalArgumentException("'USER_ID' missed");
            if (isBlank(JDBC_DRIVER))
                throw new IllegalArgumentException("'JDBC_DRIVER' missed");
            if (isBlank(JDBC_URL))
                throw new IllegalArgumentException("'JDBC_URL' missed");
            if (isBlank(TEST_TABLE))
                throw new IllegalArgumentException("'TEST_TABLE' missed");
            if (isBlank(TEST_PROCEDURE))
                throw new IllegalArgumentException("'TEST_PROCEDURE' missed");

            if (POOL_MAX_ACTIVE <= 0)
                throw new IllegalArgumentException("'POOL_MAX_ACTIVE' must be more than zero");
            if (POOL_INIT_SIZE < 0)
                throw new IllegalArgumentException("'POOL_INIT_SIZE' can't be less than zero");
            if (POOL_INIT_SIZE > POOL_MAX_ACTIVE)
                throw new IllegalArgumentException("'POOL_INIT_SIZE' must be less than 'POOL_MAX_ACTIVE'");
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
