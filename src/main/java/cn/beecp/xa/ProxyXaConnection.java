/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp.xa;

import javax.sql.ConnectionEventListener;
import javax.sql.StatementEventListener;
import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import java.sql.Connection;
import java.sql.SQLException;

import static cn.beecp.pool.PoolStaticCenter.ConnectionClosedException;
import static cn.beecp.pool.PoolStaticCenter.XaConnectionClosedException;


/**
 * XaConnection Proxy
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class ProxyXaConnection implements XAConnection {
    private boolean isClosed;
    private XAConnection raw;
    private Connection proxyConn;
    private ProxyXaResource proxyXaResource;

    public ProxyXaConnection(XAConnection raw, Connection proxyConn) {
        this.raw = raw;
        this.proxyConn = proxyConn;
    }

    private void checkClosed() throws SQLException {
        if (isClosed) throw ConnectionClosedException;
    }

    void checkClosedForXa() throws XAException {
        if (isClosed) throw XaConnectionClosedException;
    }

    public void close() throws SQLException {
        isClosed = true;
        proxyConn.close();
    }

    public Connection getConnection() throws SQLException {
        checkClosed();
        return proxyConn;
    }

    public javax.transaction.xa.XAResource getXAResource() throws SQLException {
        checkClosed();
        if (proxyXaResource == null)
            proxyXaResource = new ProxyXaResource(raw.getXAResource(), this);
        return proxyXaResource;
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
