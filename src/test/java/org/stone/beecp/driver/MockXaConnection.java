/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.driver;

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
    private final MockXaConnectionProperties properties;

    public MockXaConnection(Connection con, MockXaResource res) {
        this(new MockXaConnectionProperties(), con, res);
    }

    public MockXaConnection(MockXaConnectionProperties properties, Connection con, MockXaResource res) {
        this.con = con;
        this.res = res;
        this.properties = properties;
    }

    public void close() throws SQLException {
        con.close();
    }

    public Connection getConnection() throws SQLException {
        properties.mockThrowExceptionOnMethod("getConnection");
        return con;
    }

    public XAResource getXAResource() throws SQLException {
        properties.mockThrowExceptionOnMethod("getXAResource");
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
