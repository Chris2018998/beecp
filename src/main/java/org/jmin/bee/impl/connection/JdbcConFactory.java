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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.jmin.bee.ConnectionFactory;

/**
 * JDBC source
 * 
 * @author Chris
 * @version 1.0
 */

final class JdbcConFactory implements ConnectionFactory {
	private String dbDriver;
	private String dbURL;
	private String dbUser;
	private String dbPassword;
	private boolean driverLoaded;
	private boolean driverConnectMode;
	private Driver connectionDriver = null;
	private Properties extraProperties;

	public JdbcConFactory(String dbDriver, String dbURL, String dbUser, String dbPassword,Properties extraProperties) throws SQLException {
		this.dbDriver = dbDriver;
		this.dbURL = dbURL;
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
		this.extraProperties=extraProperties;
		if(this.extraProperties==null)
			this.extraProperties = new Properties();
		this.init();
	}

	private void init() throws SQLException {
		if (!this.driverLoaded) {
			this.loadJdbcDriver(this.dbDriver);
			this.driverLoaded = true;
			this.extraProperties.put("user", dbUser);
			this.extraProperties.put("password", dbPassword);
			
			try {
				this.connectionDriver = DriverManager.getDriver(dbURL);
				this.driverConnectMode = true;
			} catch (SQLException e) {}
		}
	}

	public Connection createConnection() throws SQLException {
		if (driverConnectMode) {
			return connectionDriver.connect(dbURL,this.extraProperties);
		} else {
			return DriverManager.getConnection(dbURL,this.extraProperties);
		}
	}

	private void loadJdbcDriver(String driver) throws SQLException {
		try {
			Class.forName(driver, true, this.getClass().getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new SQLException("Jdbc driver class[" + driver + "] not found");
		}
	}
}
