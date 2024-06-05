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
import org.stone.beecp.config.DsConfigFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class Tc0055ConnectionHoldTimeoutTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setInitialSize(0);
        config.setAliveTestSql("SELECT 1 from dual");

        config.setHoldTimeout(500L);// hold and not using connection;
        config.setTimerCheckInterval(1000L);// two seconds interval
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void test() throws Exception {
        Connection con = null;
        try {
            FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
            con = ds.getConnection();
            Assert.assertEquals(1, pool.getTotalSize());
            Assert.assertEquals(1, pool.getUsingSize());

            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
            Assert.assertEquals(0, pool.getUsingSize());

            try {
                con.getCatalog();
                fail("must throw closed exception");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof SQLException);
            }

            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
        } finally {
            if (con != null)
                TestUtil.oclose(con);
        }
    }
}
