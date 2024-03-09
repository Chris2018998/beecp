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
 * Connection eviction test on SQLException
 *
 * @author Chris Liao
 * @version 1.0
 */

public interface SQLExceptionPredication {

    //return desc of eviction,if null or empty,not be evicted
    String check(SQLException e);
}


