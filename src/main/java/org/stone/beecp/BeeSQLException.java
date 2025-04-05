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

import java.sql.SQLException;

/**
 * Bee SQL exception extends from {@link SQLException}.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeSQLException extends SQLException {

    public BeeSQLException(String s) {
        super(s);
    }

    public BeeSQLException(Throwable cause) {
        super(cause);
    }

    public BeeSQLException(String reason, Throwable cause) {
        super(reason, cause);
    }

    public int getErrorCode() {
        Throwable cause = super.getCause();
        if (cause instanceof SQLException) {
            return ((SQLException) cause).getErrorCode();
        } else {
            return 0;
        }
    }

    public String getSQLState() {
        Throwable cause = super.getCause();
        if (cause instanceof SQLException) {
            return ((SQLException) cause).getSQLState();
        } else {
            return null;
        }
    }

    public String getMessage() {
        String message = super.getMessage();
        if (message != null && !message.isEmpty()) return message;

        Throwable cause = super.getCause();
        return cause != null ? cause.getMessage() : null;
    }
}
