package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.config.DsConfigFactory;

public class Tc0069PoolRestartTest extends TestCase {
    private final int initSize = 5;
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setInitialSize(initSize);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testPoolRestart() throws Exception {
        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        Assert.assertEquals(initSize, pool.getTotalSize());
        Assert.assertEquals(initSize, pool.getIdleSize());
        BeeDataSourceConfig config2 = DsConfigFactory.createDefault();
        config2.setInitialSize(10);
        pool.clear(true, config2);
        Assert.assertEquals(10, pool.getTotalSize());
        Assert.assertEquals(10, pool.getIdleSize());
    }
}
