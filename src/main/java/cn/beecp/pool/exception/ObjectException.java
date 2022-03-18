/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beeop.pool.exception;

/**
 * Bee object exception
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class ObjectException extends Exception {
    public ObjectException(String s) {
        super(s);
    }

    public ObjectException(Throwable cause) {
        super(cause);
    }
}
