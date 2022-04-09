/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool.exception;

/**
 * pool create exception
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class PoolCreateFailedException extends PoolBaseException {

    public PoolCreateFailedException(String s) {
        super(s);
    }

    public PoolCreateFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}