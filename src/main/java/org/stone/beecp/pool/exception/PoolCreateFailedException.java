/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.pool.exception;

import org.stone.beecp.BeeConnectionPoolException;

/**
 * pool create failed
 *
 * @author Chris Liao
 * @version 1.0
 */
public class PoolCreateFailedException extends BeeConnectionPoolException {
    public PoolCreateFailedException(String s, Throwable e) {
        super(s, e);
    }
}