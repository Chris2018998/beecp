/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool.xa;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * XAResource Proxy
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class MockXaResource implements XAResource {
    private static final Xid[] EMPTY_XID_ARRAY = {};
    private Xid xid;

    public void start(Xid xid, int flags) throws XAException {
        this.xid = xid;
    }

    public int prepare(Xid xid) throws XAException {
        return XAResource.XA_OK;
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        //do nothing
    }

    public void rollback(Xid xid) throws XAException {
        //do nothing
    }

    public void end(Xid xid, int flags) throws XAException {
        //do nothing
    }

    public void forget(Xid xid) throws XAException {
        //do nothing
    }

    public Xid[] recover(int xid) throws XAException {
        return EMPTY_XID_ARRAY;
    }

    public boolean isSameRM(XAResource xares) throws XAException {
        return this == xares;
    }

    public int getTransactionTimeout() throws XAException {
        return 0;
    }

    public boolean setTransactionTimeout(int seconds) throws XAException {
        return true;
    }
}
