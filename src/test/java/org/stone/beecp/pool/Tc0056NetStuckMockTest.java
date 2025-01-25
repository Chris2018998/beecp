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
import org.stone.beecp.objects.*;
import org.stone.beecp.pool.exception.ConnectionCreateException;
import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;

import java.sql.SQLException;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0056NetStuckMockTest extends TestCase {

    public void testStuckInConnectionFactory() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        new InterruptionAction(Thread.currentThread()).start();//mock main thread

        try {
            pool.getConnection();
        } catch (ConnectionCreateException e) {
            Assert.assertTrue(e.getMessage().contains("A unknown error occurred when created a connection"));
        } finally {
            factory.interruptAll();
            pool.close();
        }


        BeeDataSourceConfig config2 = createDefault();
        config2.setMaxActive(2);
        config2.setBorrowSemaphoreSize(2);
        MockNetBlockConnectionFactory2 factory2 = new MockNetBlockConnectionFactory2();
        config.setConnectionFactory(factory2);
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config);
        new InterruptionAction(Thread.currentThread()).start();//mock main thread

        try {
            pool2.getConnection();
        } catch (ConnectionGetInterruptedException e) {
            Assert.assertTrue(e.getMessage().contains("An interruption occurred when created a connection"));
        } finally {
            pool.close();
        }
    }

    public void testStuckInXaConnectionFactory() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        MockNetBlockXaConnectionFactory factory = new MockNetBlockXaConnectionFactory();
        config.setXaConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        new InterruptionAction(Thread.currentThread()).start();

        try {
            pool.getXAConnection();
        } catch (ConnectionCreateException e) {
            Assert.assertTrue(e.getMessage().contains("A unknown error occurred when created an XA connection"));
        } finally {
            factory.getBlockingLatch().countDown();
            pool.close();
        }

        BeeDataSourceConfig config2 = createDefault();
        config2.setMaxActive(2);
        config2.setBorrowSemaphoreSize(2);
        MockNetBlockXaConnectionFactory2 factory2 = new MockNetBlockXaConnectionFactory2();
        config.setXaConnectionFactory(factory2);
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config);
        new InterruptionAction(Thread.currentThread()).start();

        try {
            pool2.getXAConnection();
        } catch (ConnectionGetInterruptedException e) {
            Assert.assertTrue(e.getMessage().contains("An interruption occurred when created an XA connection"));
        } finally {
            pool.close();
        }
    }
}
