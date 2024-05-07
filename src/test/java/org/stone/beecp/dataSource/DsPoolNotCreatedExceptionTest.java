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
import org.stone.base.TestException;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.JdbcConfig;
import org.stone.beecp.pool.exception.PoolNotCreatedException;

import java.sql.SQLException;

public class DsPoolNotCreatedExceptionTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        this.ds = new BeeDataSource();
        ds.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        ds.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        ds.setUsername(JdbcConfig.JDBC_USER);
    }

    public void tearDown() {
        ds.close();
    }

    public void testException() throws Exception {
        try {
            ds.getPoolMonitorVo();
        } catch (SQLException e) {
            if (!(e instanceof PoolNotCreatedException)) throw new TestException();
        }

        try {
            ds.getPoolLockHoldTime();
        } catch (SQLException e) {
            if (!(e instanceof PoolNotCreatedException)) throw new TestException();
        }

        try {
            ds.interruptOnPoolLock();
        } catch (SQLException e) {
            if (!(e instanceof PoolNotCreatedException)) throw new TestException();
        }

        try {
            ds.clear(false);
        } catch (SQLException e) {
            if (!(e instanceof PoolNotCreatedException)) throw new TestException();
        }

        try {
            ds.clear(false, null);
        } catch (SQLException e) {
            if (!(e instanceof PoolNotCreatedException)) throw new TestException();
        }
    }
}
