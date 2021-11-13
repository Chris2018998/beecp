/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.xa;

import cn.beecp.pool.ProxyConnectionBase;

import javax.sql.ConnectionEventListener;
import javax.sql.StatementEventListener;
import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import java.sql.Connection;
import java.sql.SQLException;

import static cn.beecp.pool.PoolStaticCenter.XaConnectionClosedException;

/**
 * XaConnection Proxy
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class ProxyXaConnection implements XAConnection {
    private XAConnection raw;
    private ProxyXaResource proxyXaResource;
    private ProxyConnectionBase proxyBaseConn;

    public ProxyXaConnection(XAConnection raw, ProxyConnectionBase proxyCon) {
        this.raw = raw;
        this.proxyBaseConn = proxyCon;
    }

    void checkClosedForXa() throws XAException {
        if (proxyBaseConn.getClosedInd())
            throw XaConnectionClosedException;
    }

    public void close() throws SQLException {
        proxyBaseConn.close();
    }

    public Connection getConnection() throws SQLException {
        proxyBaseConn.checkClosed();
        return proxyBaseConn;
    }

    public javax.transaction.xa.XAResource getXAResource() throws SQLException {
        proxyBaseConn.checkClosed();
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
