/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.pool.ProxyConnectionBase;
import org.stone.test.base.TestUtil;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.test.base.TestUtil.getFieldValue;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0038DsPoolThreadLocalTest {

    @Test
    public void testDisableThreadLocal() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setMaxActive(2);
        config.setUseThreadLocal(false);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            Object dsPool = getFieldValue(ds, "pool");
            Assertions.assertNull(getFieldValue(dsPool, "threadLocal"));

            try (Connection con = ds.getConnection()) {
                Assertions.assertNotNull(con);
            }
        }
    }

    @Test
    public void testEnableThreadLocal() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setMaxActive(2);
        config.setUseThreadLocal(true);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            Object dsPool = getFieldValue(ds, "pool");
            ThreadLocal<WeakReference<Object>> threadLocal = (ThreadLocal<WeakReference<Object>>) getFieldValue(dsPool, "threadLocal");
            Assertions.assertNotNull(threadLocal);

            //1: no cached connection
            Assertions.assertNotNull(threadLocal.get().get());
            Object lastUsedPooledConnection = TestUtil.getFieldValue(threadLocal.get().get(), "lastUsed");
            Assertions.assertNull(lastUsedPooledConnection);//no cached pooled connection

            //2: get a connection from pool
            Connection rawConInProxy1;
            try (Connection con = ds.getConnection()) {
                rawConInProxy1 = (Connection) getFieldValue(con, ProxyConnectionBase.class, "raw");
            }
            //3: get connection cached in threadLocal
            lastUsedPooledConnection = TestUtil.getFieldValue(threadLocal.get().get(), "lastUsed");
            Object cachedRawConn = TestUtil.getFieldValue(lastUsedPooledConnection, "rawConn");
            Assertions.assertSame(cachedRawConn, rawConInProxy1);

            //4: second getting: check cached connection in threadLocal(cached ---> cached reused)
            try (Connection con = ds.getConnection()) {
                Connection rawConInProxy2 = (Connection) getFieldValue(con, ProxyConnectionBase.class, "raw");
                Assertions.assertEquals(rawConInProxy1, rawConInProxy2);
            }

            //4: get Connection after GC check
            System.gc();
            Assertions.assertNull(threadLocal.get().get());//after GC

            try (Connection con = ds.getConnection()) {
                Assertions.assertNotNull(threadLocal.get().get());
                lastUsedPooledConnection = TestUtil.getFieldValue(threadLocal.get().get(), "lastUsed");
                Assertions.assertNotNull(lastUsedPooledConnection);
                cachedRawConn = TestUtil.getFieldValue(lastUsedPooledConnection, "rawConn");
                Assertions.assertNotNull(cachedRawConn);

                //4.2: get raw Connection from Proxy Connection
                Connection rawConInProxy = (Connection) getFieldValue(con, ProxyConnectionBase.class, "raw");

                //4.3:  check same connection
                Assertions.assertSame(cachedRawConn, rawConInProxy);
            }
        }
    }

    @Test
    public void testGetTwoConnections() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setMaxActive(2);
        config.setUseThreadLocal(true);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection con1 = ds.getConnection()) {
                Assertions.assertNotNull(con1);
                try (Connection con2 = ds.getConnection()) {
                    Assertions.assertNotNull(con2);
                }
            }
        }
    }
}
