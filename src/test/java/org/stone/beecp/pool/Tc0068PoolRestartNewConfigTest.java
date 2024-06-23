package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

public class Tc0068PoolRestartNewConfigTest extends TestCase {
    private final int initSize = 5;
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(initSize);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testRestartNewConfig() throws Exception {
        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        Assert.assertEquals(initSize, pool.getTotalSize());
        Assert.assertEquals(initSize, pool.getIdleSize());
        pool.clear(false);
        Assert.assertEquals(0, pool.getTotalSize());
        Assert.assertEquals(0, pool.getIdleSize());
    }
}
