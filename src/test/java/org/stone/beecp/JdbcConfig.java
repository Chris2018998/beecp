/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp;

import org.stone.base.TestException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Some JDBC info
 *
 * @author Administrator
 */
public class JdbcConfig {
    public static String JDBC_USER;
    public static String JDBC_PASSWORD;
    public static String JDBC_DRIVER;
    public static String JDBC_URL;
    public static String TEST_TABLE = "BEECP_TEST";
    public static String TEST_PROCEDURE = "BEECP_HELLO()";

    static int POOL_MAX_ACTIVE;
    static int POOL_INIT_SIZE;
    static int REQUEST_TIMEOUT = 8000;
    static String CONFIG_FILE = "beecp/jdbc.properties";

    static {
        try {
            loadConfig();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadConfig() throws Exception {
        InputStream fileStream = null;

        try {
            fileStream = JdbcConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
            if (fileStream == null) fileStream = JdbcConfig.class.getResourceAsStream(CONFIG_FILE);
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

            if (JDBC_USER == null || JDBC_USER.trim().length() == 0)
                throw new TestException("'USER_ID' missed");
            if (JDBC_DRIVER == null || JDBC_DRIVER.trim().length() == 0)
                throw new TestException("'JDBC_DRIVER' missed");
            if (JDBC_URL == null || JDBC_URL.trim().length() == 0)
                throw new TestException("'JDBC_URL' missed");
            if (TEST_TABLE == null || TEST_TABLE.trim().length() == 0)
                throw new TestException("'TEST_TABLE' missed");
            if (TEST_PROCEDURE == null || TEST_PROCEDURE.trim().length() == 0)
                throw new TestException("'TEST_PROCEDURE' missed");

            if (POOL_MAX_ACTIVE <= 0)
                throw new TestException("'POOL_MAX_ACTIVE' must be more than zero");
            if (POOL_INIT_SIZE < 0)
                throw new TestException("'POOL_INIT_SIZE' can't be less than zero");
            if (POOL_INIT_SIZE > POOL_MAX_ACTIVE)
                throw new TestException("'POOL_INIT_SIZE' must be less than 'POOL_MAX_ACTIVE'");
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