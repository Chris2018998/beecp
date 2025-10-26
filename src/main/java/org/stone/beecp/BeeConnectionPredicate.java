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
     * Do test on a SQLException thrown from a borrowed connection,test result determine that whether the connection need been evicted from pool.
     *
     * @param e is thrown from a working connection
     * @return eviction reason,which is not blank and not null,pool evicts the connection,false that pool ignores it
     */
    String evictionTest(SQLException e);
}
