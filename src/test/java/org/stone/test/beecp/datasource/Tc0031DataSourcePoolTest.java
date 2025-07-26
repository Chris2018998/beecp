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
import org.stone.beecp.*;
import org.stone.beecp.pool.ConnectionPoolStatics;
import org.stone.beecp.pool.exception.PoolCreateFailedException;
import org.stone.beecp.pool.exception.PoolInitializeFailedException;
import org.stone.beecp.pool.exception.PoolNotCreatedException;
import org.stone.test.base.LogCollector;
import org.stone.test.base.TestUtil;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;

import javax.sql.XAConnection;
import java.io.PrintWriter;
import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.*;

/**
 * @author Chris Liao
 */
public class Tc0031DataSourcePoolTest {

    @Test
    public void testOnInitializedPool() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        //config.setCreateTimeout(5);
        config.setPrintConfigInfo(false);
        BeeDataSource ds = null;

        try {
            ds = new BeeDataSource(config);
            LogCollector logCollector = LogCollector.startLogCollector();

            //getConnection test
            Connection con1 = ds.getConnection();
            Assertions.assertNotNull(con1);
            con1.close();
            con1 = ds.getConnection(JDBC_USER, JDBC_PASSWORD);
            Assertions.assertNotNull(con1);
            con1.close();

            //getXAConnection test
            XAConnection xCon1 = ds.getXAConnection();
            Assertions.assertNotNull(xCon1);
            xCon1.close();
            xCon1 = ds.getXAConnection(JDBC_USER, JDBC_PASSWORD);
            Assertions.assertNotNull(xCon1);
            xCon1.close();

            String logs = logCollector.endLogCollector();
            Assertions.assertTrue(logs.contains("getConnection (user,password) ignores authentication"));
            Assertions.assertTrue(logs.contains("getXAConnection (user,password) ignores authentication"));

            //test on methods of commonDataSource
            Assertions.assertNull(ds.getParentLogger());
            Assertions.assertNull(ds.getLogWriter());
            ds.setLogWriter(new PrintWriter(System.out));
            Assertions.assertNotNull(ds.getLogWriter());
            ds.setLogWriter(null);//reset back to null
            Assertions.assertNull(ds.getLogWriter());
            DriverManager.setLoginTimeout(0);
            Assertions.assertEquals(0, ds.getLoginTimeout());
            Assertions.assertEquals(0, DriverManager.getLoginTimeout());
            ds.setLoginTimeout(10);//ten seconds
            Assertions.assertEquals(10, ds.getLoginTimeout());
            Assertions.assertEquals(10, DriverManager.getLoginTimeout());

            //test on printRuntimeLog ind
            BeeConnectionPool pool = (BeeConnectionPool) TestUtil.getFieldValue(ds, "pool");
            Boolean printRuntimeLogInd = (Boolean) TestUtil.getFieldValue(pool, "printRuntimeLog");
            Assertions.assertFalse(printRuntimeLogInd);
            ds.setPrintRuntimeLog(true);
            printRuntimeLogInd = (Boolean) TestUtil.getFieldValue(pool, "printRuntimeLog");
            Assertions.assertTrue(printRuntimeLogInd);

            //test poolMonitorVo
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assertions.assertNotNull(vo);
            Assertions.assertEquals(1, vo.getIdleSize());

            //test on pool built-in lock
            vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(0, vo.getCreatingCount());
            Assertions.assertEquals(0, vo.getCreatingTimeoutCount());
            Thread[] interruptedThreads = ds.interruptConnectionCreating(false);
            Assertions.assertNotNull(interruptedThreads);
            Assertions.assertEquals(0, interruptedThreads.length);

            //maxWait
            try {
                ds.setMaxWait(-1L);
                fail("Max wait time test failed");
            } catch (InvalidParameterException e) {
                Assertions.assertEquals("The given value for configuration item 'max-wait' must be greater than zero", e.getMessage());
            }
            try {
                ds.setMaxWait(0L);
                fail("Max wait time test failed");
            } catch (InvalidParameterException e) {
                Assertions.assertEquals("The given value for configuration item 'max-wait' must be greater than zero", e.getMessage());
            }
            long newMaxWaitMillis2 = TimeUnit.SECONDS.toMillis(20L);
            ds.setMaxWait(newMaxWaitMillis2);
            Assertions.assertEquals(newMaxWaitMillis2, ds.getMaxWait());//changed

        } finally {
            if (ds != null && !ds.isClosed()) {
                ds.close();
                Assertions.assertTrue(ds.isClosed());
            }
        }
    }

    @Test
    public void testOnUninitializedPool() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        BeeConnectionPool pool = (BeeConnectionPool) TestUtil.getFieldValue(ds, "pool");
        Assertions.assertNull(pool);
        Assertions.assertTrue(ds.isClosed());

        //test on methods of commonDataSource
        Assertions.assertNull(ds.getParentLogger());
        Assertions.assertNull(ds.getLogWriter());
        ds.setLogWriter(new PrintWriter(System.out));
        Assertions.assertNull(ds.getLogWriter());
        Assertions.assertEquals(0, ds.getLoginTimeout());
        Assertions.assertEquals(0, DriverManager.getLoginTimeout());
        ds.setLoginTimeout(10);//ten seconds
        Assertions.assertEquals(0, ds.getLoginTimeout());
        Assertions.assertEquals(0, DriverManager.getLoginTimeout());

        ds.setPrintRuntimeLog(true);
        try {
            ds.getPoolMonitorVo();
            fail("testOnUninitializedPool");
        } catch (PoolNotCreatedException e) {
            Assertions.assertTrue(e.getMessage().contains("Pool not be created"));
        }

        try {
            ds.interruptConnectionCreating(false);
            fail("testOnUninitializedPool");
        } catch (PoolNotCreatedException e) {
            Assertions.assertTrue(e.getMessage().contains("Pool not be created"));
        }

        try {
            ds.clear(true);
            fail("testOnUninitializedPool");
        } catch (PoolNotCreatedException e) {
            Assertions.assertTrue(e.getMessage().contains("Pool not be created"));
        }

        try {
            ds.clear(true, new BeeDataSourceConfig());
            fail("testOnUninitializedPool");
        } catch (PoolNotCreatedException e) {
            Assertions.assertTrue(e.getMessage().contains("Pool not be created"));
        }

        BeeDataSourceConfig config = createDefault();
        MockCommonConnectionFactory factory = new MockCommonConnectionFactory();
        factory.setReturnNullOnCreate(true);
        config.setConnectionFactory(factory);
        new BeeDataSource(config);
    }

    @Test
    public void testPoolClassNotFound() {
        BeeDataSource ds = null;
        Connection con = null;
        try {//lazy creation
            ds = new BeeDataSource(JDBC_DRIVER, JDBC_URL, JDBC_USER, JDBC_PASSWORD);
            ds.setPoolImplementClassName("xx.xx.xx");//invalid pool class name
            con = ds.getConnection();
            fail("testPoolClassNotFound");
        } catch (SQLException e) {
            Throwable poolCause = e.getCause();
            Assertions.assertInstanceOf(ClassNotFoundException.class, poolCause);
        } finally {
            if (con != null) ConnectionPoolStatics.oclose(con);
            if (ds != null) ds.close();
        }

        BeeDataSource ds2 = null;
        try {//creation in constructor
            BeeDataSourceConfig config = createDefault();
            config.setPoolImplementClassName("xx.xx.xx");//invalid pool class name
            ds2 = new BeeDataSource(config);
            fail("testPoolClassNotFound");
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            Assertions.assertInstanceOf(PoolCreateFailedException.class, cause);
            PoolCreateFailedException poolException = (PoolCreateFailedException) cause;
            Throwable poolCause = poolException.getCause();
            Assertions.assertInstanceOf(ClassNotFoundException.class, poolCause);
        } finally {
            if (ds2 != null) ds2.close();
        }
    }

    @Test
    public void testPoolInitializeFailedException() {
        BeeDataSource ds = null;
        try {
            BeeDataSourceConfig config = createDefault();
            config.setMaxActive(5);
            config.setInitialSize(10);
            ds = new BeeDataSource(config);
            fail("testPoolInitializeFailedException");
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            Assertions.assertInstanceOf(PoolInitializeFailedException.class, cause);
            PoolInitializeFailedException poolInitializeException = (PoolInitializeFailedException) cause;
            Assertions.assertInstanceOf(BeeDataSourceConfigException.class, poolInitializeException.getCause());
            Throwable bottomException = poolInitializeException.getCause();

            System.out.println(bottomException.getMessage());
            //Assertions.assertTrue(bottomException.getMessage().equals("The configured value of item 'initial-size' cannot be greater than the configured value of item 'max-active'"));
        } finally {
            if (ds != null) ds.close();
        }
    }
}
