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
    private final XAResource proxyResource;
    private final ProxyConnectionBase proxyConn;

    XaProxyConnection(ProxyConnectionBase proxyConn, XAResource proxyResource) {
        this.proxyConn = proxyConn;
        this.proxyResource = proxyResource;
    }

    public void close() throws SQLException {
        this.proxyConn.close();
    }

    public Connection getConnection() throws SQLException {
        this.proxyConn.checkClosed();
        return this.proxyConn;
    }

    public XAResource getXAResource() throws SQLException {
        this.proxyConn.checkClosed();
        return this.proxyResource;
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
