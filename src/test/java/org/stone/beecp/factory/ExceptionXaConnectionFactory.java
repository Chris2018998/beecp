package org.stone.beecp.factory;

import org.stone.beecp.RawXaConnectionFactory;

import javax.sql.XAConnection;
import java.sql.SQLException;

public class ExceptionXaConnectionFactory implements RawXaConnectionFactory {

    public XAConnection create() throws SQLException {
        throw new SQLException("Unknown error");
    }
}
