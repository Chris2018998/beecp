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
package org.jmin.bee;

import java.sql.SQLException;
import java.util.Properties;
/**
 * Connection pool configuration
 * 
 * @author Chris
 * @version 1.0
 */

public abstract class BeeDataSourceConfig {

	/**
	 * indicator to not allow to modify configuration after initialization
	 */
	private boolean inited = false;

	/**
	 * pool initialization size
	 */
	private int poolInitSize = 0;

	/**
	 * pool allow max size
	 */
	private int poolMaxSize = 10;

	/**
	 * 'PreparedStatement' cache size
	 */
	private int preparedStatementCacheSize = 20;

	/**
	 * max idle time for pooledConnection(milliseconds),default value: three
	 * minutes
	 */
	private long connectionIdleTimeout = 180000;

	/**
	 * borrower request timeout
	 */
	private long borrowerMaxWaitTime = 180000;

	/**
	 * a test SQL to check connection active state
	 */
	private String connectionValidateSQL = "";
	
	/**
	 *  connection validate timeout:5 seconds
	 */
	private int connectionValidateTimeout=5;
	
	/**
	 * must check time for connection//nanoseconds
	 */
	private long needCheckTimeAfterActive=1000;
	
	/**
	 * BeeCP implementation class name
	 */
	private String connectionPoolClassName = "org.jmin.bee.impl.FastConnectionPool";
	
	/**
	 * JDBC extra properties
	 */
	private Properties jdbcProperties= new Properties();

	/**
	 * indicator to not allow to modify configuration after initialization
	 */
	public void setInited(boolean inited) {
		if (!this.inited)
			this.inited = inited;
	}

	/**
	 * pool initialization size
	 */
	public int getPoolInitSize() {
		return poolInitSize;
	}

	/**
	 * pool initialization size
	 */
	public void setPoolInitSize(int poolInitSize) {
		if (!this.inited && poolInitSize > 0) {
			this.poolInitSize = poolInitSize;
		}
	}

	/**
	 * pool allow max size
	 */
	public int getPoolMaxSize() {
		return poolMaxSize;
	}

	/**
	 * pool allow max size
	 */
	public void setPoolMaxSize(int poolMaxSize) {
		if (!this.inited && poolMaxSize > 0) {
			this.poolMaxSize = poolMaxSize;
		}
	}

	/**
	 * 'PreparedStatement' cache size
	 */
	public int getPreparedStatementCacheSize() {
		return preparedStatementCacheSize;
	}

	/**
	 * 'PreparedStatement' cache size
	 */
	public void setPreparedStatementCacheSize(int statementCacheSize) {
		if (!this.inited && statementCacheSize > 0) {
			this.preparedStatementCacheSize = statementCacheSize;
		}
	}

	/**
	 * max idle time for pooledConnection(milliseconds)
	 */
	public long getConnectionIdleTimeout() {
		return connectionIdleTimeout;
	}

	/**
	 * max idle time for pooledConnection(milliseconds)
	 */
	public void setConnectionIdleTimeout(long connectionIdleTimeout) {
		if (!this.inited && connectionIdleTimeout > 0) {
			this.connectionIdleTimeout = connectionIdleTimeout;
		}
	}

	/**
	 * borrower request timeout
	 */
	public long getBorrowerMaxWaitTime() {
		return borrowerMaxWaitTime;
	}

	/**
	 * borrower request timeout
	 */
	public void setBorrowerMaxWaitTime(long borrowerMaxWaitTime) {
		if (!this.inited && borrowerMaxWaitTime > 0) {
			this.borrowerMaxWaitTime = borrowerMaxWaitTime;
		}
	}

	/**
	 * a test SQL to check connections active state
	 */
	public String getConnectionValidateSQL() {
		return connectionValidateSQL;
	}

	/**
	 * a test SQL to check connections active state
	 */
	public void setConnectionValidateSQL(String connectionTestQuerySql) {
		if (!this.inited && connectionTestQuerySql != null && connectionTestQuerySql.trim().length() > 0) {
			this.connectionValidateSQL = connectionTestQuerySql;
		}
	}
	
	/**
	 * connection validate timeout:5 seconds
	 */
	public int getConnectionValidateTimeout() {
		return connectionValidateTimeout;
	}

	/**
	 * connection validate timeout:5 seconds
	 */
	public void setConnectionValidateTimeout(int connectionValidateTimeout) {
		if (!this.inited && connectionValidateTimeout > 0) {
			this.connectionValidateTimeout = connectionValidateTimeout;
		}
	}

	/**
	 * must check time for connection//nanoseconds
	 */
	public long getNeedCheckTimeAfterActive() {
		return needCheckTimeAfterActive;
	}
	
	/**
	 * must check time for connection//nanoseconds
	 */
	public void setNeedCheckTimeAfterActive(long needCheckTimeAfterActive) {
		if (!this.inited && needCheckTimeAfterActive > 0) {
		 this.needCheckTimeAfterActive=needCheckTimeAfterActive;
		}
	}

	/**
	 * BeeCP implementation class name
	 */
	public String getConnectionPoolClassName() {
		return connectionPoolClassName;
	}

	/**
	 * BeeCP implementation class name
	 */
	public void setConnectionPoolClassName(String connectionPoolClassName) {
		if (!this.inited && connectionPoolClassName != null && connectionPoolClassName.trim().length() > 0) {
			this.connectionPoolClassName = connectionPoolClassName;
		}
	}
	
	/**
	 * JDBC extra properties
	 */
	public void addProperty(String key,String value){
		if (!this.inited){
			this.jdbcProperties.put(key, value);
		}
	}

	/**
	 * JDBC extra properties
	 */
	public void removeProperty(String key){
		if (!this.inited){
			this.jdbcProperties.remove(key);
		}
	}
	
	/**
	 * JDBC extra properties
	 */
	public Properties getExtraProperties(){
		 return new Properties(this.jdbcProperties);
	}

	/**
	 * return an Connection Factory, support two kind of connection source: JDBC and JNDI
	 */
	public abstract ConnectionFactory getConnectionFactory() throws SQLException;

	/**
	 * check configuration,if failed,throw some exceptions
	 */
	public void check() throws SQLException {
		if (this.poolMaxSize <= 0)
			throw new SQLException("Pool max size must be greater than zero");
		if (this.poolInitSize < 0)
			throw new SQLException("Pool init size must be greater than zero");
		if (this.poolInitSize > poolMaxSize)
			throw new SQLException("Error configeruation,pool init size must be less than pool max size");
		if (this.connectionIdleTimeout <= 0)
			throw new SQLException("Connection max idle time must be greater than zero");
		if (this.borrowerMaxWaitTime <= 0)
			throw new SQLException("Connection max waiting time must be greater than zero");
		if (this.preparedStatementCacheSize < 0)
			throw new SQLException("Statement cache Size must be greater than zero");
		if (this.connectionPoolClassName == null || connectionPoolClassName.trim().length() == 0)
			throw new SQLException("Connection pool implmentation class name can't be null");
		if(this.connectionValidateSQL != null  &&  connectionValidateSQL.trim().length() == 0){
			if(this.connectionValidateSQL.toLowerCase().startsWith("select "))
				throw new SQLException("connection calidate SQL must start with 'select '");
		}
	}
}
