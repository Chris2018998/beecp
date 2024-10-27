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

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.sql.Connection;

import static org.stone.base.TestUtil.getFieldValue;
import static org.stone.base.TestUtil.setFieldValue;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0052PoolThreadLocalTest extends TestCase {

    public void testGetFromThreadLocal() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setMaxActive(2);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        Assert.assertNotNull(getFieldValue(pool, "threadLocal"));
        Connection con1 = pool.getConnection();
        Connection con1Raw = (Connection) getFieldValue(con1, ProxyConnectionBase.class, "raw");
        con1.close();

        setFieldValue(pool, "connectionArray", null);//clear pooledArray;
        Connection con2 = pool.getConnection();
        Connection con2Raw = (Connection) getFieldValue(con2, ProxyConnectionBase.class, "raw");
        con2.close();

        Assert.assertEquals(con1Raw, con2Raw);
    }

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
        Assert.assertTrue(fieldVal instanceof ThreadLocal);
        ThreadLocal threadLocal = (ThreadLocal) fieldVal;
        WeakReference weakRef = (WeakReference) threadLocal.get();
        Assert.assertNotNull(weakRef.get());
        System.gc();
        Assert.assertNull(weakRef.get());

        Connection con2 = pool.getConnection();
        Connection con2Raw = (Connection) getFieldValue(con2, ProxyConnectionBase.class, "raw");
        con2.close();
        Assert.assertEquals(con1Raw, con2Raw);
    }

    public void testDisableThreadLocal() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setMaxActive(2);
        config.setEnableThreadLocal(false);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertNull(getFieldValue(pool, "threadLocal"));

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
        Assert.assertNotEquals(con1Raw, con2Raw);
    }

}
