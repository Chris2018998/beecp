/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeMethodLog;
import org.stone.beecp.exception.BeeDataSourcePoolHasClosedException;
import org.stone.test.beecp.objects.listener.MockMethodExecutionListener1;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0043DataSourceCloseTest {

    @Test
    public void testDatasourceClose() throws Exception {
        String poolClosedDesc = "Pool has been closed";
        String poolClosedExceptionMsg = "No operations allowed on closed pool";

        BeeDataSource ds = new BeeDataSource(createDefault());
        Assertions.assertFalse(ds.isClosed());
        long targetTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(1L);
        DsCloseThread thread1 = new DsCloseThread(ds, targetTime);
        DsCloseThread thread2 = new DsCloseThread(ds, targetTime);
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        Assertions.assertTrue(ds.isClosed());
        Assertions.assertEquals(poolClosedDesc, ds.toString());

        try {
            ds.suspend();
            Assertions.fail("[testDatasourceClose]test failed");
        } catch (SQLException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolHasClosedException.class, e);
            Assertions.assertEquals(poolClosedExceptionMsg, e.getMessage());
        }

        try {
            ds.resume();
            Assertions.fail("[testDatasourceClose]test failed");
        } catch (SQLException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolHasClosedException.class, e);
            Assertions.assertEquals(poolClosedExceptionMsg, e.getMessage());
        }

        try {
            ds.restart(true);
            Assertions.fail("[testDatasourceClose]test failed");
        } catch (SQLException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolHasClosedException.class, e);
            Assertions.assertEquals(poolClosedExceptionMsg, e.getMessage());
        }

        try {
            ds.restart(true, new BeeDataSourceConfig());
            Assertions.fail("[testDatasourceClose]test failed");
        } catch (SQLException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolHasClosedException.class, e);
            Assertions.assertEquals(poolClosedExceptionMsg, e.getMessage());
        }

        try {
            ds.enableLogPrinter(false);
            Assertions.fail("[testDatasourceClose]test failed");
        } catch (SQLException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolHasClosedException.class, e);
            Assertions.assertEquals(poolClosedExceptionMsg, e.getMessage());
        }

        try {
            ds.enableLogCache(false);
            Assertions.fail("[testDatasourceClose]test failed");
        } catch (SQLException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolHasClosedException.class, e);
            Assertions.assertEquals(poolClosedExceptionMsg, e.getMessage());
        }

        try {
            ds.changeLogListener(new MockMethodExecutionListener1());
            Assertions.fail("[testDatasourceClose]test failed");
        } catch (SQLException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolHasClosedException.class, e);
            Assertions.assertEquals(poolClosedExceptionMsg, e.getMessage());
        }

        try {
            ds.getLogs(BeeMethodLog.Type_Pool_Log);
            Assertions.fail("[testDatasourceClose]test failed");
        } catch (SQLException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolHasClosedException.class, e);
            Assertions.assertEquals(poolClosedExceptionMsg, e.getMessage());
        }

        try {
            ds.clearLogs(BeeMethodLog.Type_Pool_Log);
            Assertions.fail("[testDatasourceClose]test failed");
        } catch (SQLException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolHasClosedException.class, e);
            Assertions.assertEquals(poolClosedExceptionMsg, e.getMessage());
        }

        try {
            ds.clearLogs(BeeMethodLog.Type_Pool_Log);
            Assertions.fail("[testDatasourceClose]test failed");
        } catch (SQLException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolHasClosedException.class, e);
            Assertions.assertEquals(poolClosedExceptionMsg, e.getMessage());
        }

        try {
            ds.cancelStatement(null);
            Assertions.fail("[testDatasourceClose]test failed");
        } catch (SQLException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolHasClosedException.class, e);
            Assertions.assertEquals(poolClosedExceptionMsg, e.getMessage());
        }

        try {
            ds.enableLogPrinter(true);
            Assertions.fail("[testDatasourceClose]test failed");
        } catch (SQLException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolHasClosedException.class, e);
            Assertions.assertEquals(poolClosedExceptionMsg, e.getMessage());
        }

        try {
            ds.getConnection();
            Assertions.fail("[testDatasourceClose]test failed");
        } catch (SQLException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolHasClosedException.class, e);
            Assertions.assertEquals(poolClosedExceptionMsg, e.getMessage());
        }

        try {
            ds.getConnection("test", "test");
            Assertions.fail("[testDatasourceClose]test failed");
        } catch (SQLException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolHasClosedException.class, e);
            Assertions.assertEquals(poolClosedExceptionMsg, e.getMessage());
        }

        try {
            ds.getXAConnection();
            Assertions.fail("[testDatasourceClose]test failed");
        } catch (SQLException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolHasClosedException.class, e);
            Assertions.assertEquals(poolClosedExceptionMsg, e.getMessage());
        }

        try {
            ds.getXAConnection("test", "test");
            Assertions.fail("[testDatasourceClose]test failed");
        } catch (SQLException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolHasClosedException.class, e);
            Assertions.assertEquals(poolClosedExceptionMsg, e.getMessage());
        }
    }

    private static class DsCloseThread extends Thread {
        private final BeeDataSource ds;
        private final long delayToTime;

        public DsCloseThread(BeeDataSource ds, long delayToTime) {
            this.ds = ds;
            this.delayToTime = delayToTime;
        }

        public void run() {
            LockSupport.parkNanos(System.nanoTime() - delayToTime);
            ds.close();
        }
    }
}
