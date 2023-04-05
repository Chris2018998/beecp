/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package cn.beecp;

import java.sql.SQLException;

/**
 * pool exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeConnectionPoolException extends SQLException {

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