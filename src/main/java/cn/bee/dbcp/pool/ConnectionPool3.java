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

import java.sql.Connection;
import java.sql.SQLException;
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
public final class ConnectionPool3 extends ConnectionPool {
	protected final Semaphore poolSemaphore;
	public ConnectionPool3(BeeDataSourceConfig poolInfo) throws SQLException {
		super(poolInfo);
		poolSemaphore=new Semaphore(poolConfig.getConcurrentSize(),poolConfig.isFairQueue());
	}
	
	//do nothing
	protected void createInitConns() throws SQLException {}

	/**
	 * borrow raw connection from pool
	 * 
	 * @param wait
	 *            max wait time for borrower
	 * @return If exists idle connection in pool,then return one;if not, waiting
	 *         until other borrower release
	 * @throws SQLException
	 *             if pool is closed or waiting timeout,then throw exception
	 */
	public Connection getConnection(long wait) throws SQLException {
		try {
			wait=MILLISECONDS.toNanos(wait);
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
}
