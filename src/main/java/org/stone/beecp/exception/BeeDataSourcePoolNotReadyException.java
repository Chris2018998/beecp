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
 * Throws this exception when datasource pool rejects methods call.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeDataSourcePoolNotReadyException extends ConnectionGetException {

    public BeeDataSourcePoolNotReadyException(String s) {
        super(s);
    }
}