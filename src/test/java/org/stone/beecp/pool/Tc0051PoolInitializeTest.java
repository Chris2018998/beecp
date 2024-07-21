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
import org.stone.beecp.objects.MockCommonConnectionFactory;
import org.stone.beecp.objects.MockCommonXaConnectionFactory;
import org.stone.beecp.pool.exception.ConnectionCreateException;
import org.stone.beecp.pool.exception.PoolInitializeFailedException;

import java.sql.SQLException;

import static org.stone.base.TestUtil.getStoneLogAppender;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0051PoolInitializeTest extends TestCase {

    public void testNullConfigCheck() throws Exception {
        try {
            new FastConnectionPool().init(null);
            fail("Test failed to check null config");
        } catch (PoolInitializeFailedException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Pool initialization configuration can't be null"));
        }
    }

    public void testInitializedCheck() throws Exception {
        BeeDataSourceConfig config = createDefault();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        try {
            pool.init(config);
            fail("Test failed to re-initialize on a completed pool");
        } catch (PoolInitializeFailedException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Pool has already been initialized or in initializing"));
        }
    }

    public void testInvalidConfiguredItemsCheck() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(10);
        config.setMaxActive(5);

        try {
            new FastConnectionPool().init(config);
            fail("Test failed on invalid configured items");
        } catch (PoolInitializeFailedException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("initialSize must not be greater than maxActive"));
        }
    }

    public void testPoolWorkMode() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setFairMode(true);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertEquals("fair", TestUtil.getFieldValue(pool, "poolMode"));
        pool.close();

        BeeDataSourceConfig config2 = createDefault();
        config2.setFairMode(false);
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        Assert.assertEquals("compete", TestUtil.getFieldValue(pool2, "poolMode"));
        pool2.close();
    }

    public void testNullConnectionFromFactory() throws Exception {
        //1:fail to create connections(sync mode)
        try {
            BeeDataSourceConfig config = createDefault();
            config.setInitialSize(2);
            config.setMaxActive(2);
            MockCommonConnectionFactory factory = new MockCommonConnectionFactory();
            factory.setReturnNullOnCreate(true);//<--return null connections from this factory
            config.setConnectionFactory(factory);
            new FastConnectionPool().init(config);
            fail("Failed to test null connection case1");
        } catch (ConnectionCreateException e) {
            Assert.assertTrue(e.getMessage().contains("An internal error occurred in connection factory"));
        }

        //2:fail to create xa-connections(sync mode)
        try {
            BeeDataSourceConfig config2 = createDefault();
            config2.setInitialSize(2);
            config2.setMaxActive(2);
            MockCommonXaConnectionFactory factory = new MockCommonXaConnectionFactory();
            factory.setReturnNullOnCreate(true);//<--return null xa-connections from this factory
            config2.setXaConnectionFactory(factory);
            new FastConnectionPool().init(config2);
            fail("Failed to test null connection case2");
        } catch (ConnectionCreateException e) {
            Assert.assertTrue(e.getMessage().contains("An internal error occurred in xa-Connection factory"));
        }

        //3:failure in async node(async mode)
        BeeDataSourceConfig config3 = createDefault();
        config3.setInitialSize(2);
        config3.setMaxActive(2);
        config3.setPrintRuntimeLog(true);
        config3.setAsyncCreateInitConnection(true);//<--async mode
        MockCommonConnectionFactory factory = new MockCommonConnectionFactory();
        factory.setReturnNullOnCreate(true);//<--return null connections from this factory
        config3.setConnectionFactory(factory);
        FastConnectionPool pool3 = new FastConnectionPool();

        //begin to collect runtime logs in pool
        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        pool3.init(config3);
        pool3.close();
        String logs = logAppender.endCollectedStoneLog();
        //Assert.assertTrue(logs.contains("Failed to create initial connections by async mode"));
    }

    public void testExceptionFromFactory() throws Exception {
        //1: mock communications exception
        try {
            BeeDataSourceConfig config1 = createDefault();
            config1.setInitialSize(1);
            MockCommonConnectionFactory factory = new MockCommonConnectionFactory();
            factory.setCreateException1(new ConnectionCreateException("Communications link failure"));
            config1.setConnectionFactory(factory);
            new FastConnectionPool().init(config1);
            fail("Failed to test exception from factory");
        } catch (ConnectionCreateException e) {
            Assert.assertTrue(e.getMessage().contains("Communications link failure"));
        }

        //2: exception test(failure at second creation)
        MockCommonConnectionFactory factory = new MockCommonConnectionFactory();
        factory.setMaxCreationSize(1);

        FastConnectionPool pool2 = null;
        try {
            BeeDataSourceConfig config2 = createDefault();
            config2.setInitialSize(2);
            config2.setConnectionFactory(factory);
            pool2 = new FastConnectionPool();
            pool2.init(config2);
            fail("Failed to test exception from factory");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("the count of created connections has reached max"));
            Assert.assertEquals(0, pool2.getIdleSize());
        } finally {
            if (pool2 != null) pool2.close();
        }

        //3: exception test(failure at second creation)
        BeeDataSourceConfig config3 = createDefault();
        config3.setInitialSize(2);
        config3.setPrintRuntimeLog(true);
        config3.setAsyncCreateInitConnection(true);//<--async mode
        config3.setConnectionFactory(factory);
        FastConnectionPool pool3 = new FastConnectionPool();

        //begin to collect runtime logs in pool
        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        pool3.init(config3);
        pool3.close();
        String logs = logAppender.endCollectedStoneLog();
        //Assert.assertTrue(logs.contains("Failed to create initial connections by async mode"));
    }
}
