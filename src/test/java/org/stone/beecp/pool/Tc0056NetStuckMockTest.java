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
import org.stone.beecp.objects.InterruptionAction;
import org.stone.beecp.objects.MockNetBlockConnectionFactory;
import org.stone.beecp.objects.MockNetBlockXaConnectionFactory;
import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;

import java.sql.SQLException;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0056NetStuckMockTest extends TestCase {

    public void testStuckInConnectionFactory() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        config.setRawConnectionFactory(new MockNetBlockConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        new InterruptionAction(Thread.currentThread()).start();//mock main thread

        try {
            pool.getConnection();
        } catch (ConnectionGetInterruptedException e) {
            Assert.assertTrue(e.getMessage().contains("An interruption occurred in connection factory"));
        }
        pool.close();
    }

    public void testStuckInXaConnectionFactory() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(2);
        config.setBorrowSemaphoreSize(2);
        config.setRawXaConnectionFactory(new MockNetBlockXaConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        new InterruptionAction(Thread.currentThread()).start();

        try {
            pool.getXAConnection();
        } catch (ConnectionGetInterruptedException e) {
            Assert.assertTrue(e.getMessage().contains("An interruption occurred in xa-connection factory"));
        }
        pool.close();
    }
}
