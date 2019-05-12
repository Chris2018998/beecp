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

/**
 * pooled connection state
 *
 * @author Chris.Liao
 * @version 1.0
 */

public final class PooledConnectionState {
	
	/**
	 * idle
	 */
	public static final int IDLE = 1;

	/**
	 * using
	 */
	public static final int USING = 2;
	
	/**
	 *closed
	 */
	public static final int CLOSED = 3;
	
}