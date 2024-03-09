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

import org.stone.beecp.RawConnectionFactory;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import static org.stone.tools.CommonUtil.isBlank;

/**
 * Connection factory Implementation with a JDBC DataSource
 *
 * @author Chris liao
 * @version 1.0
 */
public final class ConnectionFactoryByDriverDs implements RawConnectionFactory, CommonDataSource {
    //username
    private final String username;
    //password
    private final String password;
    //usernameIsNotNull
    private final boolean useUsername;
    //driverDataSource
    private final DataSource driverDataSource;

    //Constructor
    public ConnectionFactoryByDriverDs(DataSource driverDataSource, String username, String password) {
        this.driverDataSource = driverDataSource;
        this.username = username;
        this.password = password;
        this.useUsername = !isBlank(username);
    }

    //return a connection when creates successful,otherwise,throws a failure exception
    public final Connection create() throws SQLException {
        return this.useUsername ? this.driverDataSource.getConnection(this.username, this.password) : this.driverDataSource.getConnection();
    }

    //***************************************************************************************************************//
    //                                      Override methods from CommonDataSource                                   //
    //***************************************************************************************************************//
    public final PrintWriter getLogWriter() throws SQLException {
        return driverDataSource.getLogWriter();
    }

    public final void setLogWriter(PrintWriter out) throws SQLException {
        driverDataSource.setLogWriter(out);
    }

    public int getLoginTimeout() throws SQLException {
        return driverDataSource.getLoginTimeout();
    }

    public final void setLoginTimeout(int seconds) throws SQLException {
        driverDataSource.setLoginTimeout(seconds);
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return driverDataSource.getParentLogger();
    }
}
