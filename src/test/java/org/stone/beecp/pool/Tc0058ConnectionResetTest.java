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

import java.sql.Connection;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0058ConnectionResetTest extends TestCase {

    public void testReadonlyRest() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(1);
        config.setAliveTestSql("SELECT 1 from dual");
        config.setIdleTimeout(3000);
        BeeDataSource ds = new BeeDataSource(config);

        try (Connection con1 = ds.getConnection()) {
            con1.setReadOnly(true);
            Assert.assertTrue(con1.isReadOnly());
        }

        try (Connection con2 = ds.getConnection()) {
            Assert.assertFalse(con2.isReadOnly());
        }
    }

    public void testCatalogReset() throws Exception {
        String catalog = "mysql";
        String schema = "mysql";
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setDefaultAutoCommit(false);
        config.setDefaultTransactionIsolationName(TransactionIsolation.LEVEL_READ_COMMITTED);
        config.setDefaultReadOnly(true);
        config.setDefaultCatalog(catalog);
        config.setDefaultSchema(schema);
        BeeDataSource ds = new BeeDataSource(config);

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
