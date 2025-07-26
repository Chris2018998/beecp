/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.pool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.beecp.pool.ProxyBaseWrapper;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;
import org.stone.test.base.TestUtil;
import org.stone.test.beecp.driver.MockConnectionProperties;
import org.stone.test.beecp.objects.BorrowThread;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;
import org.stone.test.beecp.objects.MockDriverConnectionFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.stone.beecp.pool.ConnectionPoolStatics.CON_IDLE;
import static org.stone.test.base.TestUtil.waitUtilWaiting;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0061PoolTransferTest {
    private final Class PooledConnectionClass = Class.forName("org.stone.beecp.pool.PooledConnection");

    public Tc0061PoolTransferTest() throws ClassNotFoundException {
    }

    @Test
    public void testTransferConnection() throws Exception {
        //1: enable thread local
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(3L));
        config.setConnectionFactory(new MockDriverConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);


        Method recycleMethod = FastConnectionPool.class.getDeclaredMethod("recycle", PooledConnectionClass);
        recycleMethod.setAccessible(true);
        Field pField = ProxyBaseWrapper.class.getDeclaredField("p");
        pField.setAccessible(true);
        try (Connection con = pool.getConnection()) {
            BorrowThread secondBorrowThread = new BorrowThread(pool);
            secondBorrowThread.start();
            if (waitUtilWaiting(secondBorrowThread)) {
                Object p = pField.get(con);
                recycleMethod.invoke(pool, p);//<-- a using connection

                secondBorrowThread.join();
                Assertions.assertNotNull(secondBorrowThread.getConnection());
            }
        }

        //2: disable thread local
        BeeDataSourceConfig config2 = createDefault();
        config2.setMaxActive(1);
        config2.setBorrowSemaphoreSize(2);
        config2.setParkTimeForRetry(0L);
        config.setEnableThreadLocal(false);
        config2.setForceRecycleBorrowedOnClose(true);
        config2.setMaxWait(TimeUnit.SECONDS.toMillis(3L));
        config2.setConnectionFactory(new MockDriverConnectionFactory());
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);

        try (Connection con = pool2.getConnection()) {
            BorrowThread secondBorrowThread = new BorrowThread(pool2);
            secondBorrowThread.start();
            if (waitUtilWaiting(secondBorrowThread)) {
                Object p = pField.get(con);
                recycleMethod.invoke(pool2, p);//<-- a using connection

                secondBorrowThread.join();
                Assertions.assertNotNull(secondBorrowThread.getConnection());
            }
        }
    }

    @Test
    public void testTransferException() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10L));
        config.setConnectionFactory(new MockDriverConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        try (Connection con = pool.getConnection()) {
            Method transferExceptionMethod = FastConnectionPool.class.getDeclaredMethod("transferException", Throwable.class);
            transferExceptionMethod.setAccessible(true);

            BorrowThread secondBorrower = new BorrowThread(pool);
            secondBorrower.start();
            if (waitUtilWaiting(secondBorrower)) {//block 1 second in pool instance creation
                transferExceptionMethod.invoke(pool, new SQLException("Net Error"));
                secondBorrower.join();
                SQLException e = secondBorrower.getFailureCause();
                Assertions.assertTrue(e != null && e.getMessage().contains("Net Error"));
            }

            BorrowThread thirdBorrower = new BorrowThread(pool);
            thirdBorrower.start();
            if (waitUtilWaiting(thirdBorrower)) {//block 1 second in pool instance creation
                transferExceptionMethod.invoke(pool, new Exception("Connection created fail"));
                thirdBorrower.join();
                SQLException e = thirdBorrower.getFailureCause();
                Assertions.assertTrue(e != null && e.getMessage().contains("Connection created fail"));
            }
        }
    }

    @Test
    public void testAliveTestSuccess() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setFairMode(true);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setAliveAssumeTime(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(1L));

        MockConnectionProperties properties = new MockConnectionProperties();
        MockCommonConnectionFactory factory = new MockCommonConnectionFactory(properties);
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: get method by reflection
        Method recycleMethod = FastConnectionPool.class.getDeclaredMethod("recycle", PooledConnectionClass);
        recycleMethod.setAccessible(true);
        Field pField = ProxyBaseWrapper.class.getDeclaredField("p");
        pField.setAccessible(true);

        Connection con = pool.getConnection();
        BorrowThread secondBorrowThread = new BorrowThread(pool);
        secondBorrowThread.start();
        if (waitUtilWaiting(secondBorrowThread)) {
            Object p = pField.get(con);
            //p.lastAccessTime = 0L;
            TestUtil.setFieldValue(p, "lastAccessTime", 0);
            properties.setValid(true);
            recycleMethod.invoke(pool, p);

            secondBorrowThread.join();
            Assertions.assertNotNull(secondBorrowThread.getConnection());
        }
    }

    @Test
    public void testCatchFailAfterTransfer() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setFairMode(true);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(1L));
        config.setConnectionFactory(new MockDriverConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        Method recycleMethod = FastConnectionPool.class.getDeclaredMethod("recycle", PooledConnectionClass);
        recycleMethod.setAccessible(true);
        Field pField = ProxyBaseWrapper.class.getDeclaredField("p");
        pField.setAccessible(true);

        Connection con = pool.getConnection();
        BorrowThread secondBorrowThread = new BorrowThread(pool);
        secondBorrowThread.start();
        if (waitUtilWaiting(secondBorrowThread)) {
            Object p = pField.get(con);
            //p.state = CON_IDLE;
            TestUtil.setFieldValue(p, "state", CON_IDLE);
            recycleMethod.invoke(pool, p);

            secondBorrowThread.join();
            Assertions.assertNull(secondBorrowThread.getConnection());
            Assertions.assertInstanceOf(ConnectionGetTimeoutException.class, secondBorrowThread.getFailureCause());
        }
    }

    @Test
    public void testAliveTestFail() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setFairMode(true);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setAliveAssumeTime(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(1L));

        MockConnectionProperties properties = new MockConnectionProperties();
        MockCommonConnectionFactory factory = new MockCommonConnectionFactory(properties);
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: get method by reflection
        Method recycleMethod = FastConnectionPool.class.getDeclaredMethod("recycle", PooledConnectionClass);
        recycleMethod.setAccessible(true);
        Field pField = ProxyBaseWrapper.class.getDeclaredField("p");
        pField.setAccessible(true);

        Connection con = pool.getConnection();
        BorrowThread secondBorrowThread = new BorrowThread(pool);
        secondBorrowThread.start();
        if (waitUtilWaiting(secondBorrowThread)) {
            Object p = pField.get(con);

            //p.lastAccessTime = 0L;
            TestUtil.setFieldValue(p, "lastAccessTime", 0L);
            properties.setValid(false);
            recycleMethod.invoke(pool, p);

            secondBorrowThread.join();
            Assertions.assertNull(secondBorrowThread.getConnection());
            Assertions.assertInstanceOf(ConnectionGetTimeoutException.class, secondBorrowThread.getFailureCause());
        }
    }
}
