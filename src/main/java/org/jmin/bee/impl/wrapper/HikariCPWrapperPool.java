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
package org.jmin.bee.impl.wrapper;

import java.sql.Connection;
import java.sql.SQLException;

import org.jmin.bee.BeeConnectionPool;
import org.jmin.bee.BeeDataSourceConfig;
import org.jmin.bee.impl.connection.JdbcPoolConfig;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Wrap HikariCP 
 * 
 * HikariCP is the fastest connection pool at last.
 * Brett Wooldridge,the author is JDBC master from America,
 * his project address:https://github.com/brettwooldridge/HikariCP
 * 
 * @author Chris
 * @version 1.0
 */
public final class HikariCPWrapperPool implements BeeConnectionPool {

	private volatile boolean destroyed;
	private JdbcPoolConfig poolConfig;
	private HikariDataSource datasource;
	private ConnectionPoolHook poolHook;

	public void init(BeeDataSourceConfig info) throws SQLException {
		this.poolConfig = (JdbcPoolConfig) info;

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(poolConfig.getDbURL());
		config.setUsername(poolConfig.getDbUser());
		config.setPassword(poolConfig.getDbPassword());
		config.setDriverClassName(poolConfig.getDbDriver());

		config.setMinimumIdle(poolConfig.getPoolInitSize());
		config.setMaximumPoolSize(poolConfig.getPoolMaxSize());
		config.setConnectionTimeout(poolConfig.getBorrowerMaxWaitTime());
		config.setConnectionTestQuery(poolConfig.getConnectionValidateSQL());

		this.datasource = new HikariDataSource(config);
		this.poolHook = new ConnectionPoolHook(this);
		Runtime.getRuntime().addShutdownHook(this.poolHook);
	}

	public Connection getConnection() throws SQLException {
		return this.getConnection(this.poolConfig.getBorrowerMaxWaitTime());
	}

	public Connection getConnection(long maxWaitTime) throws SQLException {
		return this.datasource.getConnection();
	}

	public void destroy() {
		if (!destroyed) {
			destroyed = true;
			this.datasource.close();
			try {
				Runtime.getRuntime().removeShutdownHook(this.poolHook);
			} catch (Throwable e) {
			}
		}
	}

	private class ConnectionPoolHook extends Thread {

		private BeeConnectionPool connectionPool;

		public ConnectionPoolHook(BeeConnectionPool connectionPool) {
			this.connectionPool = connectionPool;
		}

		public void run() {
			connectionPool.destroy();
		}
	}

}