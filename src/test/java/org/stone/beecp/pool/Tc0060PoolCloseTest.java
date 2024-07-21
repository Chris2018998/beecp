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
import org.stone.beecp.pool.exception.ConnectionGetForbiddenException;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0060PoolCloseTest extends TestCase {

    public void testClose() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setMaxActive(2);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        Assert.assertEquals(2, pool.getTotalSize());
        Assert.assertFalse(pool.isClosed());
        pool.close();
        Assert.assertTrue(pool.isClosed());
        Assert.assertEquals(0, pool.getTotalSize());

        try {
            pool.getConnection();
        } catch (ConnectionGetForbiddenException e) {
            assertEquals(e.getMessage(), "Pool was closed or in clearing");
        }
    }
}
