/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool;

import cn.beecp.RawConnectionFactory;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Driver ConnectionFactory
 *
 * @author Chris.liao
 * @version 1.0
 */

public final class DriverConnectionFactory implements RawConnectionFactory {
    //url link
    private String url;
    //connection driver
    private Driver driver;
    //connection extra properties
    private Properties properties;

    //Constructor
    public DriverConnectionFactory(String url, Driver driver, Properties properties) {
        this.url = url;
        this.driver = driver;
        this.properties = properties;
    }

    //create one connection
    public final Connection create() throws SQLException {
        return driver.connect(url, properties);
    }
}
