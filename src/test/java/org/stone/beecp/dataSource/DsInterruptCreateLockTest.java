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
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;
import org.stone.beecp.RawConnectionFactory;
import org.stone.beecp.pool.ConnectionPoolStatics;
import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class DsInterruptCreateLockTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setRawConnectionFactory(new BlockingConnectionFactory());
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testInterruptCreationLock() throws SQLException {
        new MockThreadToInterruptBlock(ds).start();

        Connection con = null;
        try {
            con = ds.getConnection();
        } catch (SQLException e) {
            if (!(e instanceof ConnectionGetInterruptedException))
                throw e;
        } finally {
            if (con != null) ConnectionPoolStatics.oclose(con);
        }
    }

    class BlockingConnectionFactory implements RawConnectionFactory {
        public Connection create() {
            LockSupport.park();
            return null;
        }
    }

    class MockThreadToInterruptBlock extends Thread {
        private BeeDataSource ds;

        MockThreadToInterruptBlock(BeeDataSource ds) {
            this.ds = ds;
        }

        public void run() {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            try {
                long holdTimeMillsOnLock = ds.getElapsedTimeSinceCreationLock();
                if (holdTimeMillsOnLock > 0L) {
                    ds.interruptThreadsOnCreationLock();
                }
            } catch (SQLException e) {
                //do nothing
            }
        }
    }
}
