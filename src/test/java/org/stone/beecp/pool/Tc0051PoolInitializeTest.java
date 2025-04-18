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
import org.stone.beecp.driver.MockXaConnectionProperties;
import org.stone.beecp.objects.MockCommonConnectionFactory;
import org.stone.beecp.objects.MockCommonXaConnectionFactory;
import org.stone.beecp.pool.exception.ConnectionCreateException;
import org.stone.beecp.pool.exception.PoolInitializeFailedException;

import java.sql.SQLException;

import static org.stone.base.TestUtil.getStoneLogAppender;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0051PoolInitializeTest extends TestCase {

    public void testNullConfig() throws Exception {
        try {
            new FastConnectionPool().init(null);
            fail("Test failed to check null config");
        } catch (PoolInitializeFailedException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Pool initialization configuration can't be null"));
        }
    }

    public void testConfigurationCheckFail() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(10);
        config.setMaxActive(5);

        try {
            new FastConnectionPool().init(config);
            fail("Test failed on invalid configured items");
        } catch (PoolInitializeFailedException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("The configured value of item 'initial-size' cannot be greater than the configured value of item 'max-active'"));
        }
    }

    public void testInitializeFailOnReadyPool() throws Exception {
        BeeDataSourceConfig config = createDefault();
        FastConnectionPool pool = new FastConnectionPool();

        InitializeThread thread1 = new InitializeThread(pool, config);
        InitializeThread thread2 = new InitializeThread(pool, config);
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        if (thread1.getFailureException() != null)
            Assert.assertEquals("Pool has already initialized or in initializing", thread1.getFailureException().getMessage());
        if (thread2.getFailureException() != null)
            Assert.assertEquals("Pool has already initialized or in initializing", thread2.getFailureException().getMessage());
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

    public void testMockGetNullFromDriver() throws Exception {
        //1:fail to create connections(sync mode)
        try {
            BeeDataSourceConfig config = createDefault();
            config.setInitialSize(1);
            config.setMaxActive(1);
            MockCommonConnectionFactory factory = new MockCommonConnectionFactory();
            factory.setReturnNullOnCreate(true);//<--return null connections from this factory
            config.setConnectionFactory(factory);
            new FastConnectionPool().init(config);
            fail("Failed to test null connection case1");
        } catch (ConnectionCreateException e) {
            Assert.assertTrue(e.getMessage().contains("A unknown error occurred when created a connection"));
        }

        //2:fail to create xa-connections(sync mode)
        try {
            BeeDataSourceConfig config2 = createDefault();
            config2.setInitialSize(1);
            config2.setMaxActive(1);
            MockCommonXaConnectionFactory factory = new MockCommonXaConnectionFactory();
            factory.setReturnNullOnCreate(true);//<--return null xa-connections from this factory
            config2.setXaConnectionFactory(factory);
            new FastConnectionPool().init(config2);
            fail("Failed to test null connection case2");
        } catch (ConnectionCreateException e) {
            Assert.assertTrue(e.getMessage().contains("A unknown error occurred when created an XA connection"));
        }

        //3:failure in async node(async mode)
        BeeDataSourceConfig config3 = createDefault();
        config3.setInitialSize(1);
        config3.setMaxActive(1);
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
        //String logs = logAppender.endCollectedStoneLog();
        //Assert.assertTrue(logs.contains("Failed to create initial connections by async mode"));
    }

    public void testMockCatchSQLExceptionFromDriver() throws Exception {
        MockCommonConnectionFactory factory = new MockCommonConnectionFactory();
        //1: mock communications exception
        try {
            BeeDataSourceConfig config1 = createDefault();
            config1.setInitialSize(1);
            factory.setCreateException1(new SQLException("Communications link failure"));
            config1.setConnectionFactory(factory);
            new FastConnectionPool().init(config1);
            fail("Failed to test exception from factory");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("Communications link failure"));
        }

        //2: exception test(failure at second creation)
        try {
            BeeDataSourceConfig config2 = createDefault();
            config2.setInitialSize(1);
            MockCommonXaConnectionFactory xaFactory = new MockCommonXaConnectionFactory();
            xaFactory.setCreateException1(new SQLException("Communications link failure"));
            config2.setXaConnectionFactory(xaFactory);
            new FastConnectionPool().init(config2);
            fail("Failed to test exception from factory");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("Communications link failure"));
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
        //String logs = logAppender.endCollectedStoneLog();
        //Assert.assertTrue(logs.contains("Failed to create initial connections by async mode"));
    }

    public void testMockCatchRuntimeExceptionFromDriver() throws Exception {
        //1: mock RuntimeException exception
        try {
            BeeDataSourceConfig config1 = createDefault();
            config1.setInitialSize(1);
            MockCommonConnectionFactory factory = new MockCommonConnectionFactory();
            factory.setCreateException2(new RuntimeException("A runtime exception"));
            config1.setConnectionFactory(factory);
            new FastConnectionPool().init(config1);
            fail("Failed to test exception from factory");
        } catch (ConnectionCreateException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof RuntimeException);
            Assert.assertTrue(cause.getMessage().contains("A runtime exception"));
        }

        //2: exception test(failure at second creation)
        try {
            BeeDataSourceConfig config2 = createDefault();
            config2.setInitialSize(1);
            MockCommonXaConnectionFactory xaFactory = new MockCommonXaConnectionFactory();
            xaFactory.setCreateException2(new RuntimeException("A runtime exception"));
            config2.setXaConnectionFactory(xaFactory);
            new FastConnectionPool().init(config2);
            fail("Failed to test exception from factory");
        } catch (SQLException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof RuntimeException);
            Assert.assertTrue(cause.getMessage().contains("A runtime exception"));
        }
    }

    public void testMockCatchErrorFromDriver() throws Exception {
        //1: mock catch error
        try {
            BeeDataSourceConfig config1 = createDefault();
            config1.setInitialSize(1);
            MockCommonConnectionFactory factory = new MockCommonConnectionFactory();
            factory.setCreateException3(new Error("Unknown error"));
            config1.setConnectionFactory(factory);
            new FastConnectionPool().init(config1);
            fail("Failed to test exception from factory");
        } catch (ConnectionCreateException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof Error);
            Assert.assertTrue(cause.getMessage().contains("Unknown error"));
        }

        //2: mock catch error
        try {
            BeeDataSourceConfig config2 = createDefault();
            config2.setInitialSize(1);
            MockCommonXaConnectionFactory xaFactory = new MockCommonXaConnectionFactory();
            xaFactory.setCreateException3(new Error("Unknown error"));
            config2.setXaConnectionFactory(xaFactory);
            new FastConnectionPool().init(config2);
            fail("Failed to test exception from factory");
        } catch (SQLException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof Error);
            Assert.assertTrue(cause.getMessage().contains("Unknown error"));
        }
    }

    public void testMockPartlySuccess() {
        try {
            BeeDataSourceConfig config1 = createDefault();
            config1.setInitialSize(3);
            config1.setMaxActive(3);
            MockCommonConnectionFactory factory = new MockCommonConnectionFactory();
            factory.setMaxCreationSize(2);
            config1.setConnectionFactory(factory);
            new FastConnectionPool().init(config1);
            fail("Failed to test exception from factory");
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof ConnectionCreateException);
            Assert.assertEquals("java.sql.SQLException: the count of created connections has reached max", e.getMessage());
        }

        try {
            BeeDataSourceConfig config2 = createDefault();
            config2.setInitialSize(3);
            config2.setMaxActive(3);
            MockCommonXaConnectionFactory xaFactory = new MockCommonXaConnectionFactory();
            xaFactory.setMaxCreationSize(2);
            config2.setXaConnectionFactory(xaFactory);
            new FastConnectionPool().init(config2);
            fail("Failed to test exception from factory");
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof ConnectionCreateException);
            Assert.assertEquals("java.sql.SQLException: the count of created connections has reached max", e.getMessage());
        }
    }

    public void testExceptionFromXaConnection() {
        try {
            BeeDataSourceConfig config1 = createDefault();
            config1.setInitialSize(1);
            config1.setMaxActive(1);

            MockXaConnectionProperties properties = new MockXaConnectionProperties();
            properties.enableExceptionOnMethod("getConnection");
            properties.setMockException1(new SQLException("Communications link failure"));
            MockCommonXaConnectionFactory xaFactory = new MockCommonXaConnectionFactory(properties);
            config1.setXaConnectionFactory(xaFactory);
            new FastConnectionPool().init(config1);
            fail("Failed to test exception from XaConnection");
        } catch (SQLException e) {
            // e.printStackTrace();
            Assert.assertTrue(e.getMessage().contains("Communications link failure"));
        }

        try {
            BeeDataSourceConfig config2 = createDefault();
            config2.setInitialSize(1);
            config2.setMaxActive(1);
            MockXaConnectionProperties properties = new MockXaConnectionProperties();
            properties.enableExceptionOnMethod("getXAResource");
            properties.setMockException1(new SQLException("Communications link failure"));
            MockCommonXaConnectionFactory xaFactory = new MockCommonXaConnectionFactory(properties);
            config2.setXaConnectionFactory(xaFactory);
            new FastConnectionPool().init(config2);
            fail("Failed to test exception from XaConnection");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("Communications link failure"));
        }
    }


    private static class InitializeThread extends Thread {
        private final FastConnectionPool pool;
        private final BeeDataSourceConfig config;
        private Exception failureException;

        public InitializeThread(FastConnectionPool pool, BeeDataSourceConfig config) {
            this.pool = pool;
            this.config = config;
        }

        public void run() {
            try {
                pool.init(config);
            } catch (Exception e) {
                this.failureException = e;
            }
        }

        public Exception getFailureException() {
            return failureException;
        }
    }
}
