/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.datasource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.StoneLogAppender;
import org.stone.base.TestUtil;
import org.stone.beecp.*;
import org.stone.beecp.objects.MockCommonConnectionFactory;
import org.stone.beecp.pool.ConnectionPoolStatics;
import org.stone.beecp.pool.exception.PoolCreateFailedException;
import org.stone.beecp.pool.exception.PoolInitializeFailedException;
import org.stone.beecp.pool.exception.PoolNotCreatedException;

import javax.sql.XAConnection;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.stone.beecp.config.DsConfigFactory.*;

/**
 * @author Chris Liao
 */

public class Tc0031DataSourcePoolTest extends TestCase {

    public void testOnInitializedPool() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        //config.setCreateTimeout(5);
        config.setPrintConfigInfo(false);
        BeeDataSource ds = null;

        try {
            ds = new BeeDataSource(config);
            StoneLogAppender logAppender = TestUtil.getStoneLogAppender();
            logAppender.beginCollectStoneLog();

            //getConnection test
            Connection con1 = ds.getConnection();
            Assert.assertNotNull(con1);
            con1.close();
            con1 = ds.getConnection(JDBC_USER, JDBC_PASSWORD);
            Assert.assertNotNull(con1);
            con1.close();

            //getXAConnection test
            XAConnection xCon1 = ds.getXAConnection();
            Assert.assertNotNull(xCon1);
            xCon1.close();
            xCon1 = ds.getXAConnection(JDBC_USER, JDBC_PASSWORD);
            Assert.assertNotNull(xCon1);
            xCon1.close();

            String logs = logAppender.endCollectedStoneLog();
            Assert.assertTrue(logs.contains("getConnection (user,password) ignores authentication"));
            Assert.assertTrue(logs.contains("getXAConnection (user,password) ignores authentication"));

            //test on methods of commonDataSource
            Assert.assertNull(ds.getParentLogger());
            Assert.assertNull(ds.getLogWriter());
            ds.setLogWriter(new PrintWriter(System.out));
            Assert.assertNotNull(ds.getLogWriter());
            ds.setLogWriter(null);//reset back to null
            Assert.assertNull(ds.getLogWriter());
            DriverManager.setLoginTimeout(0);
            Assert.assertEquals(0, ds.getLoginTimeout());
            Assert.assertEquals(0, DriverManager.getLoginTimeout());
            ds.setLoginTimeout(10);//ten seconds
            Assert.assertEquals(10, ds.getLoginTimeout());
            Assert.assertEquals(10, DriverManager.getLoginTimeout());

            //test on printRuntimeLog ind
            BeeConnectionPool pool = (BeeConnectionPool) TestUtil.getFieldValue(ds, "pool");
            Boolean printRuntimeLogInd = (Boolean) TestUtil.getFieldValue(pool, "printRuntimeLog");
            Assert.assertFalse(printRuntimeLogInd);
            ds.setPrintRuntimeLog(true);
            printRuntimeLogInd = (Boolean) TestUtil.getFieldValue(pool, "printRuntimeLog");
            Assert.assertTrue(printRuntimeLogInd);

            //test poolMonitorVo
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assert.assertNotNull(vo);
            Assert.assertEquals(1, vo.getIdleSize());

            //test on pool built-in lock
            Assert.assertEquals(0, ds.getCreatingTime());
            Assert.assertFalse(ds.isCreatingTimeout());
            Thread[] interruptedThreads = ds.interruptOnCreation();
            Assert.assertNotNull(interruptedThreads);
            Assert.assertEquals(0, interruptedThreads.length);

            //maxWait
            ds.setMaxWait(0L);
            Assert.assertNotEquals(0L, ds.getMaxWait());//not changed
            long newMaxWaitMillis2 = TimeUnit.SECONDS.toMillis(20);
            ds.setMaxWait(newMaxWaitMillis2);
            Assert.assertEquals(newMaxWaitMillis2, ds.getMaxWait());//changed

        } finally {
            if (ds != null && !ds.isClosed()) {
                ds.close();
                Assert.assertTrue(ds.isClosed());
            }
        }
    }


    public void testOnUninitializedPool() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        BeeConnectionPool pool = (BeeConnectionPool) TestUtil.getFieldValue(ds, "pool");
        Assert.assertNull(pool);
        Assert.assertTrue(ds.isClosed());

        //test on methods of commonDataSource
        Assert.assertNull(ds.getParentLogger());
        Assert.assertNull(ds.getLogWriter());
        ds.setLogWriter(new PrintWriter(System.out));
        Assert.assertNull(ds.getLogWriter());
        Assert.assertEquals(0, ds.getLoginTimeout());
        Assert.assertEquals(0, DriverManager.getLoginTimeout());
        ds.setLoginTimeout(10);//ten seconds
        Assert.assertEquals(0, ds.getLoginTimeout());
        Assert.assertEquals(0, DriverManager.getLoginTimeout());

        ds.setPrintRuntimeLog(true);
        try {
            ds.getPoolMonitorVo();
        } catch (PoolNotCreatedException e) {
            Assert.assertTrue(e.getMessage().contains("Pool not be created"));
        }
        try {
            ds.getCreatingTime();
        } catch (PoolNotCreatedException e) {
            Assert.assertTrue(e.getMessage().contains("Pool not be created"));
        }

        try {
            ds.isCreatingTimeout();
        } catch (PoolNotCreatedException e) {
            Assert.assertTrue(e.getMessage().contains("Pool not be created"));
        }

        try {
            ds.interruptOnCreation();
        } catch (PoolNotCreatedException e) {
            Assert.assertTrue(e.getMessage().contains("Pool not be created"));
        }

        try {
            ds.clear(true);
        } catch (PoolNotCreatedException e) {
            Assert.assertTrue(e.getMessage().contains("Pool not be created"));
        }

        try {
            ds.clear(true, null);
        } catch (PoolNotCreatedException e) {
            Assert.assertTrue(e.getMessage().contains("Pool not be created"));
        }

        BeeDataSourceConfig config = createDefault();
        MockCommonConnectionFactory factory = new MockCommonConnectionFactory();
        factory.setReturnNullOnCreate(true);
        config.setConnectionFactory(factory);
        new BeeDataSource(config);
    }


    public void testPoolCreationUnderLock() throws Exception {
        BeeDataSource ds1 = null;
        try {
            ds1 = new BeeDataSource();
            ds1.setDriverClassName(JDBC_DRIVER);
            ds1.setUrl(JDBC_URL);
            Connection con1 = ds1.getConnection();
            Assert.assertNotNull(con1);
            con1.close();

            BeeDataSource ds2 = new BeeDataSource();
            ds2.setDriverClassName(JDBC_DRIVER);
            ds2.setUrl(JDBC_URL);
            XAConnection con2 = ds2.getXAConnection();
            Assert.assertNotNull(con2);
            con2.close();
        } finally {
            if (ds1 != null) ds1.close();
        }
    }

    public void testPoolClassNotFound() {
        BeeDataSource ds = null;
        Connection con = null;
        try {//lazy creation
            ds = new BeeDataSource(JDBC_DRIVER, JDBC_URL, JDBC_USER, JDBC_PASSWORD);
            ds.setPoolImplementClassName("xx.xx.xx");//invalid pool class name
            con = ds.getConnection();
        } catch (SQLException e) {
            Throwable poolCause = e.getCause();
            Assert.assertTrue(poolCause instanceof ClassNotFoundException);
        } finally {
            if (con != null) ConnectionPoolStatics.oclose(con);
            if (ds != null) ds.close();
        }

        BeeDataSource ds2 = null;
        try {//creation in constructor
            BeeDataSourceConfig config = createDefault();
            config.setPoolImplementClassName("xx.xx.xx");//invalid pool class name
            ds2 = new BeeDataSource(config);
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof PoolCreateFailedException);
            PoolCreateFailedException poolException = (PoolCreateFailedException) cause;
            Throwable poolCause = poolException.getCause();
            Assert.assertTrue(poolCause instanceof ClassNotFoundException);
        } finally {
            if (ds2 != null) ds2.close();
        }
    }

    public void testPoolInitializeFailedException() {
        BeeDataSource ds = null;
        try {
            BeeDataSourceConfig config = createDefault();
            config.setMaxActive(5);
            config.setInitialSize(10);
            ds = new BeeDataSource(config);
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof PoolInitializeFailedException);
            PoolInitializeFailedException poolInitializeException = (PoolInitializeFailedException) cause;
            Assert.assertTrue(poolInitializeException.getCause() instanceof BeeDataSourceConfigException);
            Throwable bottomException = poolInitializeException.getCause();
            Assert.assertTrue(bottomException.getMessage().contains("initialSize must not be greater than maxActive"));
        } finally {
            if (ds != null) ds.close();
        }
    }
}
