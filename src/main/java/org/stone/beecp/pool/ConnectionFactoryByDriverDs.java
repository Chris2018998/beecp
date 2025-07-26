/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.pool;

import org.stone.beecp.BeeConnectionFactory;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * Connection factory Implementation with a JDBC DataSource
 *
 * @author Chris liao
 * @version 1.0
 */
public final class ConnectionFactoryByDriverDs implements BeeConnectionFactory, CommonDataSource {
    //driverDataSource
    private final DataSource driverDataSource;
    //username
    private String username;
    //password
    private String password;
    //usernameIsNotNull
    private boolean useUsername;

    //Constructor
    public ConnectionFactoryByDriverDs(DataSource driverDataSource, String username, String password) {
        this.driverDataSource = driverDataSource;
        this.username = username;
        this.password = password;
        this.useUsername = isNotBlank(username);
    }

    //return a connection when creates successful,otherwise,throws a failure exception
    public Connection create() throws SQLException {
        return this.useUsername ? this.driverDataSource.getConnection(this.username, this.password) : this.driverDataSource.getConnection();
    }

    //***************************************************************************************************************//
    //                                     update user info                                                          //
    //***************************************************************************************************************//
    public void setUsername(String username) {
        this.username = username;
        this.useUsername = isNotBlank(username);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    //***************************************************************************************************************//
    //                                      Override methods from CommonDataSource                                   //
    //***************************************************************************************************************//
    public PrintWriter getLogWriter() throws SQLException {
        return driverDataSource.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        driverDataSource.setLogWriter(out);
    }

    public int getLoginTimeout() throws SQLException {
        return driverDataSource.getLoginTimeout();
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        driverDataSource.setLoginTimeout(seconds);
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return driverDataSource.getParentLogger();
    }
}
