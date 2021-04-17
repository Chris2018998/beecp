/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp.pool;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * DatabaseMetaDataBase wrapper
 *
 * @author Chris.Liao
 * @version 1.0
 */
abstract class ProxyDatabaseMetaDataBase implements DatabaseMetaData {
    protected DatabaseMetaData delegate;
    protected PooledConnection pCon;//called by subclass to update time
    private ProxyConnectionBase proxyCon;//called by subclass to check close state

    public ProxyDatabaseMetaDataBase(DatabaseMetaData metaData, PooledConnection pCon) {
        this.pCon = pCon;
        this.delegate = metaData;
        this.proxyCon = pCon.proxyCon;
    }

    protected final void checkClosed() throws SQLException {
        proxyCon.checkClosed();
    }

    public Connection getConnection() throws SQLException {
        checkClosed();
        return proxyCon;
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
