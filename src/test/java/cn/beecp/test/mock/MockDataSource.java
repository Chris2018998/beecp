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
package cn.beecp.test.mock;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class MockDataSource implements DataSource {
    public Connection getConnection() throws SQLException {
        return new MockConnection();
    }

    public Connection getConnection(String username, String password) throws SQLException {
        return new MockConnection();
    }

    public java.io.PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    public void setLogWriter(java.io.PrintWriter out) throws SQLException {
    }

    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    public void setLoginTimeout(int seconds) throws SQLException {
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    public <T> T unwrap(java.lang.Class<T> iface) throws java.sql.SQLException {
        return null;
    }

    public boolean isWrapperFor(java.lang.Class<?> iface) throws java.sql.SQLException {
        return false;
    }
}
