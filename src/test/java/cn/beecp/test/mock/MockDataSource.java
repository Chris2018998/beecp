/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
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
