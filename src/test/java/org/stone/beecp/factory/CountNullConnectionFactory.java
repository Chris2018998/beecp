package org.stone.beecp.factory;

import org.stone.beecp.RawConnectionFactory;
import org.stone.beecp.mock.MockDriver;

import java.sql.Connection;
import java.sql.SQLException;

public class CountNullConnectionFactory implements RawConnectionFactory {
    private int maxCount;
    private int createdCount;
    private MockDriver driver = new MockDriver();

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
