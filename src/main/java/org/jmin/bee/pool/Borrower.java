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
import static org.jmin.bee.pool.PoolObjectsState.BORROWER_NORMAL;

import java.sql.Connection;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * pooled connection Borrower
 *
 * @author Chris.Liao
 * @version 1.0
 */
final class Borrower implements Callable<Connection>{
	private Thread thread;
	private ConnectionPool pool;
	private boolean hasHoldNewOne=false;
	private PooledConnection lastUsedConn;
	
	volatile Object stateObject;
	private final static AtomicReferenceFieldUpdater<Borrower,Object> updater = AtomicReferenceFieldUpdater.newUpdater(Borrower.class,Object.class,"stateObject");
	private long deadlineNanos;
	
	public Borrower(ConnectionPool pool) {
		this.pool=pool;
	}
	public Thread getThread() {
		return thread;
	}
	public Thread resetThread(){
		thread = Thread.currentThread();
		return thread;
	}
	public boolean equals(Object o){
		return this==o;
	}
	
	public void resetInBorrowing(){
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
	public Object getStateObject() {
		return updater.get(this);
	}
	public void resetStateObject() {
		updater.set(this,BORROWER_NORMAL);
	}
	public boolean compareAndSetStateObject(Object cur,Object exp) {
		return updater.compareAndSet(this,cur,exp);
	}
	
	public void setDeadlineNanos(long deadlineNanos){
		this.deadlineNanos=deadlineNanos;
	}
	public Connection call() throws Exception{
		return pool.takeOneConnection(deadlineNanos,this);
	}
}