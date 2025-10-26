/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.pool;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * XAResource Proxy
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class XaProxyResource implements XAResource {
    private final XAResource res;
    private final ProxyConnectionBase proxyConn;

    XaProxyResource(XAResource raw, ProxyConnectionBase proxyConn) {
        this.res = raw;
        this.proxyConn = proxyConn;
    }

    private void checkClosed() throws XAException {
        if (this.proxyConn.isClosed())
            throw new XAException("No operations allowed after XAConnection closed");
    }

    public void start(Xid xid, int flags) throws XAException {
        this.checkClosed();
        this.res.start(xid, flags);
    }

    public int prepare(Xid xid) throws XAException {
        this.checkClosed();
        return this.res.prepare(xid);
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        this.checkClosed();
        this.res.commit(xid, onePhase);
    }

    public void rollback(Xid xid) throws XAException {
        this.checkClosed();
        this.res.rollback(xid);
    }

    public void end(Xid xid, int flags) throws XAException {
        this.checkClosed();
        this.res.end(xid, flags);
    }

    public void forget(Xid xid) throws XAException {
        this.checkClosed();
        this.res.forget(xid);
    }

    public Xid[] recover(int xid) throws XAException {
        this.checkClosed();
        return this.res.recover(xid);
    }

    public boolean isSameRM(XAResource res) throws XAException {
        this.checkClosed();
        return this == res;
    }

    public int getTransactionTimeout() throws XAException {
        this.checkClosed();
        return this.res.getTransactionTimeout();
    }

    public boolean setTransactionTimeout(int seconds) throws XAException {
        this.checkClosed();
        return this.res.setTransactionTimeout(seconds);
    }
}
