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
package org.jmin.bee.impl.connection;

import java.sql.SQLException;

import org.jmin.bee.BeeDataSourceConfig;
import org.jmin.bee.ConnectionFactory;

/**
 * Pool configuration
 * 
 * @author Chris
 * @version 1.0
 */

public final class JdbcPoolConfig extends BeeDataSourceConfig {

	private String dbDriver;
	private String dbURL;
	private String dbUser;
	private String dbPassword;
	private ConnectionFactory connectionFactory = null;

	public JdbcPoolConfig() {
	}

	public JdbcPoolConfig(String driver, String url, String user, String password) {
		this.dbDriver = driver;
		this.dbURL = url;
		this.dbUser = user;
		this.dbPassword = password;
	}

	public String getDbDriver() {
		return dbDriver;
	}

	public void setDbDriver(String dbDriver) {
		this.dbDriver = dbDriver;
	}

	public String getDbURL() {
		return dbURL;
	}

	public void setDbURL(String dbURL) {
		this.dbURL = dbURL;
	}

	public String getDbUser() {
		return dbUser;
	}

	public void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}

	public String getDbPassword() {
		return dbPassword;
	}

	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

	public void check() throws SQLException {
		this.checkItem("Database driver class", dbDriver);
		this.checkItem("Database connection link", dbURL);
		super.check();
	}

	public ConnectionFactory getConnectionFactory() throws SQLException {
		if (connectionFactory == null)
			connectionFactory = new JdbcConFactory(dbDriver, dbURL, dbUser, dbPassword,this.getExtraProperties());
		return this.connectionFactory;
	}

	private void checkItem(String name, String value) throws SQLException {
		if (value == null || value.trim().length() == 0)
			throw new SQLException(name + " can't be null");
	}
}
