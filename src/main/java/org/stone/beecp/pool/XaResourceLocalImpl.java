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
import java.sql.SQLException;

import static javax.transaction.xa.XAException.XAER_DUPID;

/**
 * XAResource implementation for local connection
 *
 * @author Chris Liao
 * @version 1.0
 */
public class XaResourceLocalImpl implements XAResource {
    private final ProxyConnectionBase proxyConn;
    private Xid currentXid;//set from <method>start</method>

    XaResourceLocalImpl(ProxyConnectionBase proxyConn) {
        this.proxyConn = proxyConn;
    }

    //***************************************************************************************************************//
    //                                      1:reset and check methods(2)                                             //                                                                                  //
    //***************************************************************************************************************//
    //check Xid
    private void checkXid(Xid xid) throws XAException {
        if (xid == null) throw new XAException("Xid can't be null");
        if (currentXid == null) throw new XAException("There is no current transaction");
        if (!currentXid.equals(xid))
            throw new XAException("Invalid Xid,expected " + currentXid + ", but was " + xid);
    }

    private void checkClosed() throws XAException {
        if (this.proxyConn.isClosed())
            throw new XAException("No operations allowed after XAConnection closed");
    }

    //***************************************************************************************************************//
    //                                      2:override methods(11)                                                   //                                                                                  //
    //***************************************************************************************************************//
    public synchronized void start(Xid xid, int flags) throws XAException {
        if (xid == null) throw new XAException("Xid can't be null");
        this.checkClosed();

        if (flags == XAResource.TMJOIN) {
            if (currentXid != null) throw new XAException("Resource has in a transaction");
            try {
                if (this.proxyConn.isReadOnly())
                    throw new XAException("Connection cannot be readonly");
                if (this.proxyConn.getAutoCommit())
                    this.proxyConn.setAutoCommit(false);//support transaction
            } catch (SQLException e) {
                throw new XAException("Failed to set 'autoCommit' to false for transaction");
            }

            currentXid = xid;
        } else if (flags == XAResource.TMRESUME) {
            if (currentXid == null) throw new XAException("Resource not join in a transaction");

            if (!xid.equals(currentXid))
                throw new XAException("Invalid Xid,expected " + currentXid + ", but was " + xid);
        } else if (flags != XAResource.TMNOFLAGS) {
            throw new XAException(XAER_DUPID);
        }
    }

    public synchronized void end(Xid xid, int flags) throws XAException {
        this.checkXid(xid);
        this.checkClosed();
    }

    public synchronized int prepare(Xid xid) throws XAException {
        this.checkXid(xid);
        return XAResource.XA_OK;
    }

    public synchronized void commit(Xid xid, boolean onePhase) throws XAException {
        this.checkXid(xid);
        this.checkClosed();

        try {
            this.proxyConn.commit();
        } catch (SQLException e) {
            throw new XAException(e.getMessage());
        } finally {
            this.currentXid = null;
        }
    }

    public synchronized void rollback(Xid xid) throws XAException {
        this.checkXid(xid);
        this.checkClosed();

        try {
            this.proxyConn.rollback();
        } catch (SQLException e) {
            throw (XAException) new XAException().initCause(e);
        } finally {
            this.currentXid = null;
        }
    }

    public synchronized void forget(Xid xid) throws XAException {
        this.checkXid(xid);
        this.checkClosed();
    }

    public Xid[] recover(int flag) throws XAException {
        this.checkClosed();
        return new Xid[]{currentXid};
    }

    public boolean isSameRM(XAResource xaResource) throws XAException {
        this.checkClosed();
        return this == xaResource;
    }

    public int getTransactionTimeout() throws XAException {
        this.checkClosed();
        return 0;
    }

    public boolean setTransactionTimeout(int transactionTimeout) throws XAException {
        this.checkClosed();
        return false;
    }
}
