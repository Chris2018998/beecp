/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.config;

import org.stone.beecp.SQLExceptionPredication;

import java.sql.SQLException;

public class DummySqlExceptionPredication implements SQLExceptionPredication {

    //return desc of eviction,if null or empty,not be evicted
    public String check(SQLException e) {
        return null;
    }
}
