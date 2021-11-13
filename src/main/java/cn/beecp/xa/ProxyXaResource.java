/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.xa;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * XAResource Proxy
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class ProxyXaResource implements XAResource {
    private XAResource raw;
    private ProxyXaConnection owner;

    public ProxyXaResource(XAResource raw, ProxyXaConnection owner) {
        this.raw = raw;
        this.owner = owner;
    }

    public void commit(Xid var1, boolean var2) throws XAException {
        owner.checkClosedForXa();
        raw.commit(var1, var2);
    }

    public void end(Xid var1, int var2) throws XAException {
        owner.checkClosedForXa();
        raw.end(var1, var2);
    }

    public void forget(Xid var1) throws XAException {
        owner.checkClosedForXa();
        raw.forget(var1);
    }

    public int getTransactionTimeout() throws XAException {
        owner.checkClosedForXa();
        return raw.getTransactionTimeout();
    }

    public boolean isSameRM(XAResource var1) throws XAException {
        owner.checkClosedForXa();
        return raw.isSameRM(var1);
    }

    public int prepare(Xid var1) throws XAException {
        owner.checkClosedForXa();
        return raw.prepare(var1);
    }

    public Xid[] recover(int var1) throws XAException {
        owner.checkClosedForXa();
        return raw.recover(var1);
    }

    public void rollback(Xid var1) throws XAException {
        owner.checkClosedForXa();
        raw.rollback(var1);
    }

    public boolean setTransactionTimeout(int var1) throws XAException {
        owner.checkClosedForXa();
        return raw.setTransactionTimeout(var1);
    }

    public void start(Xid var1, int var2) throws XAException {
        owner.checkClosedForXa();
        raw.start(var1, var2);
    }
}
