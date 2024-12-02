/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.Connection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
            Assert.assertEquals(0, con.getNetworkTimeout());

            //change properties
            con.setAutoCommit(true);
            con.setReadOnly(true);
            con.setTransactionIsolation(TRANSACTION_SERIALIZABLE);
            con.setSchema("DefaultSchema1");
            con.setCatalog("DefaultCatalog1");
            con.setNetworkTimeout(new ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>()), 10);

            Assert.assertEquals(TRANSACTION_SERIALIZABLE, con.getTransactionIsolation());
            Assert.assertEquals("DefaultSchema1", con.getSchema());
            Assert.assertEquals("DefaultCatalog1", con.getCatalog());
            Assert.assertEquals(10, con.getNetworkTimeout());
        }

        try (Connection con = pool.getConnection()) {
            Assert.assertFalse(con.getAutoCommit());
            Assert.assertFalse(con.isReadOnly());
            Assert.assertEquals(TRANSACTION_READ_COMMITTED, con.getTransactionIsolation());
            Assert.assertEquals("DefaultSchema", con.getSchema());
            Assert.assertEquals("DefaultCatalog", con.getCatalog());
            Assert.assertEquals(0, con.getNetworkTimeout());
        }
    }
}
