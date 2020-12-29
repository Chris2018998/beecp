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
package cn.beecp.boot;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * Jndi DataSource wrapper.
 * When SpringBoot project redeploy,referred Jndi dataSource from middleware will be closed,
 * add wrapper around jndi data Source,which can avoid being closed.
 *
 * @author Chris.Liao
 */
public class JndiDataSourceWrapper implements DataSource {
    private DataSource delegate;

    public JndiDataSourceWrapper(DataSource delegate) {
        this.delegate = delegate;
    }

    public Connection getConnection() throws SQLException {
        return delegate.getConnection();
    }

    public Connection getConnection(String username, String password) throws SQLException {
        return delegate.getConnection(username, password);
    }

    public java.io.PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    public void setLogWriter(java.io.PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }
}
