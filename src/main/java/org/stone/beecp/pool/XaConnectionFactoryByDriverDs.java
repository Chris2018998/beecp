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

import org.stone.beecp.BeeXaConnectionFactory;

import javax.sql.CommonDataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * XaConnection Factory implementation by XADataSource
 *
 * @author Chris liao
 * @version 1.0
 */
public class XaConnectionFactoryByDriverDs implements BeeXaConnectionFactory, CommonDataSource {
    //username
    private final String username;
    //password
    private final String password;
    //usernameIsNotNull
    private final boolean useUsername;
    //driverDataSource
    private final XADataSource dataSource;

    //Constructor
    public XaConnectionFactoryByDriverDs(XADataSource dataSource, String username, String password) {
        this.dataSource = dataSource;
        this.username = username;
        this.password = password;
        useUsername = isNotBlank(username);
    }

    //create one connection
    public final XAConnection create() throws SQLException {
        return this.useUsername ? this.dataSource.getXAConnection(this.username, this.password) : this.dataSource.getXAConnection();
    }

    //***************************************************************************************************************//
    //                                      Override methods from CommonDataSource                                   //
    //***************************************************************************************************************//
    public final PrintWriter getLogWriter() throws SQLException {
        return dataSource.getLogWriter();
    }

    public final void setLogWriter(PrintWriter out) throws SQLException {
        dataSource.setLogWriter(out);
    }

    public int getLoginTimeout() throws SQLException {
        return dataSource.getLoginTimeout();
    }

    public final void setLoginTimeout(int seconds) throws SQLException {
        dataSource.setLoginTimeout(seconds);
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return dataSource.getParentLogger();
    }
}