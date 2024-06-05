package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;

import java.util.concurrent.TimeUnit;

public class Tc0065PoolForceRestartTest extends TestCase {
    private final long delayTimeForNextClear = TimeUnit.SECONDS.toMillis(10);//10 Seconds
    private BeeDataSource ds;

    public void setUp() {
        int initSize = 10;
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
        Assert.assertTrue(tookTime <= delayTimeForNextClear);
    }

    public void tearDown() {
        ds.close();
    }

}
