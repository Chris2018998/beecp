/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
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
    protected ResultSet delegate;
    protected PooledConnection pConn;//called by subclass to update tim
    boolean isClosed;
    private ProxyStatementBase owner;//called by subclass to check close state

    public ProxyResultSetBase(ResultSet delegate, PooledConnection pConn) {
        this.delegate = delegate;
        this.pConn = pConn;
    }

    public ProxyResultSetBase(ResultSet delegate, ProxyStatementBase owner, PooledConnection pConn) {
        this.delegate = delegate;
        this.owner = owner;
        this.pConn = pConn;
        owner.setOpenResultSet(this);
    }

    boolean isDelegate(ResultSet delegate) {
        return this.delegate == delegate;
    }

    public Statement getStatement() throws SQLException {
        checkClosed();
        return owner;
    }

    public final boolean isClosed() throws SQLException {
        return isClosed;
    }

    private final void checkClosed() throws SQLException {
        if (isClosed) throw ResultSetClosedException;
    }

    public final void close() throws SQLException {
        if (isClosed) return;
        try {
            isClosed = true;
            delegate.close();
        } finally {
            delegate = CLOSED_RSLT;
            /*** #40-start fix NullPointException(ResultSet from DatabaseMetaData) */
            if (owner != null) owner.removeOpenResultSet(this);
            /*** #40-end *******************/
        }
    }

    final void setAsClosed() {//call by ProxyStatementBase.close
        try {
            close();
        } catch (Throwable e) {
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

    boolean containsDelegate(ResultSet delegate) {
        return this.delegate == delegate;
    }
}
