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
package cn.bee.dbcp.pool;

import static cn.bee.dbcp.pool.PoolExceptionList.RequestInterruptException;
import static cn.bee.dbcp.pool.PoolExceptionList.RequestTimeoutException;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.bee.dbcp.BeeDataSourceConfig;
import cn.bee.dbcp.pool.util.ConnectionUtil;

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
	private Logger log = LoggerFactory.getLogger(this.getClass());
	private String poolName = "";
	private static String poolNamePrefix = "RawPool-";
	private static AtomicInteger poolNameIndex = new AtomicInteger(1);
	
	/**
	 * initialize pool with configuration
	 * 
	 * @param config
	 *            data source configuration
	 * @throws SQLException
	 *             check configuration fail or to create initiated connection
	 */
	public void init(BeeDataSourceConfig config) throws SQLException {
		poolConfig = config;
		DefaultMaxWaitMills = poolConfig.getMaxWait();
		poolSemaphore = new Semaphore(poolConfig.getConcurrentSize(), poolConfig.isFairMode());
		poolName = !ConnectionUtil.isNull(config.getPoolName()) ? config.getPoolName():poolNamePrefix + poolNameIndex.getAndIncrement();
	
		String mode = "";
		if (poolConfig.isFairMode()) {
			mode = "fair";
		} else {
			mode = "compete";
		}
		
		log.info("BeeCP(" + poolName + ")has been startup{init size:" + 0 + ",max size:"
				+ 0 + ",concurrent size:" + poolConfig.getConcurrentSize() + ",mode:" + mode
				+ ",max wait:" + poolConfig.getMaxWait() + "ms}");
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
	public void destroy() {
		unregisterJMX();
	}

	/******************************** JMX **************************************/
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
	// registerJMX
	private void registerJMX() {
		if (poolConfig.isEnableJMX()) {
			try {
				final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
				final ObjectName beanPoolName = new ObjectName("cn.bee.dbcp.pool.RawConnectionPool:type=" + poolName);
				if (!mBeanServer.isRegistered(beanPoolName)) {
					mBeanServer.registerMBean((ConnectionPoolJMXBean) this, beanPoolName);
					log.info("Registered BeeCP(" + poolName + ")as jmx-bean");
				} else {
					log.error("Jmx-name BeeCP(" + poolName + ")has been exist in jmx server");
				}
			} catch (Exception e) {
				log.warn("Failed to register pool jmx-bean", e);
			}
		}
	}

	// unregisterJMX
	private void unregisterJMX() {
		if (poolConfig.isEnableJMX()) {
			try {
				final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
				final ObjectName beanPoolName = new ObjectName("cn.bee.dbcp.pool.RawConnectionPool:type=" + poolName);
				if (!mBeanServer.isRegistered(beanPoolName)) {
					mBeanServer.unregisterMBean(beanPoolName);
				}
			} catch (Exception e) {
				log.warn("Failed to unregister pool jmx-bean", e);
			}
		}
	}
	/******************************** JMX **************************************/
}
