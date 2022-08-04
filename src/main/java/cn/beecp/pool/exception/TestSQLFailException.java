/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool.exception;

import java.sql.SQLException;

/**
 * if test sql execute failed,then throws this exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TestSQLFailException extends SQLException {

    public TestSQLFailException(String message, Throwable cause) {
        super(message, cause);
    }
}