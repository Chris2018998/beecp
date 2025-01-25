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
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.driver.MockConnectionProperties;
import org.stone.beecp.objects.BorrowThread;
import org.stone.beecp.objects.MockCommonConnectionFactory;
import org.stone.beecp.objects.MockDriverConnectionFactory;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.pool.ConnectionPoolStatics.CON_IDLE;

public class Tc0061PoolTransferTest extends TestCase {

    public void testTransferConnection() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(3L));
        config.setConnectionFactory(new MockDriverConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: get method by reflection
        Method recycleMethod = FastConnectionPool.class.getDeclaredMethod("recycle", PooledConnection.class);
        recycleMethod.setAccessible(true);
        Field pField = ProxyBaseWrapper.class.getDeclaredField("p");
        pField.setAccessible(true);

        //2: create first borrow thread to get connection
        Connection con = pool.getConnection();
        //3: create a thread to get connection
        BorrowThread second = new BorrowThread(pool);
        second.start();
        //4: attempt to get connection in current thread
        TestUtil.blockUtilWaiter((ConcurrentLinkedQueue) TestUtil.getFieldValue(pool, "waitQueue"));

        //5: get PooledConnection from Connection
        PooledConnection p = (PooledConnection) pField.get(con);
        recycleMethod.invoke(pool, p);//<-- a using connection

        //6: transfer a using connection to second thread(cas failed)
        second.join();
        try {
            SQLException e = second.getFailureCause();
            Assert.assertNull(e);
            Assert.assertNotNull(second.getConnection());
        } finally {
            pool.close();
        }

        //disable thread local
        config.setEnableThreadLocal(false);
        pool = new FastConnectionPool();
        pool.init(config);
        con = pool.getConnection();
        second = new BorrowThread(pool);
        second.start();
        TestUtil.blockUtilWaiter((ConcurrentLinkedQueue) TestUtil.getFieldValue(pool, "waitQueue"));
        p = (PooledConnection) pField.get(con);
        recycleMethod.invoke(pool, p);//<-- a using connection
        second.join();
        try {
            SQLException e = second.getFailureCause();
            Assert.assertNull(e);
            Assert.assertNotNull(second.getConnection());
        } finally {
            pool.close();
        }
    }

    public void testTransferSQLException() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10L));
        config.setConnectionFactory(new MockDriverConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: create first borrow thread to get connection
        pool.getConnection();
        //2: create a thread to get connection
        BorrowThread second = new BorrowThread(pool);
        second.start();

        //3: attempt to get connection in current thread
        TestUtil.blockUtilWaiter((ConcurrentLinkedQueue) TestUtil.getFieldValue(pool, "waitQueue"));

        //4: get method by reflection
        Method transferExceptionMethod = FastConnectionPool.class.getDeclaredMethod("transferException", Throwable.class);
        transferExceptionMethod.setAccessible(true);

        //5: invoke method to transfer an exception
        transferExceptionMethod.invoke(pool, new SQLException("Net Error"));

        //6: get failure exception from second
        second.join();
        try {
            SQLException e = second.getFailureCause();
            Assert.assertTrue(e != null && e.getMessage().contains("Net Error"));
        } finally {
            pool.close();
        }
    }

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

        //1: get method by reflection
        Method recycleMethod = FastConnectionPool.class.getDeclaredMethod("recycle", PooledConnection.class);
        recycleMethod.setAccessible(true);
        Field pField = ProxyBaseWrapper.class.getDeclaredField("p");
        pField.setAccessible(true);

        //2: create first borrow thread to get connection
        Connection con = pool.getConnection();
        //3: create a thread to get connection
        BorrowThread second = new BorrowThread(pool);
        second.start();
        //4: attempt to get connection in current thread
        TestUtil.blockUtilWaiter((ConcurrentLinkedQueue) TestUtil.getFieldValue(pool, "waitQueue"));

        //5: get PooledConnection from Connection
        PooledConnection p = (PooledConnection) pField.get(con);
        p.state = CON_IDLE;
        recycleMethod.invoke(pool, p);//<-- a using connection

        //6: transfer a using connection to second thread(cas failed)
        second.join();
        try {
            SQLException e = second.getFailureCause();
            Assert.assertTrue(e instanceof ConnectionGetTimeoutException);
        } finally {
            pool.close();
        }
    }

    public void testAliveTestAfterTransfer() throws Exception {
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
        Method recycleMethod = FastConnectionPool.class.getDeclaredMethod("recycle", PooledConnection.class);
        recycleMethod.setAccessible(true);
        Field pField = ProxyBaseWrapper.class.getDeclaredField("p");
        pField.setAccessible(true);

        //2: create first borrow thread to get connection
        Connection con = pool.getConnection();
        //3: create a thread to get connection
        BorrowThread second = new BorrowThread(pool);
        second.start();
        //4: attempt to get connection in current thread
        TestUtil.blockUtilWaiter((ConcurrentLinkedQueue) TestUtil.getFieldValue(pool, "waitQueue"));

        //5: get PooledConnection from Connection
        PooledConnection p = (PooledConnection) pField.get(con);
        p.lastAccessTime = 0L;
        properties.setValid(false);
        recycleMethod.invoke(pool, p);//<-- a using connection

        //6: transfer a using connection to second thread(cas failed)
        second.join();
        try {
            SQLException e = second.getFailureCause();
            Assert.assertTrue(e instanceof ConnectionGetTimeoutException);
        } finally {
            pool.close();
        }

        config = createDefault();
        config.setMaxActive(1);
        config.setFairMode(true);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setAliveAssumeTime(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(1L));

        properties = new MockConnectionProperties();
        factory = new MockCommonConnectionFactory(properties);
        config.setConnectionFactory(factory);
        pool = new FastConnectionPool();
        pool.init(config);

        con = pool.getConnection();
        second = new BorrowThread(pool);
        second.start();
        TestUtil.blockUtilWaiter((ConcurrentLinkedQueue) TestUtil.getFieldValue(pool, "waitQueue"));

        //5: get PooledConnection from Connection
        p = (PooledConnection) pField.get(con);
        p.lastAccessTime = 0L;
        properties.setValid(true);
        recycleMethod.invoke(pool, p);//<-- a using connection

        //6: transfer a using connection to second thread(cas failed)
        second.join();
        try {
            Assert.assertNotNull(second.getConnection());
        } finally {
            pool.close();
        }
    }
}
