package org.stone.beecp.objects;

import org.stone.beecp.RawConnectionFactory;
import org.stone.beecp.driver.MockConnection;

import java.sql.Connection;

public class MockEvictPredicateConnectionFactory implements RawConnectionFactory {
    private int errorCode;
    private String errorState;

    public MockEvictPredicateConnectionFactory(int errorCode, String errorState) {
        this.errorCode = errorCode;
        this.errorState = errorState;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public void setErrorState(String errorState) {
        this.errorState = errorState;
    }

    public Connection create() {
        return new MockConnection(errorCode, errorState);
    }
}
