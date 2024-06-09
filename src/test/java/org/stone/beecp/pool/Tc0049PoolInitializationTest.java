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
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.config.DsConfigFactory;
import org.stone.beecp.pool.exception.PoolInitializeFailedException;

import static org.stone.beecp.config.DsConfigFactory.JDBC_DRIVER;
import static org.stone.beecp.config.DsConfigFactory.JDBC_URL;

public class Tc0049PoolInitializationTest extends TestCase {

    public void testNullConfig() throws Exception {
        FastConnectionPool pool = new FastConnectionPool();
        try {
            pool.init(null);
        } catch (PoolInitializeFailedException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Pool initialization configuration can't be null"));
        }
    }

    public void testDuplicatedInitialization() throws Exception {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        try {
            pool.init(config);
            fail("testDuplicatedInitialization test failed");
        } catch (PoolInitializeFailedException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Pool has already been initialized or in initializing"));
        }
    }

    public void testUrlNotMatchDriver() throws Exception {
        FastConnectionPool pool = new FastConnectionPool();
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl("Test:" + JDBC_URL);
        config.setDriverClassName(JDBC_DRIVER);
        try {
            pool.init(config);
            fail("test failed on driver match");
        } catch (PoolInitializeFailedException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof BeeDataSourceConfigException);
            String message = cause.getMessage();
            Assert.assertTrue(message != null && message.contains("can not match configured driver"));
        }
    }

    public void testCheckFailedOnInitialization() throws Exception {
        FastConnectionPool pool = new FastConnectionPool();
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setDriverClassName(JDBC_DRIVER);
        config.setInitialSize(10);
        config.setMaxActive(5);

        try {
            pool.init(config);
            fail("test failed on driver match");
        } catch (PoolInitializeFailedException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof BeeDataSourceConfigException);
            String message = cause.getMessage();
            Assert.assertTrue(message != null && message.contains("initialSize must not be greater than maxActive"));
        }
    }

    public void testPoolInitializeInFairMode() throws Exception {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setFairMode(true);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertEquals("fair", TestUtil.getFieldValue(pool, "poolMode"));
    }

    public void testPoolInitializeInCompetedMode() throws Exception {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setFairMode(false);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertEquals("compete", TestUtil.getFieldValue(pool, "poolMode"));
    }

    public void testCreateInitialConnectionBySynModeForCover() throws Exception {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setInitialSize(1);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
    }

    public void testCreateInitialConnectionByAsynModeForCover() throws Exception {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLog(true);
        config.setAsyncCreateInitConnection(true);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
    }


    public void testTimeoutOnCreateLock() throws Exception {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLog(true);
        config.setAsyncCreateInitConnection(true);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
    }
}
