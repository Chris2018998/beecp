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
package cn.bee.dbcp;

/**
 * Bee DataSourceConfig JMX Bean interface
 * 
 * @author Chris.Liao
 * @version 1.0
 */

public interface BeeDataSourceConfigJMXBean {
	
	public String getUsername();
 
	public String getUrl();
	
	public String getDriverClassName();
	
	public String getConnectionFactoryClassName();
	
	public String getPoolName();
	
	public boolean isFairMode();

	public int getInitialSize();
	
	public int getMaxActive();
	
	public int getConcurrentSize();
	
	public int getPreparedStatementCacheSize();
	
	public boolean isTestOnBorrow();
	
	public boolean isTestOnReturn();
	
	public boolean isDefaultAutoCommit();

	public int getDefaultTransactionIsolation();

	public String getDefaultCatalog();
	
	public boolean isDefaultReadOnly();
	
	public long getMaxWait();
	
	public long getIdleTimeout();
	
	public long getHoldIdleTimeout();
	
	public String getValidationQuery();
	
	public int getValidationQueryTimeout();
	
	public long getValidationInterval();

	public boolean isForceCloseConnection();
	
	public long getWaitTimeToClearPool();
	
	public long getIdleCheckTimePeriod();
	
	public long getIdleCheckTimeInitDelay();

	public String getPoolImplementClassName();
	
	public boolean isEnableJMX();
}
