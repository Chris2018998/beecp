package org.stone.beecp.issue.HikariCP.issue2176;

import org.stone.beecp.RawConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.beecp.issue.HikariCP.issue2176.SnowConstants.Snow_Key;

public class SnowConnectionFactory implements RawConnectionFactory {

    private SnowflakeKeyStore store;

    public SnowConnectionFactory(SnowflakeKeyStore store) {
        this.store = store;
    }

    public Connection create() throws SQLException {
        Connection con = null;//@todo,generate a connection to your db server(using DriveManager or DataSource?)
        con.setClientInfo(Snow_Key, store.getNewKey());//attach new key to connection
        return con;
    }
}
