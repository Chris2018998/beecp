/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.test.beecp.objects.factory.BlockingMockConnectionFactory;
import org.stone.test.beecp.objects.threads.BorrowThread;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.test.base.TestUtil.waitUtilWaiting;

/**
 * @author Chris Liao
 */
public class Tc0070ConnectionCreateBlockingTest {

    @Test
    public void testInterruptConnectionCreating1() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setInitialSize(0);
        config.setMaxActive(1);
        config.setSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.MILLISECONDS.toMillis(500L));
        BlockingMockConnectionFactory factory = new BlockingMockConnectionFactory();
        config.setConnectionFactory(factory);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            //1: create first thread
            BorrowThread firstBorrower = new BorrowThread(ds);
            firstBorrower.start();

            //2: attempt to get connection in current thread
            if (waitUtilWaiting(firstBorrower)) {//block 1 second in pool instance creation
                BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
                Assertions.assertEquals(1, vo.getCreatingSize());
                Assertions.assertEquals(0, vo.getCreatingTimeoutSize());

                ds.interruptWaitingThreads();
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toMillis(500L));
                vo = ds.getPoolMonitorVo();
                Assertions.assertEquals(0, vo.getCreatingSize());
                Assertions.assertEquals(0, vo.getCreatingTimeoutSize());
            }
        }
    }

    @Test
    public void testInterruptConnectionCreating2() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setInitialSize(0);
        config.setMaxActive(1);
        config.setSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.MILLISECONDS.toMillis(500L));
        BlockingMockConnectionFactory factory = new BlockingMockConnectionFactory();
        config.setConnectionFactory(factory);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            //1: create first thread
            BorrowThread firstBorrower = new BorrowThread(ds);
            firstBorrower.start();

            //2: attempt to get connection in current thread
            if (waitUtilWaiting(firstBorrower)) {//block 1 second in pool instance creation
                BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
                Assertions.assertEquals(1, vo.getCreatingSize());
                Assertions.assertEquals(0, vo.getCreatingTimeoutSize());
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));
                vo = ds.getPoolMonitorVo();
                Assertions.assertEquals(1, vo.getCreatingSize());
                Assertions.assertEquals(1, vo.getCreatingTimeoutSize());
                ds.interruptWaitingThreads();
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toMillis(500L));
                vo = ds.getPoolMonitorVo();
                Assertions.assertEquals(0, vo.getCreatingSize());
                Assertions.assertEquals(0, vo.getCreatingTimeoutSize());
            }
        }
    }
}
