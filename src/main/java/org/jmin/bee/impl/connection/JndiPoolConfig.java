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
 * JNDI Source pool configuration
 * 
 * @author Chris
 * @version 1.0
 */

public final class JndiPoolConfig extends BeeDataSourceConfig {

	private String jndiName;
	private String contextFactory;
	private String contextProvideURL;
	private String contextPrincipal;
	private String contextCredentials;
	private String dbUser;
	private String dbPassword;
	private ConnectionFactory connectionFactory = null;

	public JndiPoolConfig() {
	}

	public JndiPoolConfig(String jndiName, String contextFactory, String contextProvideURL, String contextPrincipal,
			String contextCredentials) {
		this.jndiName = jndiName;
		this.contextFactory = contextFactory;
		this.contextProvideURL = contextProvideURL;
		this.contextCredentials = contextCredentials;
	}

	public String getJndiName() {
		return jndiName;
	}

	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}

	public String getContextFactory() {
		return contextFactory;
	}

	public void setContextFactory(String contextFactory) {
		this.contextFactory = contextFactory;
	}

	public String getContextProvideURL() {
		return contextProvideURL;
	}

	public void setContextProvideURL(String contextProvideURL) {
		this.contextProvideURL = contextProvideURL;
	}

	public String getContextPrincipal() {
		return contextPrincipal;
	}

	public void setContextPrincipal(String contextPrincipal) {
		this.contextPrincipal = contextPrincipal;
	}

	public String getContextCredentials() {
		return contextCredentials;
	}

	public void setContextCredentials(String contextCredentials) {
		this.contextCredentials = contextCredentials;
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
		this.checkItem("JndiName", this.jndiName);
		this.checkItem("ContextFactory", this.contextFactory);
		this.checkItem("ContextProvideURL", this.contextProvideURL);
		this.checkItem("ContextPrincipal", this.contextPrincipal);
		super.check();
	}

	public ConnectionFactory getConnectionFactory() {
		if (connectionFactory == null) {
			if (dbUser == null)
				this.connectionFactory = new JndiConFactory(jndiName, contextFactory, contextProvideURL, contextPrincipal,
						contextCredentials);
			else
				this.connectionFactory = new JndiConFactory(jndiName, contextFactory, contextProvideURL, contextPrincipal,
						contextCredentials, dbUser, dbPassword);
		}
		return this.connectionFactory;
	}

	private void checkItem(String name, String value) throws SQLException {
		if (value == null || value.trim().length() == 0)
			throw new SQLException(name + " can't be null");
	}
}
