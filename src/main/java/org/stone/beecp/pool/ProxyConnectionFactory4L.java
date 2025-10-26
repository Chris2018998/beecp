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
 * proxy factory
 *
 * @author Chris Liao
 * @version 1.0
 */

final class ProxyConnectionFactory4L extends ProxyConnectionFactory {
    private final MethodExecutionLogCache logCache;

    ProxyConnectionFactory4L(MethodExecutionLogCache logCache) {
        this.logCache = logCache;
    }

    public ProxyConnectionBase createProxyConnection(PooledConnection p) throws SQLException {
        throw new SQLException("Proxy classes for log manager not be generated,please execute 'ProxyClassGenerator' after compile");
    }
}
