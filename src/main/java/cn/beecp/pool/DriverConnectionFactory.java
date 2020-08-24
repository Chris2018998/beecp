/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    public Connection create() throws SQLException {
        return connectDriver.connect(connectURL, connectProperties);
    }
}
