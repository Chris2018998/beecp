/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.Connection;

import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_SERIALIZABLE;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0102ConnectionResetTest extends TestCase {

    public void testDirtyReset() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setDefaultAutoCommit(false);
        config.setDefaultSchema("DefaultSchema");
        config.setDefaultCatalog("DefaultCatalog");
        config.setDefaultReadOnly(false);
        config.setDefaultTransactionIsolationCode(TRANSACTION_READ_COMMITTED);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        try (Connection con = pool.getConnection()) {
            Assert.assertFalse(con.getAutoCommit());
            Assert.assertFalse(con.isReadOnly());
            Assert.assertEquals(TRANSACTION_READ_COMMITTED, con.getTransactionIsolation());
            Assert.assertEquals("DefaultSchema", con.getSchema());
            Assert.assertEquals("DefaultCatalog", con.getCatalog());

            //change properties
            con.setAutoCommit(true);
            con.setReadOnly(true);
            con.setTransactionIsolation(TRANSACTION_SERIALIZABLE);
            con.setSchema("DefaultSchema1");
            con.setCatalog("DefaultCatalog1");
        }

        try (Connection con = pool.getConnection()) {
            Assert.assertFalse(con.getAutoCommit());
            Assert.assertFalse(con.isReadOnly());
            Assert.assertEquals(TRANSACTION_READ_COMMITTED, con.getTransactionIsolation());
            Assert.assertEquals("DefaultSchema", con.getSchema());
            Assert.assertEquals("DefaultCatalog", con.getCatalog());
        }
    }
}
