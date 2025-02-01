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
import org.stone.beecp.objects.BorrowThread;
import org.stone.beecp.objects.MockNetBlockConnectionFactory;
import org.stone.beecp.objects.MockNetBlockXaConnectionFactory;

import static org.stone.base.TestUtil.waitUtilWaiting;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0056NetStuckMockTest extends TestCase {

    public void testStuckInConnectionFactory() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        BorrowThread firstBorrower = new BorrowThread(pool);
        firstBorrower.start();
        if (waitUtilWaiting(firstBorrower)) {
            factory.unparkToExit(firstBorrower);
            firstBorrower.join();
            Assert.assertTrue(firstBorrower.getFailureCause().getMessage().contains("A unknown error occurred when created a connection"));
        }


        BeeDataSourceConfig config2 = createDefault();
        config2.setMaxActive(2);
        config2.setBorrowSemaphoreSize(2);
        MockNetBlockConnectionFactory factory2 = new MockNetBlockConnectionFactory();
        config2.setConnectionFactory(factory2);
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        BorrowThread secondBorrower = new BorrowThread(pool2);
        secondBorrower.start();
        if (waitUtilWaiting(secondBorrower)) {
            secondBorrower.interrupt();

            secondBorrower.join();
            Assert.assertTrue(secondBorrower.getFailureCause().getMessage().contains("An interruption occurred when created a connection"));
        }
    }

    public void testStuckInXaConnectionFactory() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        MockNetBlockXaConnectionFactory factory = new MockNetBlockXaConnectionFactory();
        config.setXaConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        BorrowThread firstBorrower = new BorrowThread(null, pool, true);
        firstBorrower.start();
        if (waitUtilWaiting(firstBorrower)) {
            factory.unparkToExit(firstBorrower);
            firstBorrower.join();
            Assert.assertTrue(firstBorrower.getFailureCause().getMessage().contains("A unknown error occurred when created an XA connection"));
        }


        BeeDataSourceConfig config2 = createDefault();
        config2.setMaxActive(2);
        config2.setBorrowSemaphoreSize(2);
        MockNetBlockXaConnectionFactory factory2 = new MockNetBlockXaConnectionFactory();
        config2.setXaConnectionFactory(factory2);
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        BorrowThread secondBorrower = new BorrowThread(null, pool2, true);
        secondBorrower.start();
        if (waitUtilWaiting(secondBorrower)) {
            secondBorrower.interrupt();

            secondBorrower.join();
            Assert.assertTrue(secondBorrower.getFailureCause().getMessage().contains("An interruption occurred when created an XA connection"));
        }
    }
}
