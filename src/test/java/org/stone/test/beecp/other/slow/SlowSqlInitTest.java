/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.test.beecp.other.slow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.test.base.TestUtil;

import static org.stone.test.beecp.config.DsConfigFactory.*;

public class SlowSqlInitTest {

    private BeeDataSource ds;

    @BeforeEach
    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JDBC_URL);// give valid URL
        config.setDriverClassName(JDBC_DRIVER);
        config.setUsername(JDBC_USER);
        config.setInitialSize(30);
        config.setMaxActive(30);
        ds = new BeeDataSource(config);
    }

    @AfterEach
    public void tearDown() {
        ds.close();
    }

    @Test
    public void testSlowSql() throws Exception {
        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        Assertions.assertEquals(30, pool.getTotalSize());
    }


}
