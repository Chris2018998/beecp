/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.objects.pool;

import org.stone.beecp.BeeDataSourceCreationException;
import org.stone.beecp.pool.exception.PoolCreateFailedException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris Liao
 */
public class BlockingPoolImpl_ParkNanos extends BaseSimplePoolImpl {

    public BlockingPoolImpl_ParkNanos() {
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));
        if (Thread.interrupted())
            throw new BeeDataSourceCreationException(new PoolCreateFailedException("Interruption occurred during pool being instantiated", new InterruptedException()));
    }
}
