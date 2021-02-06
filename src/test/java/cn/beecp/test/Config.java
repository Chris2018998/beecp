/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.beecp.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Some JDBC info
 *
 * @author Administrator
 */
public class Config {
    public static String JDBC_USER;
    public static String JDBC_PASSWORD;
    public static String JDBC_DRIVER;
    public static String JDBC_URL;

    public static int POOL_MAX_ACTIVE;
    public static int POOL_INIT_SIZE;
    public static int REQUEST_TIMEOUT = 8000;
    public static String TEST_TABLE = "BEECP_TEST";
    public static String TEST_PROCEDURE = "BEECP_HELLO()";
    public static String CONFIG_FILE = "config.properties";

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
            fileStream = Config.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
            if (fileStream == null) fileStream = Config.class.getResourceAsStream(CONFIG_FILE);
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
                throw new Exception("'USER_ID' missed");
            if (JDBC_DRIVER == null || JDBC_DRIVER.trim().length() == 0)
                throw new Exception("'JDBC_DRIVER' missed");
            if (JDBC_URL == null || JDBC_URL.trim().length() == 0)
                throw new Exception("'JDBC_URL' missed");
            if (TEST_TABLE == null || TEST_TABLE.trim().length() == 0)
                throw new Exception("'TEST_TABLE' missed");
            if (TEST_PROCEDURE == null || TEST_PROCEDURE.trim().length() == 0)
                throw new Exception("'TEST_PROCEDURE' missed");

            if (POOL_MAX_ACTIVE <= 0)
                throw new Exception("'POOL_MAX_ACTIVE' must be more than zero");
            if (POOL_INIT_SIZE < 0)
                throw new Exception("'POOL_INIT_SIZE' can't be less than zero");
            if (POOL_INIT_SIZE > POOL_MAX_ACTIVE)
                throw new Exception("'POOL_INIT_SIZE' must be less than 'POOL_MAX_ACTIVE'");
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