/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package cn.beecp.pool;

import cn.beecp.RawConnectionFactory;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Connection factory Implementation with a JDBC Driver
 *
 * @author Chris liao
 * @version 1.0
 */
public final class ConnectionFactoryByDriver implements RawConnectionFactory {
    //Jdbc url to db
    private final String url;
    //creator of raw connections
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
    public final Connection create() throws SQLException {
        return this.driver.connect(this.url, this.properties);
    }
}
