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

import java.util.concurrent.TimeUnit;

public class PoolForceClearTest extends TestCase {
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
        ds.restartPool(true);
        long tookTime = System.currentTimeMillis() - time1;
        if (tookTime > delayTimeForNextClear) {
            TestUtil.assertError("Pool force restartPool test failed");
        } else {
            System.out.println("Pool force restartPool time: " + tookTime + "ms");
        }
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

}
