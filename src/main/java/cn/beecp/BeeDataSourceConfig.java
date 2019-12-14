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

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static cn.beecp.util.BeecpUtil.isNullText;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import cn.beecp.pool.JdbcConnectionFactory;

/**
 * Connection pool configuration
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public class BeeDataSourceConfig implements BeeDataSourceConfigJMXBean{

	/**
	 * indicator to not allow to modify configuration after initialization
	 */
	private boolean checked;

	/**
	 *  User
	 */
	private String username;

	/**
	 * Password
	 */
	private String password;
	
	/**
	 *  URL
	 */
	private String jdbcUrl;
	
	/**
	 * driver class name
	 */
	private String driverClassName; 
	
	/**
	 * pool name
	 */
	private String poolName;
	
	/**
	 * if true,first arrival,first taking if false,competition for all borrower
	 * to take idle connection
	 */
	private boolean fairMode;
	
	/**
	 * pool initialization size
	 */
	private int initialSize;
	
	/**
	 * pool allow max size
	 */
	private int maxActive=10;
	
	/**
	 * pool concurrent Size
	 */
	private int concurrentSize;
	
	/**
	 * 'PreparedStatement' cache size
	 */
	private int preparedStatementCacheSize = 16;
	
	/**
	 * check on borrow connection
	 */
	private boolean testOnBorrow=true;
	
	/**
	 * check on borrow connection
	 */
	private boolean testOnReturn;

	/**
	 * connection.setAutoCommit(boolean);
	 */
	private boolean defaultAutoCommit=true;
	
	/**
	 * clear SQLWarnings before return to pool
	 */
	private boolean clearSQLWarnings;

	/**
	 * default Transaction Isolation
	 */
	private int defaultTransactionIsolation;
	
	/**
	 *connection.setCatalog
	 */
	private String defaultCatalog;
	
	/**
	 * connection.setReadOnly
	 */
	private boolean defaultReadOnly;
 
	/**
	 * borrower request timeout(milliseconds)
	 */
	protected long maxWait=SECONDS.toMillis(8);

	/**
	 * max idle time for pooledConnection(milliseconds),default value: three minutes
	 * minutes
	 */
	private long idleTimeout=MINUTES.toMillis(3);
	
	/** 
	 * max hold time in Unused(milliseconds),pool will release it by forced 
	 */
	private long holdIdleTimeout=MINUTES.toMillis(5);
	
	/**
	 * a test SQL to check connection active state
	 */
	private String connectionTestSQL = "select 1 from dual";

	/**
	 * connection validate timeout:5 seconds
	 */
	private int connectionTestTimeout = 5;
	 
	/**
	 * milliseconds,max inactive time to check active for borrower
	 */
	private long connectionTestInterval = 500L;
	
	/**
	 * close all connections in force when shutdown
	 */
	private boolean forceCloseConnection;
	
	/**
	 * seconds,wait for retry to clear all connections
	 */
	private long waitTimeToClearPool=3;
	
	/**
	 * milliseconds,idle Check Time Period 
	 */
	private long idleCheckTimeInterval=MINUTES.toMillis(3);
	
	/**
	 * milliseconds,idle Check Time initialize delay
	 */
	private long idleCheckTimeInitDelay=SECONDS.toMillis(1);

	/**
	 * BeeCP implementation class name
	 */
	private String poolImplementClassName = DefaultImplementClassName;
	
	/**
	 * Physical JDBC Connection factory class name
	 */
	private String connectionFactoryClassName;

	/**
	 * Physical JDBC Connection factory
	 */
	private ConnectionFactory connectionFactory;
	
	/**
	 * connection extra properties
	 */
	private Properties connectProperties = new Properties();
	
	/**
	 * enableJMX
	 */
	private boolean enableJMX;
	
	/**
	 * Default implementation class name
	 */
	static final String DefaultImplementClassName = "cn.beecp.pool.FastConnectionPool";
	
	public BeeDataSourceConfig() {
      this(null,null,null,null);
	}
	public BeeDataSourceConfig(String driver, String url, String user, String password) {
		this.jdbcUrl = url;
		this.username = user;
		this.password = password;
		this.driverClassName = driver;
		concurrentSize =Runtime.getRuntime().availableProcessors();
		defaultTransactionIsolation=Connection.TRANSACTION_READ_COMMITTED;
	}
	
	boolean isChecked() {
		return checked;
	}
	void setAsChecked() {
		if(!this.checked)
			this.checked = true;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		if(!this.checked) 
		 this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		if (!this.checked)
		this.password = password;
	}
	public String getUrl() {
		return jdbcUrl;
	}
	public void setUrl(String jdbcUrl) {
		if(!this.checked && !isNullText(jdbcUrl))
		this.jdbcUrl = jdbcUrl;
	}
	public void setJdbcUrl(String jdbcUrl) {
		if(!this.checked && !isNullText(jdbcUrl))
		this.jdbcUrl = jdbcUrl;
	}
	public String getDriverClassName() {
		return driverClassName;
	}
	public void setDriverClassName(String driverClassName) {
		if(!this.checked && !isNullText(driverClassName))
		this.driverClassName = driverClassName;
	}
	public String getConnectionFactoryClassName() {
		return connectionFactoryClassName;
	}
	public void setConnectionFactoryClassName(String connectionFactoryClassName) {
		if(!this.checked && !isNullText(connectionFactoryClassName))
		 this.connectionFactoryClassName = connectionFactoryClassName;
	}
	public ConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		if(!this.checked)
		this.connectionFactory = connectionFactory;
	}
	 
	public String getPoolName() {
		return poolName;
	}
	public void setPoolName(String poolName) {
		if(!this.checked && !isNullText(poolName))
		this.poolName = poolName;
	}
	public boolean isFairMode() {
		return fairMode;
	}
	public void setFairMode(boolean fairMode) {
		if(!this.checked)
		this.fairMode = fairMode;
	}
	public int getInitialSize() {
		return initialSize;
	}
	public void setInitialSize(int initialSize) {
		if(!this.checked && initialSize>0)
		this.initialSize = initialSize;
	}
	public int getMaxActive() {
		return maxActive;
	}
	public void setMaxActive(int maxActive) {
		if(!this.checked && maxActive>0)
		this.maxActive = maxActive;
	}
	public int getConcurrentSize() {
		return concurrentSize;
	}
	public void setConcurrentSize(int concurrentSize) {
		if(!this.checked && concurrentSize>0)
		this.concurrentSize = concurrentSize;
	}
	public int getPreparedStatementCacheSize() {
		return preparedStatementCacheSize;
	}
	public void setPreparedStatementCacheSize(int preparedStatementCacheSize) {
		if(!this.checked && preparedStatementCacheSize>0)
		this.preparedStatementCacheSize = preparedStatementCacheSize;
	}
	public boolean isTestOnBorrow() {
		return testOnBorrow;
	}
	public void setTestOnBorrow(boolean testOnBorrow) {
		if(!this.checked)
		this.testOnBorrow = testOnBorrow;
	}
	public boolean isTestOnReturn() {
		return testOnReturn;
	}
	public void setTestOnReturn(boolean testOnReturn) {
		if(!this.checked)
		this.testOnReturn = testOnReturn;
	}
	public boolean isDefaultAutoCommit() {
		return defaultAutoCommit;
	}
	public void setDefaultAutoCommit(boolean defaultAutoCommit) {
		if(!this.checked)this.defaultAutoCommit = defaultAutoCommit;
	}
	public boolean isClearSQLWarnings() {
		return clearSQLWarnings;
	}
	public void setClearSQLWarnings(boolean clearSQLWarnings) {
	 if(!this.checked)
		this.clearSQLWarnings = clearSQLWarnings;
	}
	public int getDefaultTransactionIsolation() {
		return defaultTransactionIsolation;
	}
	public void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
		if(!this.checked && defaultTransactionIsolation>=0)
		this.defaultTransactionIsolation = defaultTransactionIsolation;
	}
	
	public String getDefaultCatalog() {
		return defaultCatalog;
	}
	public void setDefaultCatalog(String catalog) {
	  if(!isNullText(catalog))
		this.defaultCatalog = catalog;
	}
	public boolean isDefaultReadOnly() {
		return defaultReadOnly;
	}
	public void setDefaultReadOnly(boolean readOnly) {
	   if(!this.checked)
		this.defaultReadOnly = readOnly;
	}
	public long getMaxWait() {
		return maxWait;
	}
	public void setMaxWait(long maxWait) {
	  if(!this.checked && maxWait>0) 
		this.maxWait = maxWait;
	}
	public long getIdleTimeout() {
		return idleTimeout;
	}
	public void setIdleTimeout(long idleTimeout) {
	  if(!this.checked && idleTimeout>0) 
		this.idleTimeout = idleTimeout;
	}
	public long getHoldIdleTimeout() {
		return holdIdleTimeout;
	}
	public void setHoldIdleTimeout(long maxHoldTimeInUnused) {
		if(!this.checked && maxHoldTimeInUnused>0) 
		this.holdIdleTimeout = maxHoldTimeInUnused;
	}
	public String getConnectionTestSQL() {
		return connectionTestSQL;
	}
	public void setConnectionTestSQL(String validationQuery) {
	if (!this.checked && !isNullText(validationQuery)) 
		this.connectionTestSQL = validationQuery;
	}
	public int getConnectionTestTimeout() {
		return connectionTestTimeout;
	}
	public void setConnectionTestTimeout(int connectionTestTimeout) {
		if(!this.checked && connectionTestTimeout>0) 
		this.connectionTestTimeout = connectionTestTimeout;
	}
	public long getConnectionTestInterval() {
		return connectionTestInterval;
	}
	public void setConnectionTestInterval(long connectionTestInterval) {
		if(!this.checked && connectionTestInterval>0) 
		this.connectionTestInterval = connectionTestInterval;
	}

	public boolean isForceCloseConnection() {
		return forceCloseConnection;
	}
	public void setForceCloseConnection(boolean forceCloseConnection) {
       if(!this.checked)
		this.forceCloseConnection = forceCloseConnection;
	}
	
	public long getWaitTimeToClearPool() {
		return waitTimeToClearPool;
	}
	public void setWaitTimeToClearPool(long waitTimeToClearPool) {
	  if(!this.checked && waitTimeToClearPool>=0)
		this.waitTimeToClearPool = waitTimeToClearPool;
	}

	public long getIdleCheckTimeInterval() {
		return idleCheckTimeInterval;
	}
	public void setIdleCheckTimeInterval(long idleCheckTimeInterval) {
		if(!this.checked && idleCheckTimeInterval>=1000L)
		this.idleCheckTimeInterval = idleCheckTimeInterval;
	}
	public long getIdleCheckTimeInitDelay() {
		return idleCheckTimeInitDelay;
	}
	public void setIdleCheckTimeInitDelay(long idleCheckTimeInitDelay) {
		if(!this.checked && idleCheckTimeInitDelay>=1000L)
		this.idleCheckTimeInitDelay = idleCheckTimeInitDelay;
	}
	public String getPoolImplementClassName() {
		return poolImplementClassName;
	}
	public void setPoolImplementClassName(String poolImplementClassName) {
		if (!this.checked &&!isNullText(poolImplementClassName)) {
			this.poolImplementClassName = poolImplementClassName;
		}
	}
	public void removeConnectProperty(String key){
		if(!this.checked){
			connectProperties.remove(key);
		}
	}
	
	public void addConnectProperty(String key,String value){
		if(!this.checked){
			connectProperties.put(key, value);
		}
	}

	public boolean isEnableJMX() {
		return enableJMX;
	}
	public void setEnableJMX(boolean enableJMX) {
	  if(!this.checked)
		this.enableJMX = enableJMX;
	}
	void copyTo(BeeDataSourceConfig config){
		if(!config.checked){
			config.username=this.username;
			config.password=this.password;
			config.jdbcUrl=this.jdbcUrl;
			config.driverClassName=this.driverClassName; 
			config.connectionFactoryClassName=this.connectionFactoryClassName;
			config.connectionFactory=this.connectionFactory;
			config.connectProperties=new Properties(this.connectProperties);
			config.poolName=this.poolName;
			config.fairMode=this.fairMode;
			config.initialSize=this.initialSize;
			config.maxActive=this.maxActive;
			config.concurrentSize=this.concurrentSize;
			config.preparedStatementCacheSize=this.preparedStatementCacheSize;
			config.testOnBorrow=this.testOnBorrow;
			config.testOnReturn=this.testOnReturn;
			config.defaultAutoCommit=this.defaultAutoCommit;
			config.defaultTransactionIsolation=this.defaultTransactionIsolation;
			config.defaultCatalog=this.defaultCatalog;
			config.defaultReadOnly=this.defaultReadOnly;
			config.maxWait=this.maxWait;
			config.idleTimeout=this.idleTimeout;
			config.holdIdleTimeout=this.holdIdleTimeout;
			config.connectionTestSQL=this.connectionTestSQL;
			config.connectionTestTimeout=this.connectionTestTimeout;
			config.connectionTestInterval=this.connectionTestInterval;
			config.forceCloseConnection=this.forceCloseConnection;
			config.waitTimeToClearPool=this.waitTimeToClearPool;
			config.idleCheckTimeInitDelay=this.idleCheckTimeInitDelay;
			config.idleCheckTimeInterval=this.idleCheckTimeInterval;
			config.poolImplementClassName=this.poolImplementClassName;
		}
	}
	
	private Driver loadJdbcDriver(String driverClassName) throws IllegalArgumentException {
		try {
			Class<?> driverClass = Class.forName(driverClassName,true,this.getClass().getClassLoader());
			Driver driver=(Driver)driverClass.newInstance();
			if(!driver.acceptsURL(this.jdbcUrl))throw new InstantiationException();
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
	//check pool configuration
	public void check()throws SQLException {
		if(connectionFactory==null && isNullText(this.connectionFactoryClassName)){
			Driver connectDriver=null;
			if(!isNullText(driverClassName)){
				connectDriver=loadJdbcDriver(driverClassName);
			}else if(!isNullText(jdbcUrl)){
				connectDriver = DriverManager.getDriver(this.jdbcUrl);
			} 
			
			if (isNullText(jdbcUrl))
				throw new IllegalArgumentException("Connect url can't be null");
			if (connectDriver==null)
				throw new IllegalArgumentException("Failed to find mathed jdbc Driver");
			
			if (!isNullText(this.username))
				this.connectProperties.put("user", this.username);
			if (!isNullText(this.password))
				this.connectProperties.put("password", this.password);
			
			connectionFactory= new JdbcConnectionFactory(jdbcUrl,connectDriver,connectProperties);
		}else if(connectionFactory==null && !isNullText(this.connectionFactoryClassName)){
			try {
 				Class<?> conFactClass=Class.forName(connectionFactoryClassName,true,BeeDataSourceConfig.class.getClassLoader());
				if(!ConnectionFactory.class.isAssignableFrom(conFactClass))
					throw new IllegalArgumentException("Custom connection factory class must be implemented 'ConnectionFactory' interface");
				
 				connectionFactory=(ConnectionFactory)conFactClass.newInstance();
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Class("+connectionFactoryClassName+")not found ");
			} catch (InstantiationException e) {
				throw new IllegalArgumentException("failed ot instantiated connection factory class:"+connectionFactoryClassName,e);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException("failed ot instantiated connection factory class:"+connectionFactoryClassName,e);
			}
		}
		
		if (this.maxActive <= 0)
			throw new IllegalArgumentException("Pool max size must be greater than zero");
		if (this.initialSize < 0)
			throw new IllegalArgumentException("Pool init size must be greater than zero");
		if (this.initialSize > maxActive)
			throw new IllegalArgumentException("Error configeruation,pool initiation size must be less than pool max size");
		if (this.concurrentSize <=0)
			throw new IllegalArgumentException("Error configeruation,pool concurrent size must be greater than zero");
		if (this.concurrentSize > maxActive)
			throw new IllegalArgumentException("Error configeruation,pool concurrent size must be less than pool max size");
		if (this.idleTimeout <= 0)
			throw new IllegalArgumentException("Connection max idle time must be greater than zero");
		if (this.maxWait <= 0)
			throw new IllegalArgumentException("Borrower max wait time must be greater than zero");
		if (this.preparedStatementCacheSize < 0)
			throw new IllegalArgumentException("Statement cache size must be greater than zero");
		
		//fix issue:#1 The check of validationQuerySQL has logic problem. Chris-2019-05-01 begin
		//if (this.validationQuerySQL != null && validationQuerySQL.trim().length() == 0) {
		if (!isNullText(this.connectionTestSQL) && !this.connectionTestSQL.trim().toLowerCase().startsWith("select "))
		//fix issue:#1 The check of validationQuerySQL has logic problem. Chris-2019-05-01 end	
			throw new IllegalArgumentException("Connection validate SQL must start with 'select '");
		//}
	}
}
