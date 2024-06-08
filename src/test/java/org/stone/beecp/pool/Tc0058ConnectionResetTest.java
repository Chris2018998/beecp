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
import org.stone.beecp.TransactionIsolation;
import org.stone.beecp.config.DsConfigFactory;

import java.sql.Connection;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0058ConnectionResetTest extends TestCase {
    String catlog = "mysql";
    String schema = "mysql";
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setDefaultAutoCommit(false);
        config.setDefaultTransactionIsolationName(TransactionIsolation.LEVEL_READ_COMMITTED);
        config.setDefaultReadOnly(true);
        config.setDefaultCatalog(catlog);
        config.setDefaultSchema(schema);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void test() throws Exception {
        Connection con = null;
        try {
            con = ds.getConnection();
            con.setAutoCommit(true);
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setReadOnly(false);
            con.setCatalog("test");
        } finally {
            if (con != null)
                oclose(con);
        }

        try {
            FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
            Assert.assertEquals(1, pool.getTotalSize());

            con = ds.getConnection();
            Assert.assertEquals(Connection.TRANSACTION_READ_COMMITTED, con.getTransactionIsolation());
            Assert.assertTrue(con.isReadOnly());//reset to true(default)
        } finally {
            if (con != null)
                oclose(con);
        }
    }
}
