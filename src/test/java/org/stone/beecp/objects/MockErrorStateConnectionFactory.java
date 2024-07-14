package org.stone.beecp.objects;

import org.stone.beecp.BeeConnectionFactory;
import org.stone.beecp.driver.MockConnection;

import java.sql.Connection;

public class MockErrorStateConnectionFactory implements BeeConnectionFactory {

    private final String errorState;

    public MockErrorStateConnectionFactory(String errorState) {
        this.errorState = errorState;
    }

    public Connection create() {
        return new MockConnection(errorState);
    }
}
