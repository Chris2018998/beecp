package org.stone.beecp.factory;

import org.stone.beecp.RawConnectionFactory;
import org.stone.beecp.mock.MockConnection;

import java.sql.Connection;

public class MockConnectionFactory extends SimpleConnectionFactory implements RawConnectionFactory {

    public Connection create() {
        return new MockConnection();
    }
}
