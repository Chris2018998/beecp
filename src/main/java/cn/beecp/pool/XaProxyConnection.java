/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool;

import javax.sql.ConnectionEventListener;
import javax.sql.StatementEventListener;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * XaConnection Proxy
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class XaProxyConnection implements XAConnection {
    private final XAResource resource;
    private final ProxyConnectionBase proxyConn;

    XaProxyConnection(ProxyConnectionBase proxyBaseConn, XAResource resource) {
        this.proxyConn = proxyBaseConn;
        this.resource = resource;
    }

    public void close() throws SQLException {
        proxyConn.close();
    }

    public Connection getConnection() throws SQLException {
        proxyConn.checkClosed();
        return proxyConn;
    }

    public XAResource getXAResource() throws SQLException {
        proxyConn.checkClosed();
        return resource;
    }

    public void addConnectionEventListener(ConnectionEventListener listener) {
    }

    public void removeConnectionEventListener(ConnectionEventListener listener) {
    }

    public void addStatementEventListener(StatementEventListener listener) {
    }

    public void removeStatementEventListener(StatementEventListener listener) {
    }
}
