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
 * Pool base exception.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeDataSourcePoolException extends BeeSQLException {

    public BeeDataSourcePoolException(String s) {
        super(s);
    }

    public BeeDataSourcePoolException(Throwable cause) {
        super(cause);
    }

    public BeeDataSourcePoolException(String message, Throwable cause) {
        super(message, cause);
    }
}