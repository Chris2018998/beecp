/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.mock;

import javax.sql.ConnectionEventListener;
import javax.sql.StatementEventListener;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * XaConnection
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class MockXaConnection implements XAConnection {
    private final Connection con;
    private final MockXaResource res;

    MockXaConnection(Connection con, MockXaResource res) {
        this.con = con;
        this.res = res;
    }

    public void close() throws SQLException {
        con.close();
    }

    public Connection getConnection() {
        return con;
    }

    public XAResource getXAResource() {
        return res;
    }

    public void addConnectionEventListener(ConnectionEventListener listener) {
        //do nothing
    }

    public void removeConnectionEventListener(ConnectionEventListener listener) {
        //do nothing
    }

    public void addStatementEventListener(StatementEventListener listener) {
        //do nothing
    }

    public void removeStatementEventListener(StatementEventListener listener) {
        //do nothing
    }
}
