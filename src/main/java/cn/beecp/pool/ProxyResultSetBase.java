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
 * ResultSet proxy base class
 *
 * @author Chris.Liao
 * @version 1.0
 */
abstract class ProxyResultSetBase implements ResultSet {
    protected final PooledConnection p;//called by subclass to update tim
    protected ResultSet raw;
    boolean isClosed;
    private ProxyStatementBase owner;//called by subclass to check close state

    public ProxyResultSetBase(final ResultSet raw, final PooledConnection p) {
        this.raw = raw;
        this.p = p;
    }

    public ProxyResultSetBase(final ResultSet raw, final ProxyStatementBase o, final PooledConnection p) {
        o.setOpenResultSet(this);
        this.raw = raw;
        this.owner = o;
        this.p = p;
    }

    /******************************************************************************************
     *                                                                                        *
     *                        Below are self define methods                                          *
     *                                                                                        *
     ******************************************************************************************/

    boolean containsRaw(ResultSet raw) {
        return this.raw == raw;
    }

    /*******************************************************************************************
     *                                                                                         *
     *                         Below are override methods                                      *
     *                                                                                         *
     *******************************************************************************************/

    public Statement getStatement() throws SQLException {
        if (isClosed) throw ResultSetClosedException;
        return owner;
    }

    public final boolean isClosed() throws SQLException {
        return isClosed;
    }

    public final void close() throws SQLException {
        if (isClosed) return;
        try {
            isClosed = true;
            raw.close();
        } finally {
            raw = CLOSED_RSLT;
            /*** #40-start fix NullPointException(ResultSet from DatabaseMetaData) */
            if (owner != null) owner.removeOpenResultSet(this);
            /*** #40-end *******************/
        }
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this))
            return (T) this;
        else
            throw new SQLException("Wrapped object is not an instance of " + iface);
    }
}
