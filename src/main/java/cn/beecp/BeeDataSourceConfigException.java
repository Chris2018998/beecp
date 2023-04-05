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

/**
 * configuration exception
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class BeeDataSourceConfigException extends RuntimeException {

    public BeeDataSourceConfigException(String s) {
        super(s);
    }

    public BeeDataSourceConfigException(String message, Throwable cause) {
        super(message, cause);
    }

}
