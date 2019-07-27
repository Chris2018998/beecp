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
package org.jmin.bee.pool;

import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.jmin.bee.BeeDataSourceConfig;

/**
 * JDBC Connection Pool Implementation(asynchronous)
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class ConnectionPool2 extends ConnectionPool{
	private final LinkedBlockingQueue<Runnable> taskQuue;
	private final ThreadPoolExecutor asynTakeExecutor;
 
	public ConnectionPool2(BeeDataSourceConfig poolInfo) throws SQLException {
		super(poolInfo);
		taskQuue=new LinkedBlockingQueue<Runnable>();
		asynTakeExecutor=new ThreadPoolExecutor(info.getPoolConcurrentSize(),info.getPoolConcurrentSize(),5,SECONDS,taskQuue);
	}
	
	protected boolean existBorrower() {
		return taskQuue.size() > 0;
	}

	protected Connection getConnection(long wait, Borrower borrower) throws SQLException {
		try {
			wait = wait * TO_NANO_BASE;
			borrower.setDeadlineNanos(nanoTime()+wait);
			Future<Connection> taskFuture = asynTakeExecutor.submit(borrower);
			return taskFuture.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw RequestInterruptException;
		} catch (ExecutionException e) {
			if (e.getCause() instanceof SQLException) {
				throw (SQLException) e.getCause();
			} else {
				throw new SQLException("Request failed", e.getCause());
			}
		}
	}

	public void destroy() {
		super.destroy();
		asynTakeExecutor.shutdown();
	}
}
