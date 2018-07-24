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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * BeeCP DataSource implementation
 * 
 * @author Chris
 * @version 1.0
 */

public final class BeeDataSource implements DataSource {

	/**
	 * inner connection pool
	 */
	private BeeConnectionPool pool;

	/**
	 * constructor
	 */
	public BeeDataSource(final BeeDataSourceConfig config) throws SQLException {
		this.pool = this.createPool(config);
	}

	/**
	 * return connection to borrower
	 */
	public Connection getConnection() throws SQLException {
		return pool.getConnection();
	}

	/**
	 * return connection to borrower
	 */
	public Connection getConnection(final String username, final String password) throws SQLException {
		return pool.getConnection();
	}

	/**
	 * close connection pool
	 */
	public void close() throws SQLException {
		pool.destroy();
	}
 	
	/**
	 * override method
	 */
	public PrintWriter getLogWriter() throws SQLException {
		throw new SQLException("Not supported");
	}

	/**
	 * override method
	 */
	public void setLogWriter(PrintWriter out) throws SQLException {
		throw new SQLException("Not supported");
	}

	/**
	 * override method
	 */
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException("Not supported");
	}

	/**
	 * override method
	 */
	public void setLoginTimeout(int seconds) throws SQLException {
		throw new SQLException("Not supported");
	}

	/**
	 * override method
	 */
	public int getLoginTimeout() throws SQLException {
		throw new SQLException("Not supported");
	}

	/**
	 * override method
	 */
	public <T> T unwrap(java.lang.Class<T> iface) throws java.sql.SQLException {
		throw new SQLException("Not supported");
	}

	/**
	 * override method
	 */
	public boolean isWrapperFor(java.lang.Class<?> iface) throws java.sql.SQLException {
		throw new SQLException("Not supported");
	}

	/**
	 * create connection pool by configuration
	 */
	private BeeConnectionPool createPool(BeeDataSourceConfig config) throws SQLException {
		try {
			Class poolClass = Class.forName(config.getConnectionPoolClassName(),true,BeeDataSource.class.getClassLoader());
			BeeConnectionPool pool = (BeeConnectionPool)poolClass.newInstance();
			pool.init(config);
			return pool;
		} catch (ClassNotFoundException e) {
			throw new SQLException(
					"Not found conneciton pool implementation class:" + config.getConnectionPoolClassName());
		} catch (InstantiationException e) {
			throw new SQLException(
					"Failed to initialized conneciton pool by class:" + config.getConnectionPoolClassName());
		} catch (IllegalAccessException e) {
			throw new SQLException(
					"Failed to initialized conneciton pool by class:" + config.getConnectionPoolClassName());
		}
	}
}
