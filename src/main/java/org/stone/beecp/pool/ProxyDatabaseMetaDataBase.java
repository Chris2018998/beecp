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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * DatabaseMetaData proxy
 *
 * @author Chris Liao
 * @version 1.0
 */
abstract class ProxyDatabaseMetaDataBase extends ProxyBaseWrapper implements DatabaseMetaData {
    protected final DatabaseMetaData raw;
    private final ProxyConnectionBase owner;//called by subclass to check close state

    ProxyDatabaseMetaDataBase(DatabaseMetaData raw, PooledConnection p) {
        super(p);
        this.raw = raw;
        owner = p.proxyInUsing;
    }

    final void checkClosed() throws SQLException {
        this.owner.checkClosed();
    }

    public Connection getConnection() throws SQLException {
        this.checkClosed();
        return this.owner;
    }

    public String toString() {
        return raw.toString();
    }
}
