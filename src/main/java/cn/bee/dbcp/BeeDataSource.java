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

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import cn.bee.dbcp.pool.ConnectionPool;

/**
 * Bee DataSource,there are three pool implementation for it.
 * 
 * 1) cn.bee.dbcp.pool.ConnectionPool: base implementation with semaphore
 * 2) cn.bee.dbcp.pool.ConnectionPool2: extended implementation with blockingQueue and threadPool
 * 3) cn.bee.dbcp.pool.ConnectionPool3: extended implementation,return raw connections to borrowers directly
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
	 * constructor
	 */
	public BeeDataSource() {}
	
	/**
	 * constructor
	 * @param config data source configuration
	 */
	public BeeDataSource(final BeeDataSourceConfig config){
		pool=createPool(config);
	}
	
	//return pool internal information
	public Map<String,Integer> getPoolSnapshot()throws SQLException{
		if(pool==null)throw new SQLException("Datasource not initialized");
		return pool.getPoolSnapshot();
	}
	
	public void init()throws SQLException{
		if (pool == null) {
			synchronized(this) {
				if(pool== null)
				 pool=createPool(this);
			}
		}
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
		init();
		return pool.getConnection();
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
		pool.destroy();
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
	public <T> T unwrap(java.lang.Class<T> iface) throws SQLException {
		throw new SQLException("Not supported");
	}
	public boolean isWrapperFor(java.lang.Class<?> iface) throws SQLException {
		throw new SQLException("Not supported");
	}
	/**
	 * create a pool instance by specified class name in configuration,
	 * and initialize the pool with configuration
	 *
	 * @param config  pool configuration
	 * @return a initialized pool for data source
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ConnectionPool createPool(BeeDataSourceConfig config){
		String poolImplementClassName=config.getPoolImplementClassName();
		
		try {
			if(poolImplementClassName==null || poolImplementClassName.trim().length()==0)
				poolImplementClassName=BeeDataSourceConfig.DefaultImplementClassName;
			
			Class poolClass = Class.forName(poolImplementClassName,true,BeeDataSource.class.getClassLoader());
			Constructor constructor = poolClass.getDeclaredConstructor(new Class[] {BeeDataSourceConfig.class});
			ConnectionPool pool = (ConnectionPool) constructor.newInstance(new Object[]{config});
			return pool;
		} catch (ClassNotFoundException e) {
			throw new ExceptionInInitializerError("Not found conneciton pool implementation class:" + poolImplementClassName);
		} catch (NoSuchMethodException e) {
			throw new ExceptionInInitializerError(e);
		} catch (SecurityException e) {
			throw new ExceptionInInitializerError(e);
		} catch (InstantiationException e) {
			throw new ExceptionInInitializerError(e);
		} catch (IllegalAccessException e) {
			throw new ExceptionInInitializerError(e);
		} catch (IllegalArgumentException e) {
			throw new ExceptionInInitializerError(e);
		} catch (InvocationTargetException e) {
			Throwable cause=e.getTargetException();
			throw new ExceptionInInitializerError(cause);
		}
	}
}