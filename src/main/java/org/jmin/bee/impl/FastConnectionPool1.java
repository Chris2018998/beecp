/*
 * Copyright (C) Chris Liao
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
package org.jmin.bee.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * a connection pool implementation class
 *
 * @author Chris
 * @version 1.0
 */
public final class FastConnectionPool1 extends FastConnectionPool {

	/**
	 * Queue to Transfer pooled connection to waiters
	 */
	private final LinkedBlockingQueue<WaitingBorrower> waitingBorrowerQueue = new LinkedBlockingQueue();

	/**
	 * clear waiters 
	 */
	public void clearWaiters() {
		WaitingBorrower waitingBorrower = null;
		while ((waitingBorrower = this.waitingBorrowerQueue.poll()) != null) {
			waitingBorrower.setPooledConnection(null);
		}
	}

	/**
	 * check whether exist waiters
	 */
	public boolean existWaiters() {
		return !this.waitingBorrowerQueue.isEmpty();
	}

	/**
	 * waiting transfer from other connection holders
	 */
	public PooledConnection waitTransferFromOthers(final long curMaxWaitTime) {
		final WaitingBorrower waiter = new WaitingBorrower();
		try {
			if(this.waitingBorrowerQueue.offer(waiter)){
				waiter.awaitNanoSeconds(curMaxWaitTime);
				return waiter.getPooledConnection();
			}else{
				return null;
			}
		} catch (InterruptedException e) {
			this.waitingBorrowerQueue.remove(waiter);
			return null;
		}
	}

	/**
	 * action after pooled connection release
	 */
	public void afterReleasePooledConnection(final PooledConnection pooledConnection){// 外部归还
	  WaitingBorrower waiter=this.waitingBorrowerQueue.poll();
		if (waiter != null) {
			waiter.setPooledConnection(pooledConnection);
		}else{
			pooledConnection.setState(PooledConnectionState.IDLE);
		}
	}

	/**
	 * Waiting Borrower 
	 */
	class WaitingBorrower {
		private PooledConnection pooledConnection;
		private CountDownLatch countLatch = new CountDownLatch(1);
		public PooledConnection getPooledConnection() {
			return this.pooledConnection;
		}
		public void reset() {
			this.pooledConnection = null;
			this.countLatch = new CountDownLatch(1);
		}
		public void setPooledConnection(final PooledConnection pooledConnection) {
			this.pooledConnection = pooledConnection;
			this.countLatch.countDown();
		}
		public boolean awaitNanoSeconds(final long time) throws InterruptedException {
			return this.countLatch.await(time, TimeUnit.NANOSECONDS);
		}
	}
}