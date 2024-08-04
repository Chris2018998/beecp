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
 * Predicate interface
 *
 * @author Chris Liao
 * @version 1.0
 */

public interface BeeConnectionPredicate {

    /**
     * Does a test on a sql exception for connection eviction.
     *
     * @param e is a sql exception
     * @return a string as eviction cause,if null or empty,not be evicted
     */
    String evictTest(SQLException e);
}
