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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static cn.bee.dbcp.pool.PoolExceptionList.RequestTimeoutException;
import static cn.bee.dbcp.pool.PoolExceptionList.RequestInterruptException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import cn.bee.dbcp.BeeDataSourceConfig;

/**
 * JDBC Connection Pool Implementation,which 
 * 
 * return raw connections to borrowers directly.
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public final class RawConnectionPool implements ConnectionPool {
	private Semaphore poolSemaphore;
	private long DefaultMaxWaitMills;
	private BeeDataSourceConfig poolConfig;
	private Map<String,Integer> snapshotMap;
	
	/**
	 * initialize connection pool
	 * @param config
	 * @throws SQLException
	 */
	public void init(BeeDataSourceConfig config)throws SQLException{
		poolConfig=config;
		DefaultMaxWaitMills=poolConfig.getMaxWait();
		snapshotMap = new LinkedHashMap<String,Integer>();
		poolSemaphore=new Semaphore(poolConfig.getConcurrentSize(),poolConfig.isFairQueue());
		
		snapshotMap.put("PoolMaxSize",poolConfig.getMaxActive());
		snapshotMap.put("concurrentSize",poolConfig.getConcurrentSize());
		snapshotMap.put("ConCurSize",0);
		snapshotMap.put("ConIdleSize",0);
	}
	
	/**
	 * borrow a connection from pool
	 * @return If exists idle connection in pool,then return one;if not, waiting until other borrower release
	 * @throws SQLException if pool is closed or waiting timeout,then throw exception
	 */
	public Connection getConnection() throws SQLException{
		return getConnection(DefaultMaxWaitMills);
	}

	/**
	 * borrow one connection from pool
	 * 
	 * @param wait must be greater than zero
	 *             
	 * @return If exists idle connection in pool,then return one;if not, waiting
	 *         until other borrower release
	 * @throws SQLException
	 *             if pool is closed or waiting timeout,then throw exception
	 */
	public Connection getConnection(long wait) throws SQLException{
		try {
			wait = MILLISECONDS.toNanos(wait);
			if (poolSemaphore.tryAcquire(wait,NANOSECONDS)) {
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
	 * @param pConn target connection need release
	 * @param needTest, true check active
	 */
	public void release(PooledConnection pConn,boolean needTest){}
	
	/**
	 * get pool snapshot
	 * 
	 * @return pool current info
	 */
	public Map<String,Integer> getPoolSnapshot()throws SQLException{
		return snapshotMap;
	}
	
	/**
	 * close pool
	 */
	public void destroy(){}
}
