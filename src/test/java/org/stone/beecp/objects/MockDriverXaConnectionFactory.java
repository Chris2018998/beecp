package org.stone.beecp.objects;

import org.stone.beecp.RawXaConnectionFactory;
import org.stone.beecp.driver.MockXaDataSource;

import javax.sql.XAConnection;

public class MockDriverXaConnectionFactory extends DatabaseLinkInfo implements RawXaConnectionFactory {

    private final MockXaDataSource xaDataSource = new MockXaDataSource();

    public XAConnection create() {
        return xaDataSource.getXAConnection();
    }
}
