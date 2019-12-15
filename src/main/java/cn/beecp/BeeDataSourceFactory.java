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

import static cn.beecp.util.BeecpUtil.isNullText;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;

/**
 * BeeDataSource factory
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public final class BeeDataSourceFactory implements ObjectFactory {
	public final static String PROP_INITIALSIZE = "initialSize";
	public final static String PROP_MAXACTIVE = "maxActive";
	public final static String PROP_MAXWAIT = "maxWait";
	
	public final static String PROP_URL = "url";
	public final static String PROP_USERNAME = "username";
	public final static String PROP_PASSWORD = "password";
	public final static String PROP_DRIVERCLASSNAME = "driverClassName";
	public final static String PROP_VALIDATIONQUERY = "validationQuery";

	public final static String PROP_VALIDATIONQUERY_TIMEOUT = "validationQueryTimeout";
	public final static String PROP_POOLPREPAREDSTATEMENTS = "poolPreparedStatements";
	public final static String PROP_MAXOPENPREPAREDSTATEMENTS = "maxOpenPreparedStatements";
	public final static String PROP_DEFAULTTRANSACTIONISOLATION = "defaultTransactionIsolation";
	public final static String PROP_MINEVICTABLEIDLETIMEMILLIS = "minEvictableIdleTimeMillis";
	
	public final static String PROVIDER_URL = "java.naming.provider.url";
	public final static String INITIAL_CONTEXT_FACTORY = "java.naming.factory.initial";
	public final static String SECURITY_PRINCIPAL = "java.naming.security.principal";
	public final static String SECURITY_CREDENTIALS = "java.naming.security.credentials";
	
	private Properties initProperties = new Properties();
	public BeeDataSourceFactory() {}
	public BeeDataSourceFactory(Properties initProperties) {
		if (initProperties != null)this.initProperties = initProperties;
	}
	public void addProperty(String key, String value) {
		this.initProperties.put(key, value);
	}
	public void removeProperty(String key) {
		this.initProperties.remove(key);
	}
	public void unbind(String jndi) throws NamingException {
		InitialContext ctx = new InitialContext(initProperties);
		ctx.unbind(jndi);
	}
	public DataSource lookup(String jndi) throws NamingException {
		InitialContext ctx = new InitialContext(initProperties);
		return new JndiDataSourceWrapper((DataSource) ctx.lookup(jndi));
	}
	public void bind(String jndi, DataSource obj) throws NamingException {
		InitialContext ctx = new InitialContext(initProperties);
		ctx.bind(jndi, obj);
	}
	public DataSource create(BeeDataSourceConfig config){
		return new BeeDataSource((BeeDataSourceConfig) config);
	}
	
	/**
	 *
	 * @param obj
	 *            The possibly null object containing location or reference
	 *            information that can be used in creating an object.
	 * @param name
	 *            The name of this object relative to <code>nameCtx</code>, or
	 *            null if no name is specified.
	 * @param nameCtx
	 *            The context relative to which the <code>name</code> parameter
	 *            is specified, or null if <code>name</code> is relative to the
	 *            default initial context.
	 * @param environment
	 *            The possibly null environment that is used in creating the
	 *            object.
	 * @return The object created; null if an object cannot be created.
	 * @exception Exception
	 *                if this object factory encountered an exception while
	 *                attempting to create an object, and no other object
	 *                factories are to be tried.
	 *
	 * @see NamingManager#getObjectInstance
	 * @see NamingManager#getURLContext
	 */
	public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
			throws Exception {
		
		Reference ref = (Reference) obj;
		String driverClass=null,jdbcURL=null,jdbcUser=null,password=null;
		String initSize=null,maxSize=null,maxWait=null;
		String connectionIdleTimeout=null;
		String validationQuerySQL=null,validationQueryTimeout=null;
		String needStatementCache=null,statementCacheSize=null;
	
		RefAddr ra = ref.get(PROP_DRIVERCLASSNAME);
        if(ra != null) driverClass= ra.getContent().toString();
        ra = ref.get(PROP_URL);
        if(ra != null) jdbcURL= ra.getContent().toString(); 
        ra = ref.get(PROP_USERNAME);
        if(ra != null) jdbcUser= ra.getContent().toString();
        ra = ref.get(PROP_PASSWORD);
        if(ra != null) password= ra.getContent().toString(); 
        BeeDataSourceConfig config = new BeeDataSourceConfig();
		config.setDriverClassName(driverClass);
		config.setUrl(jdbcURL);
		config.setUsername(jdbcUser);
		config.setPassword(password);
 
	    ra = ref.get(PROP_INITIALSIZE);
        if(ra != null) initSize= ra.getContent().toString(); 
	    ra = ref.get(PROP_MAXACTIVE);
        if(ra != null) maxSize= ra.getContent().toString(); 
	    ra = ref.get(PROP_MAXWAIT);
        if(ra != null) maxWait= ra.getContent().toString(); 
 
        ra = ref.get(PROP_VALIDATIONQUERY);
        if(ra != null) validationQuerySQL= ra.getContent().toString(); 
        ra = ref.get(PROP_VALIDATIONQUERY_TIMEOUT);
        if(ra != null) validationQueryTimeout= ra.getContent().toString(); 
        
        ra = ref.get(PROP_MINEVICTABLEIDLETIMEMILLIS);
        if(ra != null) connectionIdleTimeout= ra.getContent().toString(); 
       
		if (!isNullText(maxSize))
			config.setMaxActive(Integer.parseInt(maxSize));
		if (!isNullText(initSize))
			config.setInitialSize(Integer.parseInt(initSize));
		if (!isNullText(maxWait))
			config.setMaxWait(Integer.parseInt(maxWait));
		if (!isNullText(validationQuerySQL))
			config.setConnectionTestSQL(validationQuerySQL);
		if (!isNullText(validationQueryTimeout))
			config.setConnectionTestTimeout(Integer.parseInt(validationQueryTimeout));
		if (!isNullText(connectionIdleTimeout))
			config.setIdleTimeout(Integer.parseInt(connectionIdleTimeout));
	
        ra = ref.get(PROP_POOLPREPAREDSTATEMENTS);
        if(ra != null) needStatementCache= ra.getContent().toString(); 
        ra = ref.get(PROP_MAXOPENPREPAREDSTATEMENTS);
        if(ra != null) statementCacheSize= ra.getContent().toString(); 
	
		if ("true".equals(needStatementCache) || "Y".equals(needStatementCache)) {
			if (!isNullText(statementCacheSize))
				config.setPreparedStatementCacheSize(Integer.parseInt(statementCacheSize));
		} else {
			config.setPreparedStatementCacheSize(0);
		}
		return new BeeDataSource(config);
	}
	
	class JndiDataSourceWrapper implements DataSource {
		private DataSource delegete;
		public JndiDataSourceWrapper(DataSource delegete) {
			this.delegete = delegete;
		}
		public Connection getConnection() throws SQLException {
			return delegete.getConnection();
		}
		public Connection getConnection(String username, String password) throws SQLException {
			return delegete.getConnection(username, password);
		}
		public java.io.PrintWriter getLogWriter() throws SQLException {
			return delegete.getLogWriter();
		}
		public void setLogWriter(java.io.PrintWriter out) throws SQLException {
			delegete.setLogWriter(out);
		}
		public void setLoginTimeout(int seconds) throws SQLException {
			delegete.setLoginTimeout(seconds);
		}
		public int getLoginTimeout() throws SQLException {
			return delegete.getLoginTimeout();
		}
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			return null;
			//return delegete.getParentLogger();
		}
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return delegete.unwrap(iface);
		}
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return delegete.isWrapperFor(iface);
		}
	}
}
