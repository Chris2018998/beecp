/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0057ConnectionIdleTest extends TestCase {

    public void testIdleTimeout() throws Exception {
        final int initSize = 5;
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(initSize);
        config.setMaxActive(initSize);
        config.setAliveTestSql("SELECT 1 from dual");
        config.setIdleTimeout(1000);
        config.setTimerCheckInterval(1000);
        BeeDataSource ds = new BeeDataSource(config);
        try {
            FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");

            Assert.assertEquals(initSize, pool.getTotalSize());
            Assert.assertEquals(initSize, pool.getIdleSize());
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));

            Assert.assertEquals(0, pool.getTotalSize());
            Assert.assertEquals(0, pool.getIdleSize());
        } finally {
            ds.close();
        }
    }
}
