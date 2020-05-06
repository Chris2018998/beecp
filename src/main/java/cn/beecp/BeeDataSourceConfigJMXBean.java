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
package cn.beecp;

/**
 * Bee DataSourceConfig JMX Bean interface
 * 
 * @author Chris.Liao
 * @version 1.0
 */

public interface BeeDataSourceConfigJMXBean {

	String getUsername();

	String getUrl();

	String getDriverClassName();

	String getConnectionFactoryClassName();

	String getPoolName();

	boolean isFairMode();

	int getInitialSize();

	int getMaxActive();

	int getConcurrentSize();

	int getPreparedStatementCacheSize();

	boolean isDefaultAutoCommit();

	String getDefaultTransactionIsolation();

	int getDefaultTransactionIsolationCode();

	String getDefaultCatalog();

	boolean isDefaultReadOnly();

	long getMaxWait();

	long getIdleTimeout();

	long getHoldIdleTimeout();

	String getConnectionTestSQL();

	int getConnectionTestTimeout();

	long getConnectionTestInterval();

	boolean isForceCloseConnection();

	long getWaitTimeToClearPool();

	long getIdleCheckTimeInterval();

	long getIdleCheckTimeInitDelay();

	String getPoolImplementClassName();

	boolean isEnableJMX();
}
