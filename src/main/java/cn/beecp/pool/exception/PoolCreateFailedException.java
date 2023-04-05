/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package cn.beecp.pool.exception;

import cn.beecp.BeeConnectionPoolException;

/**
 * pool create failed
 *
 * @author Chris Liao
 * @version 1.0
 */
public class PoolCreateFailedException extends BeeConnectionPoolException {
    public PoolCreateFailedException(String s) {
        super(s);
    }

    public PoolCreateFailedException(String s, Throwable e) {
        super(s, e);
    }
}