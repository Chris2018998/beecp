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

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Mock XaDataSource
 *
 * @author Chris Liao
 * @version 1.0
 */
public class MockXaDataSource implements XADataSource {
    private String URL;
    private String user;
    private String password;
    private Properties properties;

    private PrintWriter logWriter;
    private int loginTimeout;
    private Logger parentLogger;

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public XAConnection getXAConnection() {
        return new MockXaConnection(new MockConnection(), new MockXaResource());
    }

    public XAConnection getXAConnection(String user, String password) {
        return new MockXaConnection(new MockConnection(), new MockXaResource());
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
}


