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
import org.stone.beecp.objects.BorrowThread;
import org.stone.beecp.objects.MockDriverConnectionFactory;
import org.stone.beecp.pool.exception.ConnectionGetException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0061PoolTransferTest extends TestCase {

    public void testTransferUsingConnectionUnderCompete() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceCloseUsingOnClear(true);
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
    }

    public void testTransferUsingConnectionUnderFair() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setFairMode(true);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceCloseUsingOnClear(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(2L));
        config.setConnectionFactory(new MockDriverConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: create first borrow thread to get connection
        Connection con = pool.getConnection();
        //2: create a thread to get connection
        BorrowThread second = new BorrowThread(pool);
        second.start();

        //3: attempt to get connection in current thread
        TestUtil.blockUtilWaiter((ConcurrentLinkedQueue) TestUtil.getFieldValue(pool, "waitQueue"));

        //4: get method by reflection
        Method recycleMethod = FastConnectionPool.class.getDeclaredMethod("recycle", PooledConnection.class);
        recycleMethod.setAccessible(true);

        //5: get PooledConnection from Connection
        Field pField = ProxyBaseWrapper.class.getDeclaredField("p");
        pField.setAccessible(true);
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
    }

    public void testTransferSQLException() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceCloseUsingOnClear(true);
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

    public void testTransferAnException() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceCloseUsingOnClear(true);
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
        transferExceptionMethod.invoke(pool, new Exception("Net Error"));

        //6: get failure exception from second
        second.join();
        try {
            SQLException e = second.getFailureCause();
            Assert.assertTrue(e instanceof ConnectionGetException);
            Assert.assertTrue(e != null && e.getCause().getMessage().contains("Net Error"));
        } finally {
            pool.close();
        }
    }
}
