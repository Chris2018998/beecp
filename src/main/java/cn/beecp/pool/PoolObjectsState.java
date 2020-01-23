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
package cn.beecp.pool;

/**
 * pool objects state
 *
 * @author Chris.Liao
 * @version 1.0
 */

class PoolObjectsState {
	//POOL STATE
	static final int POOL_UNINIT            = 1;
	static final int POOL_NORMAL            = 2;
	static final int POOL_CLOSED            = 3;
	static final int POOL_RESTING           = 4;
	
	//POOLED CONNECTION STATE
	static final int CONNECTION_IDLE        = 1;
	static final int CONNECTION_USING       = 2;
	static final int CONNECTION_CLOSED      = 3;
	
	//WORK THREAD STATE
	static final int THREAD_WORKING         = 1;
	static final int THREAD_WAITING         = 2;
	static final int THREAD_DEAD            = 3;

	//BORROWER STATE
	static final Object BORROWER_NORMAL      = new Object();
	static final Object BORROWER_WAITING     = new Object();
	static final Object BORROWER_TIMEOUT     = new Object();
	static final Object BORROWER_INTERRUPTED = new Object();
}
