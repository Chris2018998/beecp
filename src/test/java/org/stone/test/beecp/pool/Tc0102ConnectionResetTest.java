/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.pool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.test.base.TestUtil;

import java.sql.Connection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_SERIALIZABLE;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0102ConnectionResetTest {

    @Test
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

        Connection con1 = null;
        Connection con2 = null;
        Connection raw1, raw2;
        try {
            con1 = pool.getConnection();
            raw1 = (Connection) TestUtil.getFieldValue(con1, "raw");

            Assertions.assertFalse(con1.getAutoCommit());
            Assertions.assertFalse(con1.isReadOnly());
            Assertions.assertEquals(TRANSACTION_READ_COMMITTED, con1.getTransactionIsolation());
            Assertions.assertEquals("DefaultSchema", con1.getSchema());
            Assertions.assertEquals("DefaultCatalog", con1.getCatalog());
            Assertions.assertEquals(0, con1.getNetworkTimeout());

            //change properties
            con1.setAutoCommit(true);
            con1.setReadOnly(true);
            con1.setTransactionIsolation(TRANSACTION_SERIALIZABLE);
            con1.setSchema("DefaultSchema1");
            con1.setCatalog("DefaultCatalog1");
            con1.setNetworkTimeout(new ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()), 10);
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500L));

            Assertions.assertEquals(TRANSACTION_SERIALIZABLE, con1.getTransactionIsolation());
            Assertions.assertEquals("DefaultSchema1", con1.getSchema());
            Assertions.assertEquals("DefaultCatalog1", con1.getCatalog());

            Assertions.assertEquals(10, con1.getNetworkTimeout());
        } finally {
            if (con1 != null) TestUtil.oclose(con1);
        }

        try {
            con2 = pool.getConnection();
            raw2 = (Connection) TestUtil.getFieldValue(con2, "raw");
            Assertions.assertEquals(raw1, raw2);

            Assertions.assertFalse(con2.getAutoCommit());
            Assertions.assertFalse(con2.isReadOnly());
            Assertions.assertEquals(TRANSACTION_READ_COMMITTED, con2.getTransactionIsolation());
            Assertions.assertEquals("DefaultSchema", con2.getSchema());
            Assertions.assertEquals("DefaultCatalog", con2.getCatalog());
            Assertions.assertEquals(0, con2.getNetworkTimeout());
        } finally {
            if (con2 != null) TestUtil.oclose(con2);
        }
    }
}
