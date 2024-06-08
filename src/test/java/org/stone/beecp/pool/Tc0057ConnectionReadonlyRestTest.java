/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.config.DsConfigFactory;

import java.sql.Connection;

public class Tc0057ConnectionReadonlyRestTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(1);
        config.setAliveTestSql("SELECT 1 from dual");
        config.setIdleTimeout(3000);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void test() throws Exception {
        Connection con1 = null;
        try {
            con1 = ds.getConnection();
            con1.setReadOnly(true);
            Assert.assertTrue(con1.isReadOnly());
        } finally {
            if (con1 != null) con1.close();
        }

        Connection con2 = null;
        try {
            con2 = ds.getConnection();
            Assert.assertFalse(con2.isReadOnly());
        } finally {
            if (con2 != null) con2.close();
        }
    }
}
