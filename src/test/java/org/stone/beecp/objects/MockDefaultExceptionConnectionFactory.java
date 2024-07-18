package org.stone.beecp.objects;

import org.stone.beecp.BeeConnectionFactory;
import org.stone.beecp.driver.MockConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class MockDefaultExceptionConnectionFactory implements BeeConnectionFactory {

    public Connection create() throws SQLException {
        MockConnection con = new MockConnection();
        con.enableExceptionOnDefault();
        return con;
    }
}
