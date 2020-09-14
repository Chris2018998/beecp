/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.beecp.pool;

import cn.beecp.BeeDataSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.beecp.pool.PoolConstants.*;
import static cn.beecp.util.BeecpUtil.isNullText;
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
public final class RawConnectionPool implements ConnectionPool, ConnectionPoolJMXBean {
    private static AtomicInteger PoolNameIndex = new AtomicInteger(1);
    private final Logger log = LoggerFactory.getLogger(RawConnectionPool.class);
    private final ConnectionPoolMonitorVo monitorVo = new ConnectionPoolMonitorVo();
    private long defaultMaxWait;
    private Semaphore borrowSemaphore;
    private BeeDataSourceConfig poolConfig;
    private AtomicInteger poolState = new AtomicInteger(POOL_UNINIT);
    private String poolName = "";
    private String poolMode = "";

    /**
     * initialize pool with configuration
     *
     * @param config data source configuration
     */
    public void init(BeeDataSourceConfig config) {
        poolConfig = config;
        defaultMaxWait = MILLISECONDS.toNanos(poolConfig.getMaxWait());
        borrowSemaphore = new Semaphore(poolConfig.getBorrowSemaphoreSize(), poolConfig.isFairMode());
        poolName = !isNullText(config.getPoolName()) ? config.getPoolName() : "RawPool-" + PoolNameIndex.getAndIncrement();

        if (poolConfig.isFairMode()) {
            poolMode = "fair";
        } else {
            poolMode = "compete";
        }

        registerJMX();
        log.info("BeeCP({})has been startup{init size:{},max size:{},concurrent size:{},mode:{},max wait:{}ms},driver:{}}",
                poolName,
                0,
                0,
                poolConfig.getBorrowSemaphoreSize(),
                poolMode,
                poolConfig.getDriverClassName());

        poolState.set(POOL_NORMAL);
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
            if (poolState.get() != POOL_NORMAL) throw PoolCloseException;

            if (borrowSemaphore.tryAcquire(defaultMaxWait, NANOSECONDS)) {
                return poolConfig.getConnectionFactory().create();
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
     * return connection to pool
     *
     * @param pConn target connection need release
     */
    public void recycle(PooledConnection pConn) {
    }

    /**
     * close pool
     */
    public void close() throws SQLException {
        if (poolState.get() == POOL_CLOSED) throw PoolCloseException;
        while (true) {
            if (poolState.compareAndSet(POOL_NORMAL, POOL_CLOSED)) {
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
    public void reset() {
    }

    public void reset(boolean force) {
    }

    public int getConnTotalSize() {
        return 0;
    }

    public int getConnIdleSize() {
        return 0;
    }

    public int getConnUsingSize() {
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

    public ConnectionPoolMonitorVo getMonitorVo() {
        int totSize = getConnTotalSize();
        int idleSize = getConnIdleSize();
        monitorVo.setPoolName(poolName);
        monitorVo.setPoolMode(poolMode);
        monitorVo.setPoolState(POOL_NORMAL);
        monitorVo.setMaxActive(poolConfig.getBorrowSemaphoreSize());
        monitorVo.setIdleSize(idleSize);
        monitorVo.setUsingSize(totSize - idleSize);
        monitorVo.setSemaphoreWaiterSize(getSemaphoreWaitingSize());
        monitorVo.setTransferWaiterSize(getTransferWaitingSize());
        return monitorVo;
    }

    // register JMX
    private void registerJMX() {
        if (poolConfig.isEnableJMX()) {
            final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            try {
                final ObjectName poolRegName = new ObjectName("cn.beecp.pool.RawConnectionPool:type=BeeCP(" + poolName + ")");
                if (!mBeanServer.isRegistered(poolRegName)) {
                    mBeanServer.registerMBean(this, poolRegName);
                    log.info("Registered BeeCP({})as jmx-bean", poolName);
                } else {
                    log.error("Jmx-name BeeCP({})has been exist in jmx server", poolName);
                }
            } catch (Exception e) {
                log.warn("Failed to register pool jmx-bean", e);
            }

            try {
                final ObjectName configRegName = new ObjectName("cn.beecp.BeeDataSourceConfig:type=BeeCP(" + poolName + ")-config");
                if (!mBeanServer.isRegistered(configRegName)) {
                    mBeanServer.registerMBean(poolConfig, configRegName);
                    log.info("Registered BeeCP({})config as jmx-bean", poolName);
                } else {
                    log.error("Pool BeeCP({})config has been exist in jmx server", poolName);
                }
            } catch (Exception e) {
                log.warn("Failed to register pool jmx-bean", e);
            }
        }
    }

    // unregister JMX
    private void unregisterJMX() {
        if (poolConfig.isEnableJMX()) {
            final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            try {
                final ObjectName poolRegName = new ObjectName("cn.beecp.pool.RawConnectionPool:type=BeeCP(" + poolName + ")");
                if (mBeanServer.isRegistered(poolRegName)) {
                    mBeanServer.unregisterMBean(poolRegName);
                }
            } catch (Exception e) {
                log.warn("Failed to unregister pool jmx-bean", e);
            }

            try {
                final ObjectName configRegName = new ObjectName("cn.beecp.BeeDataSourceConfig:type=BeeCP(" + poolName + ")-config");
                if (mBeanServer.isRegistered(configRegName)) {
                    mBeanServer.unregisterMBean(configRegName);
                }
            } catch (Exception e) {
                log.warn("Failed to unregister pool jmx-bean", e);
            }
        }
    }
    //******************************** JMX **************************************//
}
