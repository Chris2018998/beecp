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

import java.sql.Connection;
import java.util.concurrent.Callable;

/**
 * pooled connection Borrower
 *
 * @author Chris.Liao
 * @version 1.0
 */
final class Borrower implements Callable<Connection>{
	Thread thread;
	private ConnectionPool pool;
	private boolean hasHoldNewOne=false;
	private PooledConnection lastUsedConn;
	
	volatile Object stateObject;
	private long deadlineNanos;
	
	public Borrower(ConnectionPool pool) {
		this.pool=pool;
		this.thread=Thread.currentThread();
	}
	public Thread resetThread(){
	  return(thread=Thread.currentThread());
	}
	public boolean equals(Object o){
		return this==o;
	}
	public void resetAsInBorrowing(){
		hasHoldNewOne=false;
	}
	public boolean isHasHoldNewOne() {
		return hasHoldNewOne;
	}
	
	public PooledConnection getLastUsedConn() {
		return lastUsedConn;
	}
	public void setLastUsedConn(PooledConnection pConn) {
		lastUsedConn = pConn;
		hasHoldNewOne=true;
	}
	public void setDeadlineNanos(long deadlineNanos){
		this.deadlineNanos=deadlineNanos;
	}
	public Connection call() throws Exception{
		return pool.takeOneConnection(deadlineNanos,this);
	}
}