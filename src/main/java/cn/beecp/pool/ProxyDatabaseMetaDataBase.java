/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * DatabaseMetaData proxy
 *
 * @author Chris.Liao
 * @version 1.0
 */
abstract class ProxyDatabaseMetaDataBase extends ProxyBaseWrapper implements DatabaseMetaData {
    protected final DatabaseMetaData raw;
    private final ProxyConnectionBase owner;//called by subclass to check close state

    public ProxyDatabaseMetaDataBase(DatabaseMetaData raw, PooledConnection p) {
        super(p);
        this.raw = raw;
        owner = p.proxyInUsing;
    }

    protected void checkClosed() throws SQLException {
        this.owner.checkClosed();
    }

    public Connection getConnection() throws SQLException {
        this.checkClosed();
        return this.owner;
    }
}
