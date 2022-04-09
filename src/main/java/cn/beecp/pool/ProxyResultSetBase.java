/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static cn.beecp.pool.PoolStaticCenter.CLOSED_RSLT;
import static cn.beecp.pool.PoolStaticCenter.ResultSetClosedException;

/**
 * ResultSet statement base class
 *
 * @author Chris.Liao
 * @version 1.0
 */
abstract class ProxyResultSetBase extends ProxyBaseWrapper implements ResultSet {
    protected ResultSet raw;
    private ProxyStatementBase owner;//called by subclass to check close state

    public ProxyResultSetBase(ResultSet raw, PooledConnection p) {
        super(p);
        this.raw = raw;
    }

    public ProxyResultSetBase(ResultSet raw, ProxyStatementBase o, PooledConnection p) {
        super(p);
        o.setOpenResultSet(this);
        this.raw = raw;
        owner = o;
    }

    //***************************************************************************************************************//
    //                                             Below are self-define methods                                     //
    //***************************************************************************************************************//
    boolean containsRaw(ResultSet raw) {
        return this.raw == raw;
    }

    //***************************************************************************************************************//
    //                                              Below are override methods                                       //
    //***************************************************************************************************************//
    public Statement getStatement() throws SQLException {
        if (this.isClosed) throw ResultSetClosedException;
        return this.owner;
    }

    public boolean isClosed() {
        return this.isClosed;
    }

    public final void close() throws SQLException {
        if (this.isClosed) return;
        try {
            this.isClosed = true;
            this.raw.close();
        } finally {
            this.raw = CLOSED_RSLT;
            if (this.owner != null) this.owner.removeOpenResultSet(this);
        }
    }
}
