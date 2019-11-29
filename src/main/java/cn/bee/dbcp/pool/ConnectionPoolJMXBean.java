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

/**
 *  Pool JMX Bean interface
 *
 * @author Chris.Liao
 * @version 1.0
 */
public interface ConnectionPoolJMXBean {
	
	/**
	 * reset pool 
	 */
	public void reset();
	
	/**
	 * reset pool 
	 * @param force true close connection immediately
	 */
	public void reset(boolean force);
	
	//return connection total size in pool
	public int getConnTotalSize();
	 
	//return connection idle size in pool
	public int getConnIdleSize();
	
	//return connection active size in pool
	public int getConnUsingSize();

	public int getSemaphoreAcquiredSize();

	public int getSemaphoreWatingSize();

	public int getTransferWatingSize();

}

