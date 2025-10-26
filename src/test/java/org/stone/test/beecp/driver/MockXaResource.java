/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.driver;

import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * XAResource Proxy
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class MockXaResource implements XAResource {
    private final MockConnection connection;
    private Xid currentXid;

    public MockXaResource(MockConnection connection) {
        this.connection = connection;
    }

    public void start(Xid xid, int flags) {
        this.currentXid = xid;
    }

    public int prepare(Xid xid) {
        return XAResource.XA_OK;
    }

    public void commit(Xid xid, boolean onePhase) {
        this.connection.commit();
    }

    public void rollback(Xid xid) {
        this.connection.rollback();
    }

    public void end(Xid xid, int flags) {
        //do nothing
    }

    public void forget(Xid xid) {
        //do nothing
    }

    public Xid[] recover(int flag) {
        return new Xid[]{currentXid};
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
