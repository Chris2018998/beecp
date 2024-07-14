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
import java.io.PrintWriter;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Connection factory Implementation with a JDBC Driver
 *
 * @author Chris liao
 * @version 1.0
 */
public final class ConnectionFactoryByDriver implements BeeConnectionFactory, CommonDataSource {
    //Jdbc url
    private final String url;
    //jdbc driver build connections to database
    private final Driver driver;
    //extra properties to link db
    private final Properties properties;

    //Constructor
    public ConnectionFactoryByDriver(String url, Driver driver, Properties properties) {
        this.url = url;
        this.driver = driver;
        this.properties = properties;
    }

    //return a connection if link successful to db,otherwise,throws a failure exception
    public Connection create() throws SQLException {
        return this.driver.connect(this.url, this.properties);
    }

    //***************************************************************************************************************//
    //                                      Override methods from CommonDataSource                                   //
    //***************************************************************************************************************//
    public PrintWriter getLogWriter() {
        return DriverManager.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) {
        DriverManager.setLogWriter(out);
    }

    public int getLoginTimeout() {
        return DriverManager.getLoginTimeout();
    }

    public void setLoginTimeout(int seconds) {
        DriverManager.setLoginTimeout(seconds);
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return driver.getParentLogger();
    }
}
