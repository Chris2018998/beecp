/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool.exception;

import java.sql.SQLException;

/**
 * pool exception
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class PoolBaseException extends SQLException {

    PoolBaseException(String s) {
        super(s);
    }

    PoolBaseException(Throwable cause) {
        super(cause);
    }

    PoolBaseException(String message, Throwable cause) {
        super(message, cause);
    }
}