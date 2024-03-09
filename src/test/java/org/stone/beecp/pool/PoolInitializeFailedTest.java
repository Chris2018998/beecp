/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;

public class PoolInitializeFailedTest extends TestCase {
    private final int initSize = 5;

    public void setUp() throws Throwable {
        //do nothing
    }

    public void tearDown() throws Throwable {
        //do nothing
    }

    public void testPoolInit() throws Exception {
        try {
            BeeDataSourceConfig config = new BeeDataSourceConfig();
            config.setJdbcUrl("jdbc:beecp://localhost/testdb2");//give valid URL
            config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
            config.setUsername(JdbcConfig.JDBC_USER);
            config.setPassword(JdbcConfig.JDBC_PASSWORD);
            config.setInitialSize(initSize);
            new BeeDataSource(config);
            TestUtil.assertError("A initializerError need be thrown,but not");
        } catch (RuntimeException e) {
            System.out.println(e.getCause());
        }
    }
}
