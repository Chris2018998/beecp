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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Datasource ConnectionFactory
 *
 * @author Chris.liao
 * @version 1.0
 */
public class DataSourceConnectionFactory implements ConnectionFactory {

    /**
     * username
     */
    private String username;

    /**
     * password
     */
    private String password;

    /**
     * usernameIsNull
     */
    private boolean usernameIsNull=true;

    /**
     * driverDataSource
     */
    private DataSource driverDataSource;

    //Constructor
    public DataSourceConnectionFactory(DataSource driverDataSource) {
        this.driverDataSource = driverDataSource;
    }
    //Constructor
    public DataSourceConnectionFactory(DataSource driverDataSource, String username, String password) {
        this.driverDataSource = driverDataSource;
        this.username = username;
        this.password = password;
        this.usernameIsNull=false;
    }
    //create one connection
    public Connection create() throws SQLException {
        if (usernameIsNull) {
            return driverDataSource.getConnection();
        }else {
            return driverDataSource.getConnection(username, password);
        }
    }
}
