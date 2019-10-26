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

import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import cn.bee.dbcp.BeeDataSourceConfig;

/**
 * JDBC Connection Pool Implementation(asynchronous)
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class ConnectionPool2 extends ConnectionPool{
	private final LinkedBlockingQueue<Runnable> taskQueue;
	private final ThreadPoolExecutor asynTakeExecutor;
 
	public ConnectionPool2(BeeDataSourceConfig poolInfo) throws SQLException {
		super(poolInfo);
		taskQueue=new LinkedBlockingQueue<Runnable>();
		asynTakeExecutor=new ThreadPoolExecutor(poolConfig.getConcurrentSize(),poolConfig.getConcurrentSize(),5,SECONDS,taskQueue);
	}
	
	protected boolean existBorrower() {
		return taskQueue.size()+asynTakeExecutor.getActiveCount()>0;
	}

	protected Connection getConnection(long wait, Borrower borrower)throws SQLException,InterruptedException {
		try {
			wait=MILLISECONDS.toNanos(wait);
			borrower.setDeadlineNanos(nanoTime()+wait);
			Future<Connection> taskFuture = asynTakeExecutor.submit(borrower);
			Connection con=taskFuture.get();
			if(con!=null)return con;
			
			if(Thread.currentThread().isInterrupted())
			throw RequestInterruptException;
			throw RequestTimeoutException;
		} catch (ExecutionException e) {
			 Throwable cause=  e.getCause();
			if(cause instanceof SQLException) {
				throw (SQLException)cause;
			}else if(cause instanceof InterruptedException){
				throw (InterruptedException)cause;
			} else {
				throw new SQLException("Failed to take connection",cause);
			}
		}
	}

	public void destroy() {
		super.destroy();
		taskQueue.clear();
		asynTakeExecutor.shutdown();
	}
}
