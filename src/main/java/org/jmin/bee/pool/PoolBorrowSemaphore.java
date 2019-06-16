/*
 * Copyright Chris Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package org.jmin.bee.pool;

import java.util.Collection;
import java.util.concurrent.Semaphore;

/**
 * Pool connection take semaphore
 * 
 * @author Chris
 */
@SuppressWarnings("serial")
public class PoolBorrowSemaphore extends Semaphore {
	public PoolBorrowSemaphore(int permits, boolean fair) {
		super(permits, fair);
	}

	public Collection<Thread> getQueuedThreads() {
		return super.getQueuedThreads();
	}
}
