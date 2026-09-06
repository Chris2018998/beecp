/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.exception;

/**
 * Throws this exception when data source pool re-startup fail.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeDataSourcePoolRestartFailedException extends BeeDataSourcePoolException {

    public BeeDataSourcePoolRestartFailedException(String s) {
        super(s);
    }

    public BeeDataSourcePoolRestartFailedException(String s, Throwable cause) {
        super(s, cause);
    }
}