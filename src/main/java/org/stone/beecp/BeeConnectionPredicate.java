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
 * Connection predicate interface.
 *
 * @author Chris Liao
 * @version 1.0
 */

public interface BeeConnectionPredicate {

    /**
     * Test a SQLException thrown from a connection,test result determine connection whether evicted from pool.
     *
     * @param e thrown from a working connection
     * @return a string as eviction reason,but it is null or empty,not evict target connection
     */
    String evictTest(SQLException e);
}
