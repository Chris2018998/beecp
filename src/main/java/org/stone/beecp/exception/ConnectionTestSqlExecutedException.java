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
 * if test sql execute failed,then throws this exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ConnectionTestSqlExecutedException extends BeeSQLException {

    public ConnectionTestSqlExecutedException(String message, Throwable cause) {
        super(message, cause);
    }
}