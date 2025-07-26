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
import org.stone.beecp.pool.ProxyConnectionBase;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.sql.Connection;

import static org.stone.test.base.TestUtil.getFieldValue;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0052PoolThreadLocalTest {

    @Test
    public void testGetFromThreadLocal() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setMaxActive(2);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assertions.assertNotNull(getFieldValue(pool, "threadLocal"));

        Connection con1 = pool.getConnection();
        Connection con1Raw = (Connection) getFieldValue(con1, ProxyConnectionBase.class, "raw");
        con1.close();

        Connection con2 = pool.getConnection();
        Connection con2Raw = (Connection) getFieldValue(con2, ProxyConnectionBase.class, "raw");
        con2.close();

        Assertions.assertEquals(con1Raw, con2Raw);
    }

    @Test
    public void testDisableThreadLocal() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setMaxActive(2);
        config.setEnableThreadLocal(false);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assertions.assertNull(getFieldValue(pool, "threadLocal"));

        Connection con1 = pool.getConnection();
        Connection con1Raw = (Connection) getFieldValue(con1, ProxyConnectionBase.class, "raw");
        con1.close();

        Object pooledArray = getFieldValue(pool, "connectionArray");
        Object first = Array.get(pooledArray, 0);
        Object second = Array.get(pooledArray, 1);
        Array.set(pooledArray, 0, second);
        Array.set(pooledArray, 1, first);

        Connection con2 = pool.getConnection();
        Connection con2Raw = (Connection) getFieldValue(con2, ProxyConnectionBase.class, "raw");
        con2.close();
        Assertions.assertNotEquals(con1Raw, con2Raw);
    }

    @Test
    public void testGetConnectionAfterGc() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setMaxActive(2);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        Connection con1 = pool.getConnection();
        Connection con1Raw = (Connection) getFieldValue(con1, ProxyConnectionBase.class, "raw");
        con1.close();

        Object fieldVal = getFieldValue(pool, "threadLocal");
        Assertions.assertInstanceOf(ThreadLocal.class, fieldVal);
        ThreadLocal threadLocal = (ThreadLocal) fieldVal;
        WeakReference weakRef = (WeakReference) threadLocal.get();
        Assertions.assertNotNull(weakRef.get());
        System.gc();
        Assertions.assertNull(weakRef.get());

        Connection con2 = pool.getConnection();
        Connection con2Raw = (Connection) getFieldValue(con2, ProxyConnectionBase.class, "raw");
        con2.close();
        Assertions.assertEquals(con1Raw, con2Raw);
    }
}
