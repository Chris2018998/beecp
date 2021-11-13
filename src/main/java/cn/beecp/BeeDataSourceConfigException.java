/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp;

/**
 * configuration exception
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class BeeDataSourceConfigException extends RuntimeException {

    public BeeDataSourceConfigException(String s) {
        super(s);
    }

    public BeeDataSourceConfigException(Throwable cause) {
        super(cause);
    }

    public BeeDataSourceConfigException(String message, Throwable cause) {
        super(message, cause);
    }

}
