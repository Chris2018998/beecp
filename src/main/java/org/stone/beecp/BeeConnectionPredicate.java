/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp;

import java.sql.SQLException;

/**
 * A predicate interface on pooled connections
 *
 * @author Chris Liao
 * @version 1.0
 */

public interface BeeConnectionPredicate {

    /**
     * Tests on a sql exception thrown from a connection,if result is not null/empty,then evict the connection from pool.
     *
     * @param e is a sql exception thrown from a connection
     * @return eviction cause,which can be null or empty
     */
    String evictTest(SQLException e);
}
