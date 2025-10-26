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
 * Throws this exception when set default value on connection
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ConnectionDefaultSetFailedException extends BeeSQLException {

    public ConnectionDefaultSetFailedException(String message) {
        super(message);
    }

    public ConnectionDefaultSetFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}