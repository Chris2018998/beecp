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
 * work thread state
 *
 * @author Chris.Liao
 * @version 1.0
 */

class PoolObjectsState {
	
	//POOL STATE
	public static final int POOL_UNINIT         = 0;
	public static final int POOL_NORMAL         = 1;
	public static final int POOL_CLOSED         = 2;
 
	//POOLED CONNECTION STATE
	public static final int CONNECTION_IDLE      = 0;
	public static final int CONNECTION_USING     = 1;
	public static final int CONNECTION_CLOSED    = 2;
	
	//BORROWER THREAD ACTIVE STATE
	public static final int BORROWER_NORMAL       = 0;
	public static final int BORROWER_WAITING      = 1;
	public static final int BORROWER_TIMEOUT	  = 2;
	public static final int BORROWER_TRANSFERED   = 3;

	//WORK THREAD STATE
	public static final int THREAD_NORMAL         = 0;
	public static final int THREAD_WAITING        = 1;
	public static final int THREAD_WORKING        = 2;
	public static final int THREAD_DEAD           = 3;   
}
