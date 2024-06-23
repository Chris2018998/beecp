/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.other.slow;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.pool.FastConnectionPool;

import static org.stone.beecp.config.DsConfigFactory.*;

public class SlowSqlInitTest extends TestCase {

    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JDBC_URL);// give valid URL
        config.setDriverClassName(JDBC_DRIVER);
        config.setUsername(JDBC_USER);
        config.setInitialSize(30);
        config.setMaxActive(30);
        ds = new BeeDataSource(config);
    }

    public void testSlowSql() throws Exception {
        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        Assert.assertEquals(30, pool.getTotalSize());
    }

    public void tearDown() {
        ds.close();
    }
}
