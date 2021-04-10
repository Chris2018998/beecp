/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp;

/**
 * configuration exception
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class BeeDataSourceConfigException extends RuntimeException {
    public BeeDataSourceConfigException() {
        super();
    }

    public BeeDataSourceConfigException(String s) {
        super(s);
    }

    public BeeDataSourceConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeeDataSourceConfigException(Throwable cause) {
        super(cause);
    }
}
