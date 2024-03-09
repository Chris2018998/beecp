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

import java.sql.SQLException;

/**
 * Proxy Base Wrapper
 *
 * @author Chris Liao
 * @version 1.0
 */
abstract class ProxyBaseWrapper {
    protected final PooledConnection p;//called by subclass to update parkTime
    protected boolean isClosed;

    ProxyBaseWrapper(PooledConnection p) {
        this.p = p;
    }

    public boolean isWrapperFor(Class<?> clazz) {
        return clazz != null && clazz.isInstance(this);
    }

    public <T> T unwrap(Class<T> clazz) throws SQLException {
        if (clazz != null && clazz.isInstance(this))
            return clazz.cast(this);
        else
            throw new SQLException("Wrapped object was not an instance of " + clazz);
    }
}
