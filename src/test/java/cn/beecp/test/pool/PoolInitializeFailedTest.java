/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test.pool;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import cn.beecp.test.JdbcConfig;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;

public class PoolInitializeFailedTest extends TestCase {
    private int initSize = 5;

    public void setUp() throws Throwable {
    }

    public void tearDown() throws Throwable {
    }

    public void testPoolInit() throws InterruptedException, Exception {
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
