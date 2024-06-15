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
import org.stone.base.StoneLogAppender;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.config.DsConfigFactory;
import org.stone.beecp.objects.CountNullConnectionFactory;
import org.stone.beecp.pool.exception.PoolInitializeFailedException;

import java.sql.SQLException;

import static org.stone.base.TestUtil.getStoneLogAppender;
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

    public void testOnEnableThreadLocal() throws Exception {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertNotNull(TestUtil.getFieldValue(pool, "threadLocal"));
        pool.close();

        config.setEnableThreadLocal(false);
        pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertNull(TestUtil.getFieldValue(pool, "threadLocal"));
        pool.close();
    }

    public void testPoolInitializeInFairModeAndCompeteMode() throws Exception {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setFairMode(true);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertEquals("fair", TestUtil.getFieldValue(pool, "poolMode"));
        pool.close();

        BeeDataSourceConfig config2 = DsConfigFactory.createDefault();
        config2.setFairMode(false);
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        Assert.assertEquals("compete", TestUtil.getFieldValue(pool2, "poolMode"));
        pool2.close();
    }

    public void testSyncCreateInitialConnections() throws Exception {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setInitialSize(1);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        pool.close();

        BeeDataSourceConfig config2 = DsConfigFactory.createDefault();
        config2.setInitialSize(1);
        config2.setPrintRuntimeLog(true);
        config2.setAsyncCreateInitConnection(true);//<--async
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        pool2.close();
    }

    public void testClearConnectionsOnInitializeFailed() throws Exception {
        FastConnectionPool pool =null;
        try {
            BeeDataSourceConfig config = DsConfigFactory.createDefault();
            config.setInitialSize(2);
            config.setRawConnectionFactory(new CountNullConnectionFactory(1, true));
            pool = new FastConnectionPool();
            pool.init(config);
        }catch(SQLException e){
            Assert.assertTrue(e.getMessage().contains("The count of creation has reach max size"));
            Assert.assertEquals(0,pool.getIdleSize());
        }

        FastConnectionPool pool2;
        StoneLogAppender logAppender = getStoneLogAppender();
        BeeDataSourceConfig config2 = DsConfigFactory.createDefault();
        config2.setInitialSize(2);
        config2.setRawConnectionFactory(new CountNullConnectionFactory(1, true));
        config2.setAsyncCreateInitConnection(true);
        pool2 = new FastConnectionPool();

        logAppender.beginCollectStoneLog();
        pool2.init(config2);
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.isEmpty());
    }

    public void testTimeoutOnCreateLock() throws Exception {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLog(true);
        config.setAsyncCreateInitConnection(true);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        pool.close();
    }
}
