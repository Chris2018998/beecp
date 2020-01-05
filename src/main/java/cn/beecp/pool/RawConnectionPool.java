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

import static cn.beecp.pool.PoolExceptionList.RequestInterruptException;
import static cn.beecp.pool.PoolExceptionList.RequestTimeoutException;
import static cn.beecp.util.BeecpUtil.isNullText;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * JDBC Connection Pool Implementation,which
 *
 * return raw connections to borrowers directly.
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class RawConnectionPool implements ConnectionPool, ConnectionPoolJMXBean {
	private Semaphore poolSemaphore;
	private long DefaultMaxWaitMills;
	private BeeDataSourceConfig poolConfig;
	private String poolName;
	private static Logger log = LoggerFactory.getLogger(RawConnectionPool.class);
	private static AtomicInteger PoolNameIndex = new AtomicInteger(1);

	/**
	 * initialize pool with configuration
	 *
	 * @param config
	 *            data source configuration
	 */
	public void init(BeeDataSourceConfig config){
		poolConfig = config;
		DefaultMaxWaitMills = poolConfig.getMaxWait();
		poolSemaphore = new Semaphore(poolConfig.getConcurrentSize(), poolConfig.isFairMode());
		poolName = !isNullText(config.getPoolName()) ? config.getPoolName(): "RawPool-" + PoolNameIndex.getAndIncrement();

		String mode;
		if (poolConfig.isFairMode()) {
			mode = "fair";
		} else {
			mode = "compete";
		}

		log.info("BeeCP({})has been startup{init size:{},max size:{},concurrent size:{},mode:{},max wait:{}ms}",
				poolName,
				0,
				0,
				poolConfig.getConcurrentSize(),
				mode,
				poolConfig.getMaxWait());
		registerJMX();
	}

	/**
	 * borrow a connection from pool
	 *
	 * @return If exists idle connection in pool,then return one;if not, waiting
	 *         until other borrower release
	 * @throws SQLException
	 *             if pool is closed or waiting timeout,then throw exception
	 */
	public Connection getConnection() throws SQLException {
		return getConnection(DefaultMaxWaitMills);
	}

	/**
	 * borrow one connection from pool
	 *
	 * @param wait
	 *            must be greater than zero
	 *
	 * @return If exists idle connection in pool,then return one;if not, waiting
	 *         until other borrower release
	 * @throws SQLException
	 *             if pool is closed or waiting timeout,then throw exception
	 */
	public Connection getConnection(long wait) throws SQLException {
		try {
			wait = MILLISECONDS.toNanos(wait);
			if (poolSemaphore.tryAcquire(wait, NANOSECONDS)) {
				return poolConfig.getConnectionFactory().create();
			} else {
				throw RequestTimeoutException;
			}
		} catch (InterruptedException e) {
			throw RequestInterruptException;
		} finally {
			poolSemaphore.release();
		}
	}

	/**
	 * return connection to pool
	 *
	 * @param pConn
	 *            target connection need release
	 * @param needTest,
	 *            true check active
	 */
	public void release(PooledConnection pConn, boolean needTest) {}

	/**
	 * close pool
	 */
	public void shutdown() {
		unregisterJMX();
	}

	//******************************** JMX **************************************//
	// close all connections
	public void reset() {}
	public void reset(boolean force) {}
	public int getConnTotalSize(){
		return 0;
	}
	public int getConnIdleSize(){
		return 0;
	}
	public int getConnUsingSize(){
		return 0;
	}
	public int getSemaphoreAcquiredSize(){
		return poolConfig.getConcurrentSize()-poolSemaphore.availablePermits();
	}
	public int getSemaphoreWatingSize(){
		return poolSemaphore.getQueueLength();
	}
	public int getTransferWatingSize(){
		return 0;
	}
	// register JMX
	private void registerJMX() {
		if (poolConfig.isEnableJMX()) {
			final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
			try {
				final ObjectName poolRegName = new ObjectName("cn.beecp.pool.RawConnectionPool:type=BeeCP("+poolName+")");
				if (!mBeanServer.isRegistered(poolRegName)) {
					mBeanServer.registerMBean(this,poolRegName);
					log.info("Registered BeeCP({})as jmx-bean",poolName);
				} else {
					log.error("Jmx-name BeeCP({})has been exist in jmx server",poolName);
				}
			} catch (Exception e) {
				log.warn("Failed to register pool jmx-bean", e);
			}

			try {
				final ObjectName configRegName = new ObjectName("cn.beecp.BeeDataSourceConfig:type=BeeCP("+poolName+")-config");
				if (!mBeanServer.isRegistered(configRegName)) {
					mBeanServer.registerMBean(poolConfig,configRegName);
					log.info("Registered BeeCP({})config as jmx-bean",poolName);
				} else {
					log.error("Pool BeeCP({})config has been exist in jmx server",poolName);
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
				final ObjectName poolRegName = new ObjectName("cn.beecp.pool.RawConnectionPool:type=BeeCP("+poolName+")");
				if(mBeanServer.isRegistered(poolRegName)) {
					mBeanServer.unregisterMBean(poolRegName);
				}
			} catch (Exception e) {
				log.warn("Failed to unregister pool jmx-bean", e);
			}

			try {
				final ObjectName configRegName = new ObjectName("cn.beecp.BeeDataSourceConfig:type=BeeCP("+poolName+")-config");
				if(mBeanServer.isRegistered(configRegName)) {
					mBeanServer.unregisterMBean(configRegName);
				}
			} catch (Exception e) {
				log.warn("Failed to unregister pool jmx-bean", e);
			}
		}
	}
	//******************************** JMX **************************************//
}
