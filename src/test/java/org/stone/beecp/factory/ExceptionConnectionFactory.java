package org.stone.beecp.factory;

import org.stone.beecp.RawConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class ExceptionConnectionFactory implements RawConnectionFactory {

    public Connection create() throws SQLException {
        throw new SQLException("Unknown error");
    }
}
