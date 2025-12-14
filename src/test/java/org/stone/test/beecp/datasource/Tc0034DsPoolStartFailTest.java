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
import org.stone.beecp.BeeDataSourceCreationException;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.beecp.pool.exception.PoolInitializeFailedException;
import org.stone.test.base.LogCollector;
import org.stone.test.beecp.objects.factory.ExceptionConnectionFactory;
import org.stone.test.beecp.objects.factory.MaxSizeMockConnectionFactory;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0034DsPoolStartFailTest {

    @Test
    public void testInitializationFail() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setInitialSize(5);
        config.setAsyncCreateInitConnections(false);//<---- test point

        String errorMsg1 = "Network exception,connection can't be established";
        ExceptionConnectionFactory connectionFactory = new ExceptionConnectionFactory();
        connectionFactory.setFailCause(new SQLException(errorMsg1));
        config.setConnectionFactory(connectionFactory);

        //1: create initial connections in sync mode(no one created successful)
        try (BeeDataSource ignored = new BeeDataSource(config)) {
            Assertions.fail("[testInitializationFail]Test failed");
        } catch (BeeDataSourceCreationException e) {
            Assertions.assertInstanceOf(SQLException.class, e.getCause());
            SQLException failCause = (SQLException) e.getCause();
            Assertions.assertEquals(errorMsg1, failCause.getMessage());
        }

        //2: create initial connections in sync mode(some created successful)
        MaxSizeMockConnectionFactory maxSizeConnectionFactory = new MaxSizeMockConnectionFactory(3);
        config.setConnectionFactory(maxSizeConnectionFactory);
        String errorMsg2 = "the count of created connections has reached max";
        try (BeeDataSource ignored = new BeeDataSource(config)) {
            Assertions.fail("[testInitializationFail]Test failed");
        } catch (BeeDataSourceCreationException e) {
            Assertions.assertInstanceOf(SQLException.class, e.getCause());
            SQLException failCause = (SQLException) e.getCause();
            Assertions.assertEquals(errorMsg2, failCause.getMessage());
        }

        //3: create initial connections in sync mode
        config.setPrintRuntimeLogs(true);
        config.setAsyncCreateInitConnections(true);//<---- test point
        LogCollector logCollector = LogCollector.startLogCollector();
        try (BeeDataSource ignored = new BeeDataSource(config)) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500L));

            Assertions.assertNotNull(ignored);
            String runtimeLog = logCollector.endLogCollector();
            Assertions.assertTrue(runtimeLog.contains("Failed to create initial connections by async mode"));
        } catch (BeeDataSourceCreationException e) {
            Assertions.fail("[testInitializationFail]Test failed");
        }
    }

    @Test
    public void testNullConfig() {
        try (FastConnectionPool pool = new FastConnectionPool()) {
            pool.start(null);
            Assertions.fail("[testNullConfig]Test failed");
        } catch (SQLException e) {
            Assertions.assertInstanceOf(PoolInitializeFailedException.class, e);
            Assertions.assertEquals("Data source configuration can't be null", e.getMessage());
        }
    }

    @Test
    public void testCasOnInitialization() throws Exception {
        try (FastConnectionPool pool = new FastConnectionPool()) {
            BeeDataSourceConfig config = createDefault();
            CasThread thread1 = new CasThread(pool, config);
            CasThread thread2 = new CasThread(pool, config);
            long targetTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500L);
            thread1.setTagetTime(targetTime);
            thread2.setTagetTime(targetTime);

            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            if (thread1.getFailCause() != null && thread2.getFailCause() == null) {
                Assertions.assertInstanceOf(PoolInitializeFailedException.class, thread1.getFailCause());
                Assertions.assertEquals("Pool has already initialized or in initializing", thread1.getFailCause().getMessage());
            } else if (thread1.getFailCause() == null && thread2.getFailCause() != null) {
                Assertions.assertInstanceOf(PoolInitializeFailedException.class, thread2.getFailCause());
                Assertions.assertEquals("Pool has already initialized or in initializing", thread2.getFailCause().getMessage());
            }
        }
    }

    private static class CasThread extends Thread {
        private final BeeDataSourceConfig config;
        private final FastConnectionPool pool;
        private long tagetTime;
        private SQLException failCause;

        public CasThread(FastConnectionPool pool, BeeDataSourceConfig config) {
            this.config = config;
            this.pool = pool;
        }

        public void run() {
            LockSupport.parkNanos(tagetTime - System.nanoTime());
            try {
                pool.start(config);
            } catch (SQLException e) {
                this.failCause = e;
            }
        }

        public void setTagetTime(long tagetTime) {
            this.tagetTime = tagetTime;
        }

        public SQLException getFailCause() {
            return failCause;
        }
    }
}
