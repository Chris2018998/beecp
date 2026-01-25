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
import org.stone.beecp.exception.BeeDataSourceConfigException;
import org.stone.beecp.exception.BeeDataSourcePoolRestartedFailureException;
import org.stone.test.beecp.config.DsConfigFactory;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0044DataSourceRestartTest {

    @Test
    public void testRestart() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(DsConfigFactory.JDBC_URL);
        config.setDriverClassName(DsConfigFactory.JDBC_DRIVER);
        config.setParkTimeForRetry(50L);
        config.setInitialSize(1);
        try (BeeDataSource ds = new BeeDataSource(config)) {
            //restart 1
            Assertions.assertEquals(1, ds.getPoolMonitorVo().getIdleSize());
            ds.restart(false);//no borrowed connections
            Assertions.assertEquals(0, ds.getPoolMonitorVo().getIdleSize());//restart successful

            //force restart when exists borrowed connection
            Connection con = ds.getConnection();
            Assertions.assertNotNull(con);
            Assertions.assertEquals(1, ds.getPoolMonitorVo().getBorrowedSize());
            ds.restart(true);//recycle borrowed connection by force
            Assertions.assertEquals(0, ds.getPoolMonitorVo().getIdleSize());//no idle
            Assertions.assertEquals(0, ds.getPoolMonitorVo().getBorrowedSize());//no using
            Assertions.assertTrue(con.isClosed());//closed by force

            //3: restart(wait borrowed connection return to pool)
            con = ds.getConnection();
            Assertions.assertNotNull(con);
            Assertions.assertEquals(1, ds.getPoolMonitorVo().getBorrowedSize());
            RestartThread restartThread = new RestartThread(ds, false);
            restartThread.start();
            //sleep 100 milliseconds
            Thread.sleep(100L);
            Assertions.assertTrue(restartThread.isAlive());
            Assertions.assertTrue(ds.getPoolMonitorVo().isRestarting());
            Assertions.assertEquals("Pool is restarting", ds.toString());

            //sleep 100 milliseconds again
            Thread.sleep(100L);
            Assertions.assertTrue(restartThread.isAlive());
            con.close();//close the borrowed connection
            restartThread.join();
            Assertions.assertEquals(0, ds.getPoolMonitorVo().getIdleSize());//no idle
            Assertions.assertEquals(0, ds.getPoolMonitorVo().getBorrowedSize());//no using
        }
    }

    @Test
    public void testRestartWithNewConfig() throws Exception {
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.setJdbcUrl(DsConfigFactory.JDBC_URL);
        config1.setDriverClassName(DsConfigFactory.JDBC_DRIVER);
        config1.setParkTimeForRetry(50L);
        config1.setInitialSize(1);
        config1.setPoolName("BeeCP1");

        try (BeeDataSource ds = new BeeDataSource(config1)) {
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(1, ds.getPoolMonitorVo().getIdleSize());
            Assertions.assertEquals("BeeCP1", ds.getPoolMonitorVo().getPoolName());

            //new configuration
            BeeDataSourceConfig config2 = new BeeDataSourceConfig();
            config2.setJdbcUrl(DsConfigFactory.JDBC_URL);
            config2.setDriverClassName(DsConfigFactory.JDBC_DRIVER);
            config2.setInitialSize(10);
            config2.setMaxActive(10);
            config2.setPoolName("BeeCP2");
            config2.addSqlExceptionCode(500151);
            config2.addSqlExceptionState("0A000");

            ds.restart(true, config2);
            Assertions.assertEquals(10, ds.getPoolMonitorVo().getIdleSize());
            Assertions.assertEquals("BeeCP2", ds.getPoolMonitorVo().getPoolName());//Great! success!
            List<Integer> codeList2 = ds.getSqlExceptionCodeList();
            List<String> stateList2 = ds.getSqlExceptionStateList();
            Assertions.assertNotNull(codeList2);
            Assertions.assertNotNull(stateList2);
            Assertions.assertTrue(codeList2.contains(500151));
            Assertions.assertTrue(stateList2.contains("0A000"));

            //mock restart failure
            MockConnectionFactory conFactory = new MockConnectionFactory();
            conFactory.setFailCause(new SQLException("Network error"));
            BeeDataSourceConfig config3 = new BeeDataSourceConfig();
            config3.setConnectionFactory(conFactory);
            config3.setInitialSize(1);
            try {
                Assertions.assertFalse(ds.getPoolMonitorVo().isRestartFailed());
                ds.restart(true, config3);
                Assertions.fail("[testRestartWithNewConfig]failed");
            } catch (SQLException e) {
                Assertions.assertInstanceOf(BeeDataSourcePoolRestartedFailureException.class, e);
                String cause = e.getCause().getMessage();
                Assertions.assertEquals("Network error", cause);
                //check monitor vo
                Assertions.assertTrue(ds.getPoolMonitorVo().isRestartFailed());
                Assertions.assertEquals("Pool has restarted failed", ds.toString());

                //^-^: New configuration is coming to save your app
                BeeDataSourceConfig config4 = createDefault();
                config4.setPoolName("BeeCP4");
                config4.setInitialSize(5);
                config4.setMaxActive(5);
                config4.addSqlExceptionCode(500152);
                config4.addSqlExceptionState("0B000");

                ds.restart(true, config4);
                Assertions.assertEquals(5, ds.getPoolMonitorVo().getIdleSize());
                Assertions.assertEquals("BeeCP4", ds.getPoolMonitorVo().getPoolName());//Congratulation! success!

                List<Integer> codeList4 = ds.getSqlExceptionCodeList();
                List<String> stateList4 = ds.getSqlExceptionStateList();
                Assertions.assertNotNull(codeList4);
                Assertions.assertNotNull(stateList4);
                Assertions.assertFalse(codeList4.contains(500151));
                Assertions.assertFalse(stateList4.contains("0A000"));
                Assertions.assertTrue(codeList4.contains(500152));
                Assertions.assertTrue(stateList4.contains("0B000"));
            }
        }
    }

    @Test
    public void testRestartFailure() throws Exception {
        BeeDataSourceConfig config = createDefault();
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try {
                ds.restart(true, null);
                Assertions.fail("[testRestartFailure]failed");
            } catch (BeeDataSourceConfigException e) {
                Assertions.assertEquals("Data source configuration can't be null", e.getMessage());
            }

            RestartThread thread1 = new RestartThread(ds, true);
            RestartThread thread2 = new RestartThread(ds, true);
            long concurrentTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500L);
            thread1.setConcurrentTime(concurrentTime);
            thread2.setConcurrentTime(concurrentTime);
            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();
        }
    }

    private static class RestartThread extends Thread {
        private final boolean force;
        private final BeeDataSource ds;
        private long concurrentTime;
        private Exception failException;

        public RestartThread(BeeDataSource ds, boolean force) {
            this.ds = ds;
            this.force = force;
        }

        public Exception getFailException() {
            return failException;
        }

        public void setConcurrentTime(long concurrentTime) {
            this.concurrentTime = concurrentTime;
        }

        public void run() {
            if (concurrentTime > 0L)
                LockSupport.parkNanos(concurrentTime - System.nanoTime());

            try {
                ds.restart(force);
            } catch (Exception e) {
                this.failException = e;
            }
        }
    }
}
