/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool;

import cn.beecp.BeeDataSourceConfig;
import cn.beecp.RawConnectionFactory;
import cn.beecp.RawXaConnectionFactory;
import cn.beecp.pool.exception.PoolCreateFailedException;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.XAConnection;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.beecp.pool.PoolStaticCenter.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * JDBC Connection Pool Implementation,which
 * <p>
 * return raw connections to borrowers directly.
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class RawConnectionPool implements ConnectionPool, ConnectionPoolJmxBean {
    private static final AtomicInteger poolNameIndex = new AtomicInteger(1);
    private static final ConnectionPoolMonitorVo monitorVo = new ConnectionPoolMonitorVo();

    private String poolName = "";
    private String poolMode = "";
    private long defaultMaxWait;
    private Semaphore borrowSemaphore;
    private BeeDataSourceConfig poolConfig;
    private boolean isRawXaConnFactory;
    private RawConnectionFactory rawConnFactory;
    private RawXaConnectionFactory rawXaConnFactory;
    private AtomicInteger poolState = new AtomicInteger(POOL_NEW);

    /**
     * initialize pool with configuration
     *
     * @param config data source configuration
     */
    public void init(BeeDataSourceConfig config) throws SQLException {
        poolConfig = config;
        defaultMaxWait = MILLISECONDS.toNanos(poolConfig.getMaxWait());
        borrowSemaphore = new Semaphore(poolConfig.getBorrowSemaphoreSize(), poolConfig.isFairMode());
        poolName = !isBlank(config.getPoolName()) ? config.getPoolName() : "RawPool-" + poolNameIndex.getAndIncrement();

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
        CommonLog.info("BeeCP({})has been startup{init size:{},max size:{},concurrent size:{},mode:{},max wait:{}ms},driver:{}}",
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
            if (poolState.get() != POOL_READY) throw PoolCloseException;
            if (borrowSemaphore.tryAcquire(defaultMaxWait, NANOSECONDS)) {
                if (isRawXaConnFactory) {
                    return rawXaConnFactory.create().getConnection();
                } else {
                    return rawConnFactory.create();
                }
            } else {
                throw RequestTimeoutException;
            }
        } catch (InterruptedException e) {
            throw RequestInterruptException;
        } finally {
            borrowSemaphore.release();
        }
    }

    //borrow a connection from pool
    public XAConnection getXAConnection() throws SQLException {
        try {
            if (poolState.get() != POOL_READY) throw PoolCloseException;
            if (borrowSemaphore.tryAcquire(defaultMaxWait, NANOSECONDS)) {
                if (isRawXaConnFactory) {
                    return rawXaConnFactory.create();
                } else {
                    throw new SQLException("Not support");
                }
            } else {
                throw RequestTimeoutException;
            }
        } catch (InterruptedException e) {
            throw RequestInterruptException;
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

    /**
     * is pool shutdown
     */
    public boolean isClosed() {
        return poolState.get() == POOL_CLOSED;
    }

    //******************************** JMX **************************************//
    // close all connections
    public void clear() {
        //do nothing
    }

    public void clear(boolean force) {
        //do nothing
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

    public ConnectionPoolMonitorVo getPoolMonitorVo() {
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
                final ObjectName poolRegName = new ObjectName("cn.beecp.pool.RawConnectionPool:type=BeeCP(" + poolName + ")");
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
                final ObjectName configRegName = new ObjectName("cn.beecp.BeeDataSourceConfig:type=BeeCP(" + poolName + ")-config");
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
                final ObjectName poolRegName = new ObjectName("cn.beecp.pool.RawConnectionPool:type=BeeCP(" + poolName + ")");
                if (mBeanServer.isRegistered(poolRegName)) {
                    mBeanServer.unregisterMBean(poolRegName);
                }
            } catch (Exception e) {
                CommonLog.warn("Failed to unregister pool jmx-bean", e);
            }

            try {
                final ObjectName configRegName = new ObjectName("cn.beecp.BeeDataSourceConfig:type=BeeCP(" + poolName + ")-config");
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
