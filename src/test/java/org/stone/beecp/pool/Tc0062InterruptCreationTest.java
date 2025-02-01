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
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.objects.BorrowThread;
import org.stone.beecp.objects.MockNetBlockConnectionFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.base.TestUtil.waitUtilWaiting;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0062InterruptCreationTest extends TestCase {

    public void testInterruptConnectionCreating1() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.MILLISECONDS.toMillis(500L));
        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: create first thread
        BorrowThread firstBorrower = new BorrowThread(pool);
        firstBorrower.start();

        //2: attempt to get connection in current thread
        if (waitUtilWaiting(firstBorrower)) {//block 1 second in pool instance creation
            BeeConnectionPoolMonitorVo vo = pool.getPoolMonitorVo();
            Assert.assertEquals(1, vo.getCreatingCount());
            Assert.assertEquals(0, vo.getCreatingTimeoutCount());
            pool.interruptConnectionCreating(false);
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toMillis(500L));
            vo = pool.getPoolMonitorVo();
            Assert.assertEquals(0, vo.getCreatingCount());
            Assert.assertEquals(0, vo.getCreatingTimeoutCount());
        }
    }

    public void testInterruptConnectionCreating2() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.MILLISECONDS.toMillis(500L));
        MockNetBlockConnectionFactory factory = new MockNetBlockConnectionFactory();
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: create first thread
        BorrowThread firstBorrower = new BorrowThread(pool);
        firstBorrower.start();

        //2: attempt to get connection in current thread
        if (waitUtilWaiting(firstBorrower)) {//block 1 second in pool instance creation
            BeeConnectionPoolMonitorVo vo = pool.getPoolMonitorVo();
            Assert.assertEquals(1, vo.getCreatingCount());
            Assert.assertEquals(0, vo.getCreatingTimeoutCount());
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));
            vo = pool.getPoolMonitorVo();
            Assert.assertEquals(1, vo.getCreatingCount());
            Assert.assertEquals(1, vo.getCreatingTimeoutCount());
            pool.interruptConnectionCreating(true);
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toMillis(500L));
            vo = pool.getPoolMonitorVo();
            Assert.assertEquals(0, vo.getCreatingCount());
            Assert.assertEquals(0, vo.getCreatingTimeoutCount());
        }
    }
}



