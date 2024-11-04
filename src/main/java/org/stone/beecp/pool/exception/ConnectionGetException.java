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
 * exception on getting Connection
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ConnectionGetException extends BeeSQLException {

    public ConnectionGetException(String message) {
        super(message);
    }

    public ConnectionGetException(Throwable cause) {
        super(cause);
    }
}