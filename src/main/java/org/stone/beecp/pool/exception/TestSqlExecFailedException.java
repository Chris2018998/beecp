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

import java.sql.SQLException;

/**
 * if test sql execute failed,then throws this exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TestSqlExecFailedException extends SQLException {

    public TestSqlExecFailedException(String s) {
        super(s);
    }

    public TestSqlExecFailedException(Throwable cause) {
        super(cause);
    }

    public TestSqlExecFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}