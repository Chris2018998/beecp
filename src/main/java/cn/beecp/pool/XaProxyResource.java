/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import static cn.beecp.pool.PoolStaticCenter.XAConnectionClosedException;

/**
 * XAResource Proxy
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class XaProxyResource implements XAResource {
    private final XAResource raw;
    private final ProxyConnectionBase proxyConn;

    XaProxyResource(XAResource raw, ProxyConnectionBase proxyConn) {
        this.raw = raw;
        this.proxyConn = proxyConn;
    }

    private void checkClosed() throws XAException {
        if (proxyConn.getClosedInd())
            throw XAConnectionClosedException;
    }

    public void start(Xid xid, int flags) throws XAException {
        checkClosed();
        raw.start(xid, flags);
    }

    public int prepare(Xid xid) throws XAException {
        checkClosed();
        return raw.prepare(xid);
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        checkClosed();
        raw.commit(xid, onePhase);
    }

    public void rollback(Xid xid) throws XAException {
        checkClosed();
        raw.rollback(xid);
    }

    public void end(Xid xid, int flags) throws XAException {
        checkClosed();
        raw.end(xid, flags);
    }

    public void forget(Xid xid) throws XAException {
        checkClosed();
        raw.forget(xid);
    }

    public Xid[] recover(int xid) throws XAException {
        checkClosed();
        return raw.recover(xid);
    }

    public boolean isSameRM(XAResource xares) throws XAException {
        checkClosed();
        return this == xares;
    }

    public int getTransactionTimeout() throws XAException {
        checkClosed();
        return raw.getTransactionTimeout();
    }

    public boolean setTransactionTimeout(int seconds) throws XAException {
        checkClosed();
        return raw.setTransactionTimeout(seconds);
    }
}
