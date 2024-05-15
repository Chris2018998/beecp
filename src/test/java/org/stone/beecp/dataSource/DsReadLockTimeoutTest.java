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
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.JdbcConfig;
import org.stone.beecp.pool.ConnectionPoolStatics;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class DsReadLockTimeoutTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        ds = new BeeDataSource();
        ds.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        ds.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        ds.setUsername(JdbcConfig.JDBC_USER);
        ds.setMaxWait(TimeUnit.SECONDS.toMillis(3));
        ds.setPoolImplementClassName("org.stone.beecp.dataSource.DummyPoolImplementation");
    }

    public void tearDown() {
        ds.close();
    }

    public void testReadLockTimeout() throws Exception {
        MockThreadToHoldWriteLock mockThread = new MockThreadToHoldWriteLock(ds);
        mockThread.start();

        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        Connection con = null;
        try {
            con = ds.getConnection();//blocking in pool instance creation
        } catch (SQLException e) {
            Assert.assertTrue(e instanceof ConnectionGetTimeoutException);
//            if (!(e instanceof ConnectionGetTimeoutException)) {
//                throw new TestException();
//            }
        } finally {
            if (con != null) ConnectionPoolStatics.oclose(con);
        }

        mockThread.interrupt();
    }

    static class MockThreadToHoldWriteLock extends Thread {
        private final BeeDataSource ds;

        MockThreadToHoldWriteLock(BeeDataSource ds) {
            this.ds = ds;
        }

        public void run() {
            try {
                ds.getConnection();
            } catch (Exception e) {
                //do noting
            }
        }
    }
}
