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
 * Throws this exception when call on datasource if its pool is null(lazy creation mode).
 * Four methods call may drive datasource to instantiate pool instance under lazy mode.
 * 1: BeeDataSource.getConnection()
 * 2: BeeDataSource.getConnection(String,String)
 * 3: BeeDataSource.getXAConnection()
 * 4: BeeDataSource.getXAConnection(String,String)
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeDataSourcePoolLazyInitializationException extends BeeDataSourcePoolException {

    public BeeDataSourcePoolLazyInitializationException(String s) {
        super(s);
    }
}

