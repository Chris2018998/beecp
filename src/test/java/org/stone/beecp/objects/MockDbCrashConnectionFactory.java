package org.stone.beecp.objects;

import org.stone.beecp.BeeConnectionFactory;
import org.stone.beecp.driver.MockConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * Mock Impl on ConnectionFactory
 *
 * @author Chris Liao
 */
public class MockDbCrashConnectionFactory implements BeeConnectionFactory {
    private final List<MockConnection> connectionList = new LinkedList<>();
    private int errorCode;
    private String errorState;
    private volatile boolean crashed;

    public void setCrashed(boolean crashed) {
        this.crashed = crashed;
        if (crashed) {
            for (MockConnection con : connectionList) {
                con.setValid(false);
                con.setErrorCode(errorCode);
                con.setErrorState(errorState);
            }
        }
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public void setErrorState(String errorState) {
        this.errorState = errorState;
    }

    public Connection create() throws SQLException {
        if (crashed) throw new SQLException("Unlucky,your db has crashed");
        MockConnection con = new MockConnection(errorCode, errorState);
        connectionList.add(con);
        return con;
    }
}
