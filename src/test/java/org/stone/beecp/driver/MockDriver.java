/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.driver;

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
            throw new RuntimeException(e);
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
