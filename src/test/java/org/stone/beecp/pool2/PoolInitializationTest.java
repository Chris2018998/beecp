/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.pool2;

import junit.framework.TestCase;
import org.stone.base.TestException;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.ConfigFactory;
import org.stone.beecp.JdbcConfig;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.beecp.pool.exception.PoolInitializeFailedException;

import java.sql.SQLException;

public class PoolInitializationTest extends TestCase {

    public void testNullConfig() throws Exception {
        FastConnectionPool pool = new FastConnectionPool();
        try {
            pool.init(null);
            throw new TestException();
        } catch (PoolInitializeFailedException e) {
            //do nothing
        }
    }

    public void testInitialSQLException() throws Exception {
        FastConnectionPool pool = new FastConnectionPool();
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl("Test:" + JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        try {
            pool.init(config);
            throw new TestException();
        } catch (SQLException e) {
            //do nothing
        }
    }

    public void testPoolInitializeFailedException() throws Exception {
        FastConnectionPool pool = new FastConnectionPool();
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setInitialSize(10);
        config.setMaxActive(5);

        try {
            pool.init(config);
            throw new TestException();
        } catch (PoolInitializeFailedException e) {
            //do nothing
        }
    }

    public void testOnAlreadyInitialized() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        try {
            pool.init(config);
            throw new TestException();
        } catch (PoolInitializeFailedException e) {
            //do nothing
        }
    }

    public void testInFairMode() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setFairMode(true);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
    }

    public void testInCompeteMode() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setFairMode(false);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
    }

    public void testInSyncCreation() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLog(true);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
    }

    public void testInASyncCreation() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLog(true);
        config.setAsyncCreateInitConnection(true);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
    }
}
