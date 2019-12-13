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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import javax.sql.DataSource;

import cn.beecp.pool.ConnectionPool;

/**
 * Bee DataSource,there are three pool implementation for it.
 * 
 * 1) cn.beecp.pool.FastConnectionPool:base implementation with semaphore
 * 2) cn.beecp.pool.RawConnectionPool:return raw connections to borrowers directly(maybe used for BeeNode)
 * 
 * Email:  Chris2018998@tom.com
 * Project: https://github.com/Chris2018998/BeeCP
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public final class BeeDataSource extends BeeDataSourceConfig implements DataSource {

	/**
	 * connection pool
	 */
	private volatile ConnectionPool pool=null;
	
	/**
	 * read Write Locker
	 */
	private ReentrantReadWriteLock lock=new ReentrantReadWriteLock();
	
	/**
	 * read Locker
	 */
	private ReentrantReadWriteLock.ReadLock readLock=lock.readLock();
	
	/**
	 *  write Locker
	 */
	private ReentrantReadWriteLock.WriteLock writeLock=lock.writeLock();
	
	/**
	 * constructor
	 */
	public BeeDataSource() {}
	
	/**
	 * constructor
	 * @param config data source configuration
	 */
	public BeeDataSource(final BeeDataSourceConfig config){
		config.copyTo(this);
		pool=createPool(this);
	}
	
	/**
	 * constructor
	 * @param driver driver class name
	 * @param url JDBC url
	 * @param user JDBC user name
	 * @param password JDBC password
	 * 
	 */
	public BeeDataSource(String driver, String url, String user, String password) {
		super(driver,url,user,password);
	}
	
	/**
	 * borrow a connection from pool
	 * 
	 * @return If exists idle connection in pool,then return one;if not, waiting
	 *         until other borrower release
	 * @throws SQLException
	 *             if pool is closed or waiting timeout,then throw exception
	 */
	public Connection getConnection() throws SQLException {
		if(pool== null) {
			if(writeLock.tryLock()) {
				try {
					if(pool==null)pool=createPool(this);
				} finally {
					writeLock.unlock();
				}
			}else{
				try{
					readLock.lock();
					//do nothing
				} finally {
					readLock.unlock();
				}
			}
		}
		
		return pool.getConnection(maxWait);
	}
	
	/**
	 * <p>Attempts to establish a connection with the data source that
     * this {@code DataSource} object represents.
	 *
	 * @param username the database user on whose behalf the connection is
	 *  being made
	 * @param password the user's password
	 * @return  a connection to the data source
	 * @exception SQLException if a database access error occurs
	 * @throws java.sql.SQLTimeoutException  when the driver has determined that the
	 * timeout value specified by the {@code setLoginTimeout} method
	 * has been exceeded and has at least tried to cancel the
	 * current database connection attempt
	 */
	public Connection getConnection(String username, String password) throws SQLException {
		throw new SQLException("Not support");
	}
	public void close(){
		if(pool!=null)
		 pool.shutdown();
	}
	public PrintWriter getLogWriter() throws SQLException {
		throw new SQLException("Not supported");
	}
	public void setLogWriter(PrintWriter out) throws SQLException {
		throw new SQLException("Not supported");
	}
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException("Not supported");
	}
	public void setLoginTimeout(int seconds) throws SQLException {
		throw new SQLException("Not supported");
	}
	public int getLoginTimeout() throws SQLException {
		throw new SQLException("Not supported");
	}
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new SQLException("Not supported");
	}
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new SQLException("Not supported");
	}
	/**
	 * create a pool instance by specified class name in configuration,
	 * and initialize the pool with configuration
	 *
	 * @param config  pool configuration
	 * @return a initialized pool for data source
	 */
	private ConnectionPool createPool(BeeDataSourceConfig config){
		String poolImplementClassName=config.getPoolImplementClassName();
		
		try {
			if(poolImplementClassName==null || poolImplementClassName.trim().length()==0)
				poolImplementClassName=BeeDataSourceConfig.DefaultImplementClassName;
			
			Class<?> poolClass = Class.forName(poolImplementClassName,true,BeeDataSource.class.getClassLoader());
			if(!ConnectionPool.class.isAssignableFrom(poolClass))
			 throw new IllegalArgumentException("Connection pool class must be implemented 'ConnectionPool' interface");
			 
			config.check();
			config.setAsChecked();
			ConnectionPool pool=(ConnectionPool)poolClass.newInstance();
			pool.init(config);
			return pool;
		} catch (SQLException e) {
			throw new ExceptionInInitializerError(e);
		} catch (ClassNotFoundException e) {
			throw new ExceptionInInitializerError("Not found conneciton pool implementation class:" + poolImplementClassName);
		} catch (SecurityException e) {
			throw new ExceptionInInitializerError(e);
		} catch (InstantiationException e) {
			throw new ExceptionInInitializerError(e);
		} catch (IllegalAccessException e) {
			throw new ExceptionInInitializerError(e);
		} catch (IllegalArgumentException e) {
			throw new ExceptionInInitializerError(e);
		}
	}
}