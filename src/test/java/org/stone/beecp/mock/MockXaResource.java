/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.mock;

import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * XAResource Proxy
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class MockXaResource implements XAResource {
    private static final Xid[] EMPTY_XID_ARRAY = {};

    public void start(Xid xid, int flags) {
        //do nothing
    }

    public int prepare(Xid xid) {
        return XAResource.XA_OK;
    }

    public void commit(Xid xid, boolean onePhase) {
        //do nothing
    }

    public void rollback(Xid xid) {
        //do nothing
    }

    public void end(Xid xid, int flags) {
        //do nothing
    }

    public void forget(Xid xid) {
        //do nothing
    }

    public Xid[] recover(int xid) {
        return EMPTY_XID_ARRAY;
    }

    public boolean isSameRM(XAResource xares) {
        return this == xares;
    }

    public int getTransactionTimeout() {
        return 0;
    }

    public boolean setTransactionTimeout(int seconds) {
        return true;
    }
}
