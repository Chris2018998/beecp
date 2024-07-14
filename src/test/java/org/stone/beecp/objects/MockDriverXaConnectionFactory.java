package org.stone.beecp.objects;

import org.stone.beecp.BeeXaConnectionFactory;
import org.stone.beecp.driver.MockXaDataSource;

import javax.sql.XAConnection;

public class MockDriverXaConnectionFactory extends DatabaseLinkInfo implements BeeXaConnectionFactory {

    private final MockXaDataSource xaDataSource = new MockXaDataSource();

    public XAConnection create() {
        return xaDataSource.getXAConnection();
    }
}
