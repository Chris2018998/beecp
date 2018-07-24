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

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
 
/**
 * a connection pool implementation class
 * 
 * @author Chris
 * @version 1.0
 */
public final class FastConnectionPool2 extends FastConnectionPool {

	/**
	 * Transfer Queue
	 */
	private final LinkedTransferQueue<PooledConnection> transferQueue = new LinkedTransferQueue();

	/**
	 * clear waiters
	 */
	public void clearWaiters() {
		this.transferQueue.clear();
	}

	/**
	 * check whether exist waiters
	 */
	public boolean existWaiters() {
		return this.transferQueue.hasWaitingConsumer();
	}

	/**
	 * waiting transfer from other connection holders
	 */
	public PooledConnection waitTransferFromOthers(final long curMaxWaitTime) {
		try {
			return this.transferQueue.poll(curMaxWaitTime, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			return null;
		}
	}

	/**
	 * action after pooled connection release
	 */
	public void afterReleasePooledConnection(final PooledConnection pooledConnection) {
		if(!this.transferQueue.tryTransfer(pooledConnection))
			pooledConnection.setState(PooledConnectionState.IDLE);
	}
}