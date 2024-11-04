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

/**
 * Base pool exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeConnectionPoolException extends BeeSQLException {

    public BeeConnectionPoolException(String s) {
        super(s);
    }

    public BeeConnectionPoolException(Throwable cause) {
        super(cause);
    }

    public BeeConnectionPoolException(String message, Throwable cause) {
        super(message, cause);
    }
}