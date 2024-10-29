/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.objects;

import org.stone.beecp.BeeConnectionFactory;

import java.sql.Connection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

/**
 * Mock Impl on ConnectionFactory
 *
 * @author Chris Liao
 */
public class MockNetBlockConnectionFactory implements BeeConnectionFactory {
    private final CountDownLatch latch;

    public MockNetBlockConnectionFactory() {
        this(1);
    }

    public MockNetBlockConnectionFactory(int count) {
        latch = new CountDownLatch(count);
    }

    public Connection create() {
        latch.countDown();
        LockSupport.park();
        return null;
    }

    public void waitOnLatch() throws InterruptedException {
        latch.await();
    }
}
