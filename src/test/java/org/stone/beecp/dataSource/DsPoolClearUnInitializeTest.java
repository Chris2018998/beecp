/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.dataSource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.pool.exception.PoolNotCreatedException;

import java.sql.SQLException;

public class DsPoolClearUnInitializeTest extends TestCase {

    public void testPoolClear() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        try {
            ds.clear(false);
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof PoolNotCreatedException);
            //if (!(e instanceof PoolNotCreatedException)) throw new TestException();
        }

        try {
            ds.clear(false, new BeeDataSourceConfig());
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof PoolNotCreatedException);
            // if (!(e instanceof PoolNotCreatedException)) throw new TestException();
        }
    }
}
