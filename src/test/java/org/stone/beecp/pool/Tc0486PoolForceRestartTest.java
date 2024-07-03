package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import java.util.concurrent.TimeUnit;

import static org.stone.beecp.config.DsConfigFactory.*;

public class Tc0486PoolForceRestartTest extends TestCase {
    private final long delayTimeForNextClear = TimeUnit.SECONDS.toMillis(10);//10 Seconds
    private BeeDataSource ds;

    public void setUp() {
        int initSize = 10;
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setDriverClassName(JDBC_DRIVER);
        config.setUsername(JDBC_USER);
        config.setPassword(JDBC_PASSWORD);

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
