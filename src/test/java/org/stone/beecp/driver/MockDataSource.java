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

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.logging.Logger;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class MockDataSource implements DataSource {

    public Connection getConnection() {
        return new MockConnection();
    }

    public Connection getConnection(String username, String password) {
        return new MockConnection();
    }

    public PrintWriter getLogWriter() {
        return null;
    }

    public void setLogWriter(PrintWriter out) {
        //do nothing
    }

    public int getLoginTimeout() {
        return 0;
    }

    public void setLoginTimeout(int seconds) {
        //do nothing
    }

    public Logger getParentLogger() {
        return null;
    }

    public <T> T unwrap(Class<T> face) {
        return null;
    }

    public boolean isWrapperFor(Class<?> face) {
        return false;
    }
}
