/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.mock;

import java.sql.*;
import java.util.logging.Logger;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class MockDriver implements Driver {
    static {
        try {
            DriverManager.registerDriver(new MockDriver());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection connect(String url, java.util.Properties info) throws SQLException {
        if (!url.endsWith("testdb")) throw new SQLException("db not found");
        return new MockConnection();
    }

    //jdbc:beecp://localhost/test
    public boolean acceptsURL(String url) {
        return url.startsWith("jdbc:beecp:");
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, java.util.Properties info) {
        return null;
    }

    public int getMajorVersion() {
        return 1;
    }

    public int getMinorVersion() {
        return 1;
    }

    public boolean jdbcCompliant() {
        return true;
    }

    public Logger getParentLogger() {
        return null;
    }
}
