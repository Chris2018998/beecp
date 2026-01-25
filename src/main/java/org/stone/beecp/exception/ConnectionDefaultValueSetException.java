/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.exception;

/**
 * Throws this exception when set default value on connection
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ConnectionDefaultValueSetException extends BeeSQLException {

    public ConnectionDefaultValueSetException(String message) {
        super(message);
    }

    public ConnectionDefaultValueSetException(String message, Throwable cause) {
        super(message, cause);
    }
}