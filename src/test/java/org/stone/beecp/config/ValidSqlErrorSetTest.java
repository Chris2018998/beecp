/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.config;

import org.stone.base.TestCase;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.JdbcConfig;

/**
 * @author Chris Liao
 * @version 1.0
 */

public class ValidSqlErrorSetTest extends TestCase {
    public void test() throws Exception {
        BeeDataSourceConfig testConfig = new BeeDataSourceConfig();
        String url = JdbcConfig.JDBC_URL;
        testConfig.setJdbcUrl(url);
        testConfig.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        testConfig.setUsername(JdbcConfig.JDBC_USER);
        testConfig.setPassword(JdbcConfig.JDBC_PASSWORD);
        testConfig.setValidTestSql("?={call test(}");
        try {
            testConfig.check();
        } catch (BeeDataSourceConfigException e) {
            if (!e.getMessage().equals("validTestSql must be start with 'select '"))
                throw e;
        }
    }
}