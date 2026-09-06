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
 * connection creation exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ConnectionCreationException extends BeeSQLException {

    public ConnectionCreationException(String message) {
        super(message);
    }

    public ConnectionCreationException(Throwable cause) {
        super(cause);
    }
}