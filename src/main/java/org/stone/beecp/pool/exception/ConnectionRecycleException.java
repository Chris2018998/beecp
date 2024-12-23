/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.pool.exception;

import org.stone.beecp.BeeSQLException;

/**
 * connection exception
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ConnectionRecycleException extends BeeSQLException {

    public ConnectionRecycleException(Throwable cause) {
        super(cause);
    }
}