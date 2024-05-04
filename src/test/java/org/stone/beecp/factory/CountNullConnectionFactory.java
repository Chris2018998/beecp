package org.stone.beecp.factory;

import org.stone.beecp.RawConnectionFactory;
import org.stone.beecp.mock.MockDriver;

import java.sql.Connection;
import java.sql.SQLException;

public class CountNullConnectionFactory implements RawConnectionFactory {
    private final int maxCount;
    private final MockDriver driver = new MockDriver();
    private int createdCount;

    public CountNullConnectionFactory(int maxCount) {
        this.maxCount = maxCount;
    }

    //create connection instance
    public Connection create() throws SQLException {
        if (createdCount >= maxCount) return null;
        Connection con = driver.connect("testdb", null);
        createdCount++;
        return con;
    }
}
