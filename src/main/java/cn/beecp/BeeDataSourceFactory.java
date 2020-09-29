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

import cn.beecp.util.BeecpUtil;

import javax.naming.*;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * BeeDataSource factory
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class BeeDataSourceFactory implements ObjectFactory {
    private Properties initProperties = new Properties();

    public BeeDataSourceFactory() {
    }

    public BeeDataSourceFactory(Properties initProperties) {
        if (initProperties != null) this.initProperties = initProperties;
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

    public DataSource create(BeeDataSourceConfig config) {
        return new BeeDataSource(config);
    }

    /**
     * @param obj         The possibly null object containing location or reference
     *                    information that can be used in creating an object.
     * @param name        The name of this object relative to <code>nameCtx</code>, or
     *                    null if no name is specified.
     * @param nameCtx     The context relative to which the <code>name</code> parameter
     *                    is specified, or null if <code>name</code> is relative to the
     *                    default initial context.
     * @param environment The possibly null environment that is used in creating the
     *                    object.
     * @return The object created; null if an object cannot be created.
     * @throws Exception if this object factory encountered an exception while
     *                   attempting to create an object, and no other object
     *                   factories are to be tried.
     * @see NamingManager#getObjectInstance
     * @see NamingManager#getURLContext
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
            throws Exception {

        Class configClass = BeeDataSourceConfig.class;
        Field[] fields = configClass.getDeclaredFields();
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Properties connectProperties = new Properties();

        Reference ref = (Reference) obj;
        for (Field field : fields) {
            String fieldName = field.getName();
            if ("checked".equals(fieldName) || "connectionFactory".equals(fieldName))
                continue;
            RefAddr ra = ref.get(fieldName);
            if (ra == null) continue;
            String configVal = ra.getContent().toString();

            if (!BeecpUtil.isBlank(configVal)) {
                configVal = configVal.trim();

                Class fieldType = field.getType();
                boolean ChangedAccessible = false;
                try {
                    if (Modifier.isPrivate(field.getModifiers()) || Modifier.isProtected(field.getModifiers())) {
                        field.setAccessible(true);
                        ChangedAccessible = true;
                    }

                    if (fieldType.equals(String.class)) {
                        field.set(config, configVal);
                    } else if (fieldType.equals(Boolean.class) || fieldType.equals(Boolean.TYPE)) {
                        field.set(config, Boolean.valueOf(configVal));
                    } else if (fieldType.equals(Integer.class) || fieldType.equals(Integer.TYPE)) {
                        field.set(config, Integer.valueOf(configVal));
                    } else if (fieldType.equals(Long.class) || fieldType.equals(Long.TYPE)) {
                        field.set(config, Long.valueOf(configVal));
                    } else if ("connectProperties".equals(field.getName())) {
                        connectProperties.clear();
                        configVal = configVal.trim();
                        String[] attributeArray = configVal.split(";");
                        for (String attribute : attributeArray) {
                            String[] pairs = attribute.split("=");
                            if (pairs.length == 2)
                                connectProperties.put(pairs[0].trim(), pairs[1].trim());
                        }
                        field.set(config,connectProperties);
                    }
                } finally {
                    if (ChangedAccessible) field.setAccessible(false);//reset
                }
            }
        }
        return new BeeDataSource(config);
    }

    static final class JndiDataSourceWrapper implements DataSource {
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

        public int getLoginTimeout() throws SQLException {
            return delegete.getLoginTimeout();
        }

        public void setLoginTimeout(int seconds) throws SQLException {
            delegete.setLoginTimeout(seconds);
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
