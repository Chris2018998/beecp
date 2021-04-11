/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp.pool;

import cn.beecp.ConnectionFactory;

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

public final class DriverConnectionFactory implements ConnectionFactory {

    /**
     * url link
     */
    private String connectURL;

    /**
     * connection driver
     */
    private Driver connectDriver;

    /**
     * connection extra properties
     */
    private Properties connectProperties;

    //Constructor
    public DriverConnectionFactory(String connectURL, Driver connectDriver, Properties connectProperties) {
        this.connectURL = connectURL;
        this.connectDriver = connectDriver;
        this.connectProperties = connectProperties;
    }

    //create one connection
    public final Connection create() throws SQLException {
        return connectDriver.connect(connectURL, connectProperties);
    }
}
