package org.stone.beecp.objects;

import org.stone.beecp.BeeConnectionFactory;
import org.stone.beecp.driver.MockConnection;

import java.sql.Connection;

public class MockErrorCodeConnectionFactory implements BeeConnectionFactory {

    private final int errorCode;

    public MockErrorCodeConnectionFactory(int errorCode) {
        this.errorCode = errorCode;
    }

    public Connection create() {
        return new MockConnection(errorCode);
    }
}
