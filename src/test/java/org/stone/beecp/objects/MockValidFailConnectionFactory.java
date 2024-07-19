package org.stone.beecp.objects;

import org.stone.beecp.BeeConnectionFactory;
import org.stone.beecp.driver.MockConnection;

import java.sql.Connection;

public class MockValidFailConnectionFactory implements BeeConnectionFactory {
    private int errorCode;
    private boolean validate;
    private int netWorkTimeout;
    private boolean exceptionOnNetworkTimeout;

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public void setNetWorkTimeout(int netWorkTimeout) {
        this.netWorkTimeout = netWorkTimeout;
    }

    public void setExceptionOnNetworkTimeout(boolean exceptionOnNetworkTimeout) {
        this.exceptionOnNetworkTimeout = exceptionOnNetworkTimeout;
    }

    public Connection create() {
        MockConnection connection = new MockConnection();
        connection.setValid(validate);
        connection.setErrorCode(errorCode);
        connection.setNetworkTimeout(netWorkTimeout);
        connection.setExceptionOnNetworkTimeout(exceptionOnNetworkTimeout);

        return connection;
    }
}