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

import java.util.concurrent.TimeUnit;

public class PoolForceRestartTest extends TestCase {
    private final int initSize = 10;
    private final long delayTimeForNextClear = TimeUnit.SECONDS.toMillis(10);//10 Seconds
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        config.setInitialSize(initSize);
        config.setDelayTimeForNextClear(delayTimeForNextClear);//Ms
        ds = new BeeDataSource(config);
    }

    public void testForceClear() throws Throwable {
        long time1 = System.currentTimeMillis();
        ds.clear(true);
        long tookTime = System.currentTimeMillis() - time1;
        if (tookTime > delayTimeForNextClear) {
            TestUtil.assertError("Pool force clear test failed");
        } else {
            System.out.println("Pool force clear parkTime: " + tookTime + "ms");
        }
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

}
