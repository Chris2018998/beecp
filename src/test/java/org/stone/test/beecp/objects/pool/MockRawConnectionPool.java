/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.beecp.objects.pool;

import org.stone.beecp.*;
import org.stone.beecp.exception.BeeDataSourcePoolNotReadyException;
import org.stone.beecp.exception.BeeDataSourcePoolStartedFailureException;
import org.stone.beecp.exception.ConnectionGetInterruptedException;
import org.stone.beecp.exception.ConnectionGetTimeoutException;
import org.stone.tools.extension.InterruptableSemaphore;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.stone.beecp.pool.ConnectionPoolStatics.*;
import static org.stone.tools.LogPrinter.DefaultLogPrinter;

/**
 * JDBC Connection Pool Implementation,which
 * <p>
 * return raw connections to borrowers directly.
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class MockRawConnectionPool implements BeeConnectionPool {
    private static final PoolMonitorVoImpl monitorVo = new PoolMonitorVoImpl();
    private final AtomicInteger poolState = new AtomicInteger(POOL_NEW);
    private String poolName = "";
    private String poolMode = "";
    private long defaultMaxWait;
    private InterruptableSemaphore borrowSemaphore;
    private BeeDataSourceConfig poolConfig;
    private boolean isRawXaConnFactory;
    private BeeConnectionFactory rawConnFactory;
    private BeeXaConnectionFactory rawXaConnFactory;

    /**
     * initialize pool with configuration
     *
     * @param config data source configuration
     */
    public void start(BeeDataSourceConfig config) throws SQLException {
        if (config == null)
            throw new BeeDataSourcePoolStartedFailureException("Data source configuration can't be null");
        this.poolConfig = config.check();
        this.defaultMaxWait = MILLISECONDS.toNanos(poolConfig.getMaxWait());
        this.poolName = poolConfig.getPoolName();
        this.borrowSemaphore = new InterruptableSemaphore(poolConfig.getSemaphoreSize(), poolConfig.isFairMode());

        if (poolConfig.isFairMode()) {
            poolMode = "fair";
        } else {
            poolMode = "compete";
        }

        Object rawFactory = this.poolConfig.getConnectionFactory();
        if (rawFactory instanceof BeeXaConnectionFactory) {
            this.isRawXaConnFactory = true;
            this.rawXaConnFactory = (BeeXaConnectionFactory) rawFactory;
        } else {
            this.rawConnFactory = (BeeConnectionFactory) rawFactory;
        }

        //registerJMX();
        DefaultLogPrinter.info("BeeCP({})has been startup{init size:{},max size:{}, size:{},mode:{},max wait:{}ms},driver:{}}",
                poolName,
                0,
                0,
                poolConfig.getSemaphoreSize(),
                poolMode,
                0,
                poolConfig.getDriverClassName());

        poolState.set(POOL_READY);
    }

    public Connection getConnection() throws SQLException {
        return getConnection(false, null, null);
    }

    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection(true, username, password);
    }

    //borrow a connection from pool
    public XAConnection getXAConnection() throws SQLException {
        return getXAConnection(false, null, null);
    }

    public XAConnection getXAConnection(String username, String password) throws SQLException {
        return getXAConnection(true, username, password);
    }

    private Connection getConnection(boolean useUsername, String username, String password) throws SQLException {
        try {
            if (poolState.get() != POOL_READY)
                throw new BeeDataSourcePoolNotReadyException("Access forbidden,connection pool was closed or in clearing");
            if (borrowSemaphore.tryAcquire(defaultMaxWait, NANOSECONDS)) {
                if (useUsername) {
                    if (isRawXaConnFactory) {
                        return rawXaConnFactory.create().getConnection();
                    } else {
                        return rawConnFactory.create();
                    }
                } else {
                    if (isRawXaConnFactory) {
                        return rawXaConnFactory.create().getConnection();
                    } else {
                        return rawConnFactory.create();
                    }
                }
            } else {
                throw new ConnectionGetTimeoutException("Waited timeout on pool semaphore");
            }
        } catch (InterruptedException e) {
            throw new ConnectionGetInterruptedException("An interruption occurred while waiting on pool semaphore");
        } finally {
            borrowSemaphore.release();
        }
    }

    private XAConnection getXAConnection(boolean useUsername, String username, String password) throws SQLException {
        try {
            if (poolState.get() != POOL_READY)
                throw new BeeDataSourcePoolNotReadyException("Access forbidden,connection pool was closed or in clearing");

            if (borrowSemaphore.tryAcquire(defaultMaxWait, NANOSECONDS)) {
                if (isRawXaConnFactory) {
                    if (useUsername) {
                        return rawXaConnFactory.create();
                    } else {
                        return rawXaConnFactory.create();
                    }
                } else {
                    throw new SQLException("Not support");
                }
            } else {
                throw new ConnectionGetTimeoutException("Waited timeout on pool semaphore");
            }
        } catch (InterruptedException e) {
            throw new ConnectionGetInterruptedException("An interruption occurred while waiting on pool semaphore");
        } finally {
            borrowSemaphore.release();
        }
    }

    /**
     * close pool
     */
    public void close() {
        int state = poolState.get();
        if (state == POOL_CLOSED) return;
        if (state == POOL_READY)
            poolState.compareAndSet(POOL_READY, POOL_CLOSED);
    }

    /**
     * is pool shutdown
     */
    public boolean isClosed() {
        return poolState.get() == POOL_CLOSED;
    }

    public boolean isReady() {
        return poolState.get() == POOL_READY;
    }

    public boolean isSuspended() {
        return poolState.get() == POOL_SUSPENDED;
    }

    public boolean suspendPool() {
        return poolState.compareAndSet(POOL_READY, POOL_SUSPENDED);
    }

    public boolean resumePool() {
        return poolState.compareAndSet(POOL_SUSPENDED, POOL_READY);
    }

    public List<Thread> interruptWaitingThreads() {
        return null;
    }

    //******************************** JMX **************************************//
    public void restart(boolean force) {
        //do nothing
    }

    public void restart(boolean force, BeeDataSourceConfig config) {
        //do nothing
    }

    public String getPoolName() {
        return this.poolName;
    }

    public int getSemaphoreAcquiredSize() {
        return poolConfig.getSemaphoreSize() - borrowSemaphore.availablePermits();
    }

    public int getSemaphoreWaitingSize() {
        return borrowSemaphore.getQueueLength();
    }

    //set pool info debug switch
    public void enableLogPrinter(boolean enabledDebug) {
        //do nothing
    }

    public void enableLogCache(boolean enable) {
        //do nothing
    }

    public boolean cancelStatement(String logId) {
        return false;
    }

    public List<BeeMethodLog> getLogs(int type) {
        return Collections.emptyList();
    }

    public void clearLogs(int type) {
    }

    public void changeLogListener(BeeMethodLogListener handler) {

    }

    public BeeConnectionPoolMonitorVo getPoolMonitorVo() {
        monitorVo.setPoolName(this.poolName);
        monitorVo.setPoolMode(poolConfig.isFairMode());
        monitorVo.setPoolState(poolState.get());
        monitorVo.setMaxSize(poolConfig.getMaxActive());

        monitorVo.setIdleSize(0);
        monitorVo.setBorrowedSize(0);
        monitorVo.setSemaphoreWaitingSize(getSemaphoreWaitingSize());
        monitorVo.setTransferWaitingSize(0);
        return monitorVo;
    }

//    // register JMX
//    private void registerJMX() {
//        if (poolConfig.isEnableJmx()) {
//            final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
//            try {
//                final ObjectName poolRegName = new ObjectName("RawConnectionPool:type=BeeCP(" + poolName + ")");
//                if (!mBeanServer.isRegistered(poolRegName)) {
//                    mBeanServer.registerMBean(this, poolRegName);
//                    CommonLog.info("Registered BeeCP({})as jmx-bean", poolName);
//                } else {
//                    CommonLog.error("Jmx-name BeeCP({})has been exist in jmx server", poolName);
//                }
//            } catch (Exception e) {
//                CommonLog.warn("Failed to register pool jmx-bean", e);
//            }
//
//            try {
//                final ObjectName configRegName = new ObjectName("BeeDataSourceConfig:type=BeeCP(" + poolName + ")-config");
//                if (!mBeanServer.isRegistered(configRegName)) {
//                    mBeanServer.registerMBean(poolConfig, configRegName);
//                    CommonLog.info("Registered BeeCP({})config as jmx-bean", poolName);
//                } else {
//                    CommonLog.error("Pool BeeCP({})config has been exist in jmx server", poolName);
//                }
//            } catch (Exception e) {
//                CommonLog.warn("Failed to register pool jmx-bean", e);
//            }
//        }
//    }
//
//    // unregister JMX
//    private void unregisterJMX() {
//        if (poolConfig.isEnableJmx()) {
//            final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
//            try {
//                final ObjectName poolRegName = new ObjectName("RawConnectionPool:type=BeeCP(" + poolName + ")");
//                if (mBeanServer.isRegistered(poolRegName)) {
//                    mBeanServer.unregisterMBean(poolRegName);
//                }
//            } catch (Exception e) {
//                CommonLog.warn("Failed to unregister pool jmx-bean", e);
//            }
//
//            try {
//                final ObjectName configRegName = new ObjectName("BeeDataSourceConfig:type=BeeCP(" + poolName + ")-config");
//                if (mBeanServer.isRegistered(configRegName)) {
//                    mBeanServer.unregisterMBean(configRegName);
//                }
//            } catch (Exception e) {
//                CommonLog.warn("Failed to unregister pool jmx-bean", e);
//            }
//        }
//    }
    //******************************** JMX **************************************//
}
