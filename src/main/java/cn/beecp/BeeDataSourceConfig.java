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

import cn.beecp.pool.DataSourceConnectionFactory;
import cn.beecp.pool.DriverConnectionFactory;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import static cn.beecp.util.BeecpUtil.isNullText;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

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
	 * borrow Semaphore Size
	 */
	private int borrowSemaphoreSize;
	
	/**
	 * 'PreparedStatement' cache size
	 */
	private int preparedStatementCacheSize;

	/**
	 * connection.setAutoCommit(boolean);
	 */
	private boolean defaultAutoCommit=true;

	/**
	 * default Transaction Isolation
	 */
	private String defaultTransactionIsolation;

	/**
	 * default Transaction Isolation code
	 */
	private int defaultTransactionIsolationCode;

	/**
	 *connection.setCatalog
	 */
	private String defaultCatalog;

	/**
	 *connection.setSchema
	 */
	private String defaultSchema;

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
	 * connection validate timeout:3 seconds
	 */
	private int connectionTestTimeout = 3;
	 
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
		defaultTransactionIsolation=TransactionIsolationLevel.LEVEL_READ_COMMITTED;
		defaultTransactionIsolationCode=TransactionIsolationLevel.CODE_READ_COMMITTED;
        //fix issue:#19 Chris-2020-08-16 begin
		borrowSemaphoreSize =Math.min(maxActive/2,Runtime.getRuntime().availableProcessors());
        //fix issue:#19 Chris-2020-08-16 end
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
		if(!this.checked && maxActive>0) {
            this.maxActive = maxActive;
            //fix issue:#19 Chris-2020-08-16 begin
            this.borrowSemaphoreSize=Math.min(maxActive/2,Runtime.getRuntime().availableProcessors());
            //fix issue:#19 Chris-2020-08-16 end
        }
	}
	public int getBorrowSemaphoreSize() {
		return borrowSemaphoreSize;
	}
	public void setBorrowSemaphoreSize(int borrowSemaphoreSize) {
		if(!this.checked && borrowSemaphoreSize>0)
		this.borrowSemaphoreSize = borrowSemaphoreSize;
	}
	public int getPreparedStatementCacheSize() {
		return preparedStatementCacheSize;
	}
	public void setPreparedStatementCacheSize(int preparedStatementCacheSize) {
		if(!this.checked && preparedStatementCacheSize>=0)
		this.preparedStatementCacheSize = preparedStatementCacheSize;
	}
	public boolean isDefaultAutoCommit() {
		return defaultAutoCommit;
	}
	public void setDefaultAutoCommit(boolean defaultAutoCommit) {
		if(!this.checked)this.defaultAutoCommit = defaultAutoCommit;
	}
	public String getDefaultTransactionIsolation() {
		return defaultTransactionIsolation;
	}
	public void setDefaultTransactionIsolation(String defaultTransactionIsolation) {
		if(!this.checked && !isNullText(defaultTransactionIsolation))
		this.defaultTransactionIsolation = defaultTransactionIsolation;
	}
	public int getDefaultTransactionIsolationCode(){
		return defaultTransactionIsolationCode;
	}

	public String getDefaultCatalog() {return defaultCatalog; }
	public void setDefaultCatalog(String catalog) {
		if(!isNullText(catalog))
			this.defaultCatalog = catalog;
	}
	public String getDefaultSchema(){return defaultSchema;}
	public void setDefaultSchema(String schema) {
		if(!isNullText(schema))
			this.defaultSchema = schema;
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
	public void setHoldIdleTimeout(long holdIdleTimeout) {
		if(!this.checked && holdIdleTimeout>0)
		this.holdIdleTimeout = holdIdleTimeout;
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
	void copyTo(BeeDataSourceConfig config)throws SQLException{
		int modifiers;
		Field[] fields=BeeDataSourceConfig.class.getDeclaredFields();
		for(Field field:fields){
			if("checked".equals(field.getName()))continue;
			modifiers=field.getModifiers();
			if(Modifier.isStatic(modifiers)||Modifier.isFinal(modifiers))
				continue;

			boolean accessible=field.isAccessible();
			try {
				if(!accessible)field.setAccessible(true);
				field.set(config, field.get(this));
			}catch(Exception e){
				throw new BeeDataSourceConfigException("Failed to copy field["+field.getName()+"]",e);
			}finally{
				if(!accessible)field.setAccessible(accessible);
			}
		}
	}
	
	private Driver loadJdbcDriver(String driverClassName) throws BeeDataSourceConfigException {
		try {
			Class<?> driverClass = Class.forName(driverClassName,true,this.getClass().getClassLoader());
			Driver driver=(Driver)driverClass.newInstance();
			if(!driver.acceptsURL(this.jdbcUrl))throw new InstantiationException();
			return driver;
		} catch (ClassNotFoundException e) {
			throw new BeeDataSourceConfigException("Driver class[" + driverClassName + "]not found");
		} catch (InstantiationException e) {
			throw new BeeDataSourceConfigException("Driver class[" + driverClassName + "]can't be instantiated");
		} catch (IllegalAccessException e) {
			throw new BeeDataSourceConfigException("Driver class[" + driverClassName + "]can't be instantiated",e);
		} catch (SQLException e) {
			throw new BeeDataSourceConfigException("Driver class[" + driverClassName + "]can't be instantiated",e);
		}
	}
	//check pool configuration
	void check()throws SQLException {
		if(connectionFactory==null && isNullText(this.connectionFactoryClassName)){
			Driver connectDriver=null;
			if(!isNullText(driverClassName)){
				connectDriver=loadJdbcDriver(driverClassName);
			}else if(!isNullText(jdbcUrl)){
				connectDriver = DriverManager.getDriver(this.jdbcUrl);
			} 
			
			if (isNullText(jdbcUrl))
				throw new BeeDataSourceConfigException("Connect url can't be null");
			if (connectDriver==null)
				throw new BeeDataSourceConfigException("Failed to load jdbc Driver");
			
			if (!isNullText(this.username))
				this.connectProperties.put("user", this.username);
			if (!isNullText(this.password))
				this.connectProperties.put("password", this.password);
			
			connectionFactory= new DriverConnectionFactory(jdbcUrl,connectDriver,connectProperties);
		}else if(connectionFactory==null && !isNullText(this.connectionFactoryClassName)){
			try {
 				Class<?> conFactClass=Class.forName(connectionFactoryClassName,true,BeeDataSourceConfig.class.getClassLoader());
				if(ConnectionFactory.class.isAssignableFrom(conFactClass)){
					connectionFactory=(ConnectionFactory)conFactClass.newInstance();
				}else if(DataSource.class.isAssignableFrom(conFactClass)){
					DataSource driverDataSource=(DataSource)conFactClass.newInstance();
					Iterator itor=connectProperties.entrySet().iterator();
					while(itor.hasNext()) {
						Map.Entry entry = (Map.Entry) itor.next();
						if (entry.getKey() instanceof String) {
							try {
								setDataSourceProperty((String)entry.getKey(),entry.getValue(),driverDataSource);
							}catch(Exception e){
								throw new BeeDataSourceConfigException("Failed to set datasource property["+entry.getKey()+"]",e);
							}
						}
					}
					connectionFactory=new DataSourceConnectionFactory(driverDataSource,username,password);
				}else{
					throw new BeeDataSourceConfigException("Custom connection factory class must be implemented 'ConnectionFactory' interface");
				}
			} catch (ClassNotFoundException e) {
				throw new BeeDataSourceConfigException("Class("+connectionFactoryClassName+")not found ");
			} catch (InstantiationException e) {
				throw new BeeDataSourceConfigException("Failed to instantiate connection factory class:"+connectionFactoryClassName,e);
			} catch (IllegalAccessException e) {
				throw new BeeDataSourceConfigException("Failed to instantiate connection factory class:"+connectionFactoryClassName,e);
			}
		}
		
		if (this.maxActive <= 0)
			throw new BeeDataSourceConfigException("Pool 'maxActive' must be greater than zero");
		if (this.initialSize < 0)
			throw new BeeDataSourceConfigException("Pool 'initialSize' must be greater than zero");
		if (this.initialSize > maxActive)
			throw new BeeDataSourceConfigException("Pool 'initialSize' must not be greater than 'maxActive'");
		if (this.borrowSemaphoreSize <=0)
			throw new BeeDataSourceConfigException("Pool 'borrowSemaphoreSize' must be greater than zero");
		//fix issue:#19 Chris-2020-08-16 begin
		//if (this.borrowConcurrentSize > maxActive)
			//throw new BeeDataSourceConfigException("Pool 'borrowConcurrentSize' must not be greater than pool max size");
		//fix issue:#19 Chris-2020-08-16 end

		if (this.idleTimeout <= 0)
			throw new BeeDataSourceConfigException("Connection 'idleTimeout' must be greater than zero");
		if (this.holdIdleTimeout <= 0)
			throw new BeeDataSourceConfigException("Connection 'holdIdleTimeout' must be greater than zero");
		if (this.maxWait <= 0)
			throw new BeeDataSourceConfigException("Borrower 'maxWait' must be greater than zero");
		if (this.preparedStatementCacheSize < 0)
			throw new BeeDataSourceConfigException("Connection 'preparedStatementCacheSize' must not be lesser than zero");

		defaultTransactionIsolationCode=TransactionIsolationLevel.nameToCode(defaultTransactionIsolation);
		if(defaultTransactionIsolationCode==-999){
			throw new BeeDataSourceConfigException("Valid transaction isolation level list:"+TransactionIsolationLevel.TRANS_LEVEL_LIST);
		}

		//fix issue:#1 The check of validationQuerySQL has logic problem. Chris-2019-05-01 begin
		//if (this.validationQuerySQL != null && validationQuerySQL.trim().length() == 0) {
		if (!isNullText(this.connectionTestSQL) && !this.connectionTestSQL.trim().toLowerCase().startsWith("select "))
		//fix issue:#1 The check of validationQuerySQL has logic problem. Chris-2019-05-01 end	
			throw new BeeDataSourceConfigException("Connection 'connectionTestSQL' must start with 'select '");
		//}
	}
	private void setDataSourceProperty(String propName,Object propValue,Object bean)throws Exception{
		if(propName.endsWith("."))return;
		int index=propName.lastIndexOf(".");
		if(index>=0)propName=propName.substring(index+1);

		propName=propName.trim();
		String methodName="set"+propName.substring(0,1).toUpperCase()+propName.substring(1);
		Method[] methods =bean.getClass().getMethods();
		Method targetMethod=null;
		for(Method method:methods){
			if(method.getName().equals(methodName) && method.getParameterTypes().length==1){
				targetMethod = method;
				break;
			}
		}

		if(targetMethod!=null){
			Class paramType=targetMethod.getParameterTypes()[0];
			if(paramType.isInstance(propValue)){
				targetMethod.invoke(bean,new Object[]{propValue});
			}else if(propValue instanceof String){
				String value=(String)propValue;
				if(paramType==String.class ){
					targetMethod.invoke(bean,new Object[]{propValue});
				}else if(paramType==boolean.class||paramType==Boolean.class){
					targetMethod.invoke(bean,new Object[]{Boolean.valueOf(value)});
				}else if(paramType==int.class||paramType==Integer.class){
					targetMethod.invoke(bean,new Object[]{Integer.valueOf(value)});
				}else if(paramType==long.class||paramType==Long.class){
					targetMethod.invoke(bean,new Object[]{Long.valueOf(value)});
				}
			}
		}
	}
	public void loadPropertiesFile(String filename)throws IOException{
		File file = new File(filename);
		if(!file.exists())throw new FileNotFoundException(filename);
		loadPropertiesFile(file);
	}
	public void loadPropertiesFile(File file)throws IOException{
		if(!file.isFile())throw new IOException("Invalid properties file");
		if(!file.getAbsolutePath().toLowerCase().endsWith(".properties"))throw new IOException("Invalid properties file");

		if(!checked){
			FileInputStream stream=null;
			try {
				stream=new FileInputStream(file);
				connectProperties.clear();
				connectProperties.load(stream);
			}finally{
				if(stream!=null)stream.close();
			}
		}
	}
}

