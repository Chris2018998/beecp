/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.pool;

import org.stone.beecp.*;
import org.stone.beecp.pool.exception.ConnectionGetForbiddenException;
import org.stone.beecp.pool.exception.PoolCreateFailedException;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.XAConnection;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.stone.beecp.pool.ConnectionPoolStatics.*;
import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * JDBC Connection Pool Implementation,which
 * <p>
 * return raw connections to borrowers directly.
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class RawConnectionPool implements BeeConnectionPool, BeeConnectionPoolJmxBean {
    private static final AtomicInteger poolNameIndex = new AtomicInteger(1);
    private static final FastConnectionPoolMonitorVo monitorVo = new FastConnectionPoolMonitorVo();
    private final AtomicInteger poolState = new AtomicInteger(POOL_NEW);
    private String poolName = "";
    private String poolMode = "";
    private long defaultMaxWait;
    private Semaphore borrowSemaphore;
    private BeeDataSourceConfig poolConfig;
    private boolean isRawXaConnFactory;
    private RawConnectionFactory rawConnFactory;
    private RawXaConnectionFactory rawXaConnFactory;

    /**
     * initialize pool with configuration
     *
     * @param config data source configuration
     */
    public void init(BeeDataSourceConfig config) throws SQLException {
        poolConfig = config;
        defaultMaxWait = MILLISECONDS.toNanos(poolConfig.getMaxWait());
        borrowSemaphore = new Semaphore(poolConfig.getBorrowSemaphoreSize(), poolConfig.isFairMode());
        poolName = isNotBlank(config.getPoolName()) ? config.getPoolName() : "RawPool-" + poolNameIndex.getAndIncrement();

        if (poolConfig.isFairMode()) {
            poolMode = "fair";
        } else {
            poolMode = "compete";
        }

        Object rawFactory = this.poolConfig.getConnectionFactory();
        if (rawFactory instanceof RawXaConnectionFactory) {
            this.isRawXaConnFactory = true;
            this.rawXaConnFactory = (RawXaConnectionFactory) rawFactory;
        } else if (rawFactory instanceof RawConnectionFactory) {
            this.rawConnFactory = (RawConnectionFactory) rawFactory;
        } else {
            throw new PoolCreateFailedException("Invalid connection factory");
        }

        registerJMX();
        CommonLog.info("BeeCP({})has been startup{init size:{},max size:{}, size:{},mode:{},max wait:{}ms},driver:{}}",
                poolName,
                0,
                0,
                poolConfig.getBorrowSemaphoreSize(),
                poolMode,
                0,
                poolConfig.getDriverClassName());

        poolState.set(POOL_READY);
    }

    /**
     * borrow one connection from pool
     *
     * @return If exists idle connection in pool,then return one;if not, waiting
     * until other borrower release
     * @throws SQLException if pool is closed or waiting timeout,then throw exception
     */
    public Connection getConnection() throws SQLException {
        try {
            if (poolState.get() != POOL_READY)
                throw new ConnectionGetForbiddenException("Access forbidden,connection pool was closed or in clearing");
            if (borrowSemaphore.tryAcquire(defaultMaxWait, NANOSECONDS)) {
                if (isRawXaConnFactory) {
                    return rawXaConnFactory.create().getConnection();
                } else {
                    return rawConnFactory.create();
                }
            } else {
                throw new SQLException("Request timeout");
            }
        } catch (InterruptedException e) {
            throw new SQLException("Request interrupted");
        } finally {
            borrowSemaphore.release();
        }
    }

    //borrow a connection from pool
    public XAConnection getXAConnection() throws SQLException {
        try {
            if (poolState.get() != POOL_READY)
                throw new ConnectionGetForbiddenException("Access forbidden,connection pool was closed or in clearing");
            if (borrowSemaphore.tryAcquire(defaultMaxWait, NANOSECONDS)) {
                if (isRawXaConnFactory) {
                    return rawXaConnFactory.create();
                } else {
                    throw new SQLException("Not support");
                }
            } else {
                throw new SQLException("Request timeout");
            }
        } catch (InterruptedException e) {
            throw new SQLException("Request interrupted");
        } finally {
            borrowSemaphore.release();
        }
    }

    /**
     * Connection return to pool after it end use,if exist waiter in pool,
     * then try to transfer the connection to one waiting borrower
     *
     * @param p target connection need release
     */
    public void recycle(PooledConnection p) {
        //do nothing
    }

    /**
     * close pool
     */
    public void close() {
        if (poolState.get() == POOL_CLOSED) return;
        while (true) {
            if (poolState.compareAndSet(POOL_READY, POOL_CLOSED)) {
                unregisterJMX();
                break;
            } else if (poolState.get() == POOL_CLOSED) {
                break;
            }
        }
    }

    public long getElapsedTimeSinceCreationLock() {
        return 0;
    }

    public void interruptThreadsOnCreationLock() {
        //do noting
    }

    /**
     * is pool shutdown
     */
    public boolean isClosed() {
        return poolState.get() == POOL_CLOSED;
    }

    //******************************** JMX **************************************//
    public void clear(boolean force) {
        //do nothing
    }

    public void clear(boolean force, BeeDataSourceConfig config) {
        //do nothing
    }

    public String getPoolName() {
        return this.poolName;
    }

    public int getTotalSize() {
        return 0;
    }

    public int getIdleSize() {
        return 0;
    }

    public int getUsingSize() {
        return 0;
    }

    public int getSemaphoreAcquiredSize() {
        return poolConfig.getBorrowSemaphoreSize() - borrowSemaphore.availablePermits();
    }

    public int getSemaphoreWaitingSize() {
        return borrowSemaphore.getQueueLength();
    }

    public int getTransferWaitingSize() {
        return 0;
    }

    //set pool info debug switch
    public void setPrintRuntimeLog(boolean enabledDebug) {
        //do nothing
    }

    public BeeConnectionPoolMonitorVo getPoolMonitorVo() {
        int totSize = getTotalSize();
        int idleSize = getIdleSize();
        monitorVo.setPoolName(poolName);
        monitorVo.setPoolMode(poolMode);
        monitorVo.setPoolState(poolState.get());
        monitorVo.setPoolMaxSize(poolConfig.getMaxActive());
        monitorVo.setIdleSize(idleSize);
        monitorVo.setUsingSize(totSize - idleSize);
        monitorVo.setSemaphoreWaitingSize(getSemaphoreWaitingSize());
        monitorVo.setTransferWaitingSize(getTransferWaitingSize());
        return monitorVo;
    }

    // register JMX
    private void registerJMX() {
        if (poolConfig.isEnableJmx()) {
            final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            try {
                final ObjectName poolRegName = new ObjectName("RawConnectionPool:type=BeeCP(" + poolName + ")");
                if (!mBeanServer.isRegistered(poolRegName)) {
                    mBeanServer.registerMBean(this, poolRegName);
                    CommonLog.info("Registered BeeCP({})as jmx-bean", poolName);
                } else {
                    CommonLog.error("Jmx-name BeeCP({})has been exist in jmx server", poolName);
                }
            } catch (Exception e) {
                CommonLog.warn("Failed to register pool jmx-bean", e);
            }

            try {
                final ObjectName configRegName = new ObjectName("BeeDataSourceConfig:type=BeeCP(" + poolName + ")-config");
                if (!mBeanServer.isRegistered(configRegName)) {
                    mBeanServer.registerMBean(poolConfig, configRegName);
                    CommonLog.info("Registered BeeCP({})config as jmx-bean", poolName);
                } else {
                    CommonLog.error("Pool BeeCP({})config has been exist in jmx server", poolName);
                }
            } catch (Exception e) {
                CommonLog.warn("Failed to register pool jmx-bean", e);
            }
        }
    }

    // unregister JMX
    private void unregisterJMX() {
        if (poolConfig.isEnableJmx()) {
            final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            try {
                final ObjectName poolRegName = new ObjectName("RawConnectionPool:type=BeeCP(" + poolName + ")");
                if (mBeanServer.isRegistered(poolRegName)) {
                    mBeanServer.unregisterMBean(poolRegName);
                }
            } catch (Exception e) {
                CommonLog.warn("Failed to unregister pool jmx-bean", e);
            }

            try {
                final ObjectName configRegName = new ObjectName("BeeDataSourceConfig:type=BeeCP(" + poolName + ")-config");
                if (mBeanServer.isRegistered(configRegName)) {
                    mBeanServer.unregisterMBean(configRegName);
                }
            } catch (Exception e) {
                CommonLog.warn("Failed to unregister pool jmx-bean", e);
            }
        }
    }
    //******************************** JMX **************************************//
}
