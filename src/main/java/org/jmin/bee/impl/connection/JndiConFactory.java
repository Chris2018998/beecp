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
import java.sql.SQLException;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jmin.bee.ConnectionFactory;

/**
 * JNDI Source
 * 
 * @author Chris
 * @version 1.0
 */

final class JndiConFactory implements ConnectionFactory {

	private String jndiName;
	private String contextFactory;
	private String contextProvideURL;
	private String contextPrincipal;
	private String contextCredentials;
	private String dbUser;
	private String dbPassword;
	private DataSource datasource = null;

	public JndiConFactory(String jndiName, String contextFactory, String contextProvideURL, String contextPrincipal,
			String contextCredentials) {
		this(jndiName, contextFactory, contextProvideURL, contextPrincipal, contextCredentials, null, null);
	}

	public JndiConFactory(String jndiName, String contextFactory, String contextProvideURL, String contextPrincipal,
			String contextCredentials, String dbUser, String dbPassword) {
		this.jndiName = jndiName;
		this.contextFactory = contextFactory;
		this.contextProvideURL = contextProvideURL;
		this.contextPrincipal = contextPrincipal;
		this.contextCredentials = contextCredentials;
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
	}

	public void init() throws SQLException {
		if (datasource == null) {
			try {
				Properties prop = new Properties();
				prop.put(Context.PROVIDER_URL, this.contextProvideURL);
				prop.put(Context.INITIAL_CONTEXT_FACTORY, this.contextFactory);
				prop.put(Context.SECURITY_PRINCIPAL, this.contextPrincipal);
				prop.put(Context.SECURITY_CREDENTIALS, this.contextCredentials);
				InitialContext ctx = new InitialContext(prop);
				this.datasource = (DataSource) ctx.lookup(this.jndiName);
			} catch (NamingException e) {
				throw new SQLException(e.getMessage());
			}
		}
	}

	public Connection createConnection() throws SQLException {
		if (dbUser == null)
			return datasource.getConnection();
		else
			return datasource.getConnection(dbUser.trim(), dbPassword);
	}

}
