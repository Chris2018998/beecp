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
import org.stone.beecp.pool.exception.ConnectionGetForbiddenException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0060PoolCloseTest {

    @Test
    public void testClose() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setMaxActive(2);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        assertEquals(2, pool.getTotalSize());
        Assertions.assertFalse(pool.isClosed());
        pool.close();
        Assertions.assertTrue(pool.isClosed());
        assertEquals(0, pool.getTotalSize());

        try {
            pool.getConnection();
            fail("testClose");
        } catch (ConnectionGetForbiddenException e) {
            assertEquals("Pool has been closed or is being cleared", e.getMessage());
        }
    }
}
