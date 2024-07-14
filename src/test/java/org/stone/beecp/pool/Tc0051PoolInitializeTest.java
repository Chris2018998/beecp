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
import org.stone.beecp.objects.MockCreateNullConnectionFactory;
import org.stone.beecp.objects.MockCreateNullXaConnectionFactory;
import org.stone.beecp.objects.MockFailSizeReachConnectionFactory;
import org.stone.beecp.pool.exception.ConnectionCreateException;
import org.stone.beecp.pool.exception.PoolInitializeFailedException;

import java.sql.SQLException;

import static org.stone.base.TestUtil.getStoneLogAppender;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0051PoolInitializeTest extends TestCase {

    public void testNullConfig() throws Exception {
        FastConnectionPool pool = new FastConnectionPool();
        try {
            pool.init(null);
        } catch (PoolInitializeFailedException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Pool initialization configuration can't be null"));
        }
    }

    public void testCasFailedOnInitialization() throws Exception {
        BeeDataSourceConfig config = createDefault();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        try {
            pool.init(config);
            fail("test failed on Pool initialization cas");
        } catch (PoolInitializeFailedException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Pool has already been initialized or in initializing"));
        }
    }

    public void testFailCreateInitConnections() throws Exception {
        //1:fail to create connections
        FastConnectionPool pool;
        try {
            BeeDataSourceConfig config = createDefault();
            config.setInitialSize(2);
            config.setAsyncCreateInitConnection(false);
            config.setConnectionFactory(new MockCreateNullConnectionFactory());
            pool = new FastConnectionPool();
            pool.init(config);
        } catch (ConnectionCreateException e) {
            Assert.assertTrue(e.getMessage().contains("An internal error occurred in connection factory"));
        }

        //2:fail to create xa-connections
        FastConnectionPool pool2;
        try {
            BeeDataSourceConfig config2 = createDefault();
            config2.setInitialSize(2);
            config2.setXaConnectionFactory(new MockCreateNullXaConnectionFactory());
            pool2 = new FastConnectionPool();
            pool2.init(config2);
        } catch (ConnectionCreateException e) {
            Assert.assertTrue(e.getMessage().contains("An internal error occurred in xa-Connection factory"));
        }

        //3:failure in async node
        BeeDataSourceConfig config3 = createDefault();
        config3.setInitialSize(1);
        config3.setPrintRuntimeLog(true);
        config3.setAsyncCreateInitConnection(true);//<--async
        FastConnectionPool pool3 = new FastConnectionPool();
        pool3.init(config3);
        pool3.close();
    }

    public void testFailCreateInitConnections2() throws Exception {
        FastConnectionPool pool = null;
        try {
            BeeDataSourceConfig config = createDefault();
            config.setInitialSize(2);
            config.setConnectionFactory(new MockFailSizeReachConnectionFactory(1, true));
            pool = new FastConnectionPool();
            pool.init(config);
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("The count of creation has reach max size"));
            Assert.assertEquals(0, pool.getIdleSize());
        }
        pool.close();

        StoneLogAppender logAppender = getStoneLogAppender();
        BeeDataSourceConfig config3 = createDefault();
        config3.setInitialSize(2);
        config3.setConnectionFactory(new MockFailSizeReachConnectionFactory(1, true));
        config3.setAsyncCreateInitConnection(true);
        FastConnectionPool pool2 = new FastConnectionPool();

        logAppender.beginCollectStoneLog();
        pool2.init(config3);
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.isEmpty());
        pool2.close();
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
}
