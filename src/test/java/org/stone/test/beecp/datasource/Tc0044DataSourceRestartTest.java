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
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.pool.exception.PoolInClearingException;
import org.stone.test.beecp.objects.threads.TimeDelayCloseConnectionThread;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0044DataSourceRestartTest {

    @Test
    public void testClear() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(5);
        config.setMaxActive(10);
        config.setParkTimeForRetry(1L);
        config.addSqlExceptionCode(100);
        config.addSqlExceptionState("B100");
        config.setForceRecycleBorrowedOnClose(false);//configuration is not force

        //1: force close borrowed connection
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Connection con = ds.getConnection();//not close it
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(1, vo.getBorrowedSize());
            Assertions.assertEquals(4, vo.getIdleSize());

            //concurrent clear
            ClearThread thread1 = new ClearThread(ds);
            ClearThread thread2 = new ClearThread(ds);
            long concurrentTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500L);
            thread1.setConcurrentTime(concurrentTime);
            thread2.setConcurrentTime(concurrentTime);
            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            if (thread1.getFailException() != null) {
                Assertions.assertInstanceOf(PoolInClearingException.class, thread1.getFailException());
                Assertions.assertEquals("Pool has been closed or is restarting", thread1.getFailException().getMessage());
            }
            if (thread2.getFailException() != null) {
                Assertions.assertInstanceOf(PoolInClearingException.class, thread2.getFailException());
                Assertions.assertEquals("Pool has been closed or is restarting", thread2.getFailException().getMessage());
            }

            vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(0, vo.getBorrowedSize());
            Assertions.assertEquals(0, vo.getIdleSize());
        }

        //2: wait borrowed connections released to pool
        config.setEnableMethodExecutionLogCache(true);
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Connection con = ds.getConnection();//not close it
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(1, vo.getBorrowedSize());
            new TimeDelayCloseConnectionThread(con, Long.valueOf(System.currentTimeMillis() + 500L)).start();

            //2.1: wait borrowed connections return to pool
            ds.restart(false);//not force
            vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(0, vo.getBorrowedSize());

            try {
                ds.restart(true, null);
                Assertions.fail("[testReInitialize]test fail");
            } catch (BeeDataSourceConfigException e) {
                Assertions.assertEquals("Pool reinitialization configuration can't be null", e.getMessage());
            }

            //3: clear with a new configuration
            BeeDataSourceConfig newConfig = createDefault();
            newConfig.setInitialSize(10);
            newConfig.setMaxActive(20);
            newConfig.addSqlExceptionCode(200);
            newConfig.addSqlExceptionState("B200");
            ds.restart(true, newConfig);
            vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(0, vo.getBorrowedSize());
            Assertions.assertEquals(10, vo.getIdleSize());
            Assertions.assertEquals(20, ds.getMaxActive());

            List<Integer> sqlExceptionCodeList = ds.getSqlExceptionCodeList();
            List<String> sqlExceptionStateList = ds.getSqlExceptionStateList();
            Assertions.assertNotNull(sqlExceptionCodeList);
            Assertions.assertNotNull(sqlExceptionStateList);
            Assertions.assertEquals(1, sqlExceptionCodeList.size());
            Assertions.assertEquals(1, sqlExceptionStateList.size());

            Assertions.assertTrue(sqlExceptionCodeList.contains(200));
            Assertions.assertTrue(sqlExceptionStateList.contains("B200"));
        }
    }


    private static class ClearThread extends Thread {
        private final BeeDataSource ds;
        private long concurrentTime;
        private Exception failException;

        public ClearThread(BeeDataSource ds) {
            this.ds = ds;
        }

        public Exception getFailException() {
            return failException;
        }

        public void setConcurrentTime(long concurrentTime) {
            this.concurrentTime = concurrentTime;
        }

        public void run() {
            LockSupport.parkNanos(concurrentTime - System.nanoTime());
            try {
                ds.restart(true);
            } catch (Exception e) {
                this.failException = e;
            }
        }
    }
}
