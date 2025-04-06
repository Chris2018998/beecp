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
    private PrintWriter logWriter;
    private int loginTimeout;
    private Logger parentLogger;

    public Connection getConnection() {
        return new MockConnection();
    }

    public Connection getConnection(String username, String password) {
        return new MockConnection();
    }

    public PrintWriter getLogWriter() {
        return this.logWriter;
    }

    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    public int getLoginTimeout() {
        return loginTimeout;
    }

    public void setLoginTimeout(int seconds) {
        this.loginTimeout = seconds;
    }

    public Logger getParentLogger() {
        return this.parentLogger;
    }

    public <T> T unwrap(Class<T> face) {
        return null;
    }

    public boolean isWrapperFor(Class<?> face) {
        return false;
    }
}
