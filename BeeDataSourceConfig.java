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
package org.jmin.bee;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
/**
 * Connection pool configuration
 * 
 * @author Chris.Liao
 * @version 1.0
 */

public final class BeeDataSourceConfig {

	/**
	 * indicator to not allow to modify configuration after initialization
	 */
	private boolean inited;
	
	/**
	 *  User
	 */
	private String userName;

	/**
	 * Password
	 */
	private String password;
	
	/**
	 *  URL
	 */
	private String connectURL;
	
	/**
	 * driver class name
	 */
	private String driverClassName;

	/**
	 * connection driver
	 */
	private Driver connectDriver = null;

	/**
	 * connection extra properties
	 */
	private Properties connectProperties = new Properties();
	
	/**
	 * pool name
	 */
	private String poolName="Pool1";
	
	/**
	 * if true,first arrival,first taking if false,competition for all borrower
	 * to take idle connection
	 */
	private boolean fairMode;
	
	/**
	 * check on borrow connection
	 */
	private boolean checkOnBorrow;
	
	/**
	 * check on borrow connection
	 */
	private boolean checkOnReturn;
	
	/**
	 * pool initialization size
	 */
	private int poolInitSize = 0;
	
	/**
	 * pool allow max size
	 */
	private int poolMaxSize;
	
	/**
	 * pool concurrent Size
	 */
	private int poolConcurrentSize=Runtime.getRuntime().availableProcessors();
	
	/**
	 * 'PreparedStatement' cache size
	 */
	private int preparedStatementCacheSize = 10;

	/**
	 * borrower request timeout
	 */
	private long borrowerMaxWaitTime = 180000L;

	/**
	 * max idle time for pooledConnection(milliseconds),default value: three
	 * minutes
	 */
	private long connectionIdleTimeout = 180000L;

	/**
	 * a test SQL to check connection active state
	 */
	private String validationQuerySQL = "select 1 from dual";

	/**
	 * connection validate timeout:5 seconds
	 */
	private int validationQueryTimeout = 5;
	
	/**
	 * milliseconds,max inactive time to check active for borrower
	 */
	private long maxInactiveTimeToCheck = 1000L;
	
	/**
	 * BeeCP implementation class name
	 */
	private String poolImplementClassName = "org.jmin.bee.pool.ConnectionPool";
	

	public BeeDataSourceConfig(String driver, String url, String user, String password) {
		this.driverClassName = driver;
		this.connectURL = url;
		this.userName = user;
		this.password = password;
		this.inited = false;
	}

	public void setInited(boolean inited) {
		if (!this.inited)
			this.inited = inited;
	}

	public String getDriver() {
		return driverClassName;
	}

	public String getConnectURL() {
		return connectURL;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public void setDriverClassName(String driver) {
		if (!this.inited)
			this.driverClassName = driver;
	}

	public void setConnectionURL(String driverURL) {
		if (!this.inited)
			this.connectURL = driverURL;
	}

	public void setUserName(String userName) {
		if (!this.inited)
			this.userName = userName;
	}

	public void setPassword(String password) {
		if (!this.inited)
			this.password = password;
	}

	public Driver getConnectDriver() {
		return connectDriver;
	}

	public Properties getConnectProperties() {
		return new Properties(connectProperties);
	}

	public void addProperty(String key, String value) {
		if (!this.inited) {
			this.connectProperties.put(key, value);
		}
	}

	public void removeProperty(String key) {
		if (!this.inited) {
			this.connectProperties.remove(key);
		}
	}
	
	public boolean isFairMode() {
		return fairMode;
	}

	public void setFairMode(boolean fairMode) {
		if (!this.inited)
			this.fairMode = fairMode;
	}
	
	public boolean isCheckOnBorrow() {
		return checkOnBorrow;
	}

	public void setCheckOnBorrow(boolean checkOnBorrow) {
		if(!this.inited)
		this.checkOnBorrow = checkOnBorrow;
	}

	public boolean isCheckOnReturn() {
		return checkOnReturn;
	}

	public void setCheckOnReturn(boolean checkOnReturn) {
		if(!this.inited)
		this.checkOnReturn = checkOnReturn;
	}

	public String getPoolName() {
		return poolName;
	}

	public void setPoolName(String poolName) {
		if (!this.inited && !isNull(poolName)) {
			this.poolName = poolName;
		}
	}

	public int getPoolInitSize() {
		return poolInitSize;
	}

	public void setPoolInitSize(int poolInitSize) {
		if (!this.inited && poolInitSize >= 0) {
			this.poolInitSize = poolInitSize;
		}
	}

	public  int getPoolMaxSize() {
		return poolMaxSize;
	}

	public void setPoolMaxSize(int poolMaxSize) {
		if (!this.inited && poolMaxSize > 0) {
			this.poolMaxSize = poolMaxSize;
			this.poolConcurrentSize = poolMaxSize;
		}
	}
	
	public int getPoolConcurrentSize() {
		return poolConcurrentSize;
	}

	public void setPoolConcurrentSize(int poolConcurrentSize) {
	  if(!this.inited && poolConcurrentSize > 0) {
		this.poolConcurrentSize = poolConcurrentSize;
	  }
	}

	public  int getPreparedStatementCacheSize() {
		return preparedStatementCacheSize;
	}

	public void setPreparedStatementCacheSize(int statementCacheSize) {
		if (!this.inited && statementCacheSize >= 0) {
			this.preparedStatementCacheSize = statementCacheSize;
		}
	}

	public long getConnectionIdleTimeout() {
		return connectionIdleTimeout;
	}

	public void setConnectionIdleTimeout(long connectionIdleTimeout) {
		if (!this.inited && connectionIdleTimeout > 0) {
			this.connectionIdleTimeout = connectionIdleTimeout;
		}
	}

	public long getBorrowerMaxWaitTime() {
		return borrowerMaxWaitTime;
	}

	public void setBorrowerMaxWaitTime(long borrowerMaxWaitTime) {
		if (!this.inited && borrowerMaxWaitTime > 0) {
			this.borrowerMaxWaitTime = borrowerMaxWaitTime;
		}
	}

	public String getValidationQuerySQL() {
		return validationQuerySQL;
	}

	public void setValidationQuerySQL(String validationQuerySQL) {
		if (!this.inited && validationQuerySQL != null && validationQuerySQL.trim().length() > 0) {
			this.validationQuerySQL = validationQuerySQL;
		}
	}

	public int getValidationQueryTimeout() {
		return validationQueryTimeout;
	}

	public void setValidationQueryTimeout(int validationQueryTimeout) {
		if (!this.inited && validationQueryTimeout > 0) {
			this.validationQueryTimeout = validationQueryTimeout;
		}
	}

	public long getMaxInactiveTimeToCheck() {
		return maxInactiveTimeToCheck;
	}

	public void setMaxInactiveTimeToCheck(long maxInactiveTimeToCheck) {
		if (!this.inited && maxInactiveTimeToCheck > 0) {
			this.maxInactiveTimeToCheck = maxInactiveTimeToCheck;
		}
	}
	
	public String getPoolImplementClassName() {
		return poolImplementClassName;
	}
	public void setPoolImplementClassName(String poolImplementClassName) {
		if (!this.inited && poolImplementClassName != null && poolImplementClassName.trim().length() > 0) {
			this.poolImplementClassName = poolImplementClassName;
		}
	}
	
	private Driver loadJdbcDriver(String driverClassName) throws IllegalArgumentException {
		try {
			Class<?> driverClass = Class.forName(driverClassName,true,this.getClass().getClassLoader());
			Driver driver=(Driver)driverClass.newInstance();
			if(!driver.acceptsURL(this.connectURL))throw new InstantiationException();
			return driver;
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Driver class[" + driverClassName + "]not found");
		} catch (InstantiationException e) {
			throw new IllegalArgumentException("Driver class[" + driverClassName + "]can't be instantiated");
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Driver class[" + driverClassName + "]can't be instantiated",e);
		} catch (SQLException e) {
			throw new IllegalArgumentException("Driver class[" + driverClassName + "]can't be instantiated",e);
		}
	}
	
	/**
	 * check pool configuration
	 */
	public void check() {
		if (isNull(this.driverClassName))
			throw new IllegalArgumentException("Driver class name can't be null");
		if (isNull(this.connectURL))
			throw new IllegalArgumentException("Connect url can't be null");
		try {
			this.connectDriver = DriverManager.getDriver(this.connectURL);
		} catch (SQLException e) {}
		if(this.connectDriver==null)this.connectDriver=loadJdbcDriver(driverClassName);
		
		if (this.poolMaxSize <= 0)
			throw new IllegalArgumentException("Pool max size must be greater than zero");
		if (this.poolInitSize < 0)
			throw new IllegalArgumentException("Pool init size must be greater than zero");
		if (this.poolInitSize > poolMaxSize)
			throw new IllegalArgumentException("Error configeruation,pool initiation size must be less than pool max size");
		if (this.connectionIdleTimeout <= 0)
			throw new IllegalArgumentException("Connection max idle time must be greater than zero");
		if (this.borrowerMaxWaitTime <= 0)
			throw new IllegalArgumentException("Borrower max wait time must be greater than zero");
		if (this.preparedStatementCacheSize <= 0)
			throw new IllegalArgumentException("Statement cache size must be greater than zero");
		
		//fix issue:#1 The check of validationQuerySQL has logic problem. Chris-2019-05-01 begin
		//if (this.validationQuerySQL != null && validationQuerySQL.trim().length() == 0) {
		if (!isNull(this.validationQuerySQL) && !this.validationQuerySQL.trim().toLowerCase().startsWith("select "))
		//fix issue:#1 The check of validationQuerySQL has logic problem. Chris-2019-05-01 end	
			throw new IllegalArgumentException("Connection validate SQL must start with 'select '");
		//}

		if (!isNull(this.userName))
			this.connectProperties.put("user", this.userName);
		if (!isNull(this.password))
			this.connectProperties.put("password", this.password);
	}

	private boolean isNull(String value) {
		return (value == null || value.trim().length() == 0);
	}
}
