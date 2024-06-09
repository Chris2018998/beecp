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

import java.sql.ResultSetMetaData;

/**
 * ResultSet Meta Data proxy impl
 *
 * @author Chris Liao
 * @version 1.0
 */

abstract class ProxyResultSetMetaDataBase extends ProxyBaseWrapper implements ResultSetMetaData {
    protected final ResultSetMetaData raw;
    protected final ProxyResultSetBase owner;//called by subclass to check close state

    ProxyResultSetMetaDataBase(ResultSetMetaData raw, ProxyResultSetBase owner, PooledConnection p) {
        super(p);
        this.raw = raw;
        this.owner = owner;
    }

    public String toString() {
        return raw.toString();
    }
}
