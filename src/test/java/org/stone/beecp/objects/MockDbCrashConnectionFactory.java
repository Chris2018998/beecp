package org.stone.beecp.objects;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Mock Impl on ConnectionFactory
 *
 * @author Chris Liao
 */
public class MockDbCrashConnectionFactory extends MockCommonConnectionFactory {
    private volatile boolean crashed;

    public Connection create() throws SQLException {
        if (crashed) throw new SQLException("Unlucky,your db has crashed");
        return super.create();
    }

    public void dbCrash() {
        this.crashed = true;
        properties.enableExceptionOnMethod("isValid");
    }

    public void dbRestore() {
        this.crashed = false;
        properties.enableExceptionOnMethod("isValid");
    }
}
