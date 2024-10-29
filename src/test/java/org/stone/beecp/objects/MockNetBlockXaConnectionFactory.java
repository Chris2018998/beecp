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

import org.stone.beecp.BeeXaConnectionFactory;

import javax.sql.XAConnection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Mock Impl on ConnectionFactory
 *
 * @author Chris Liao
 */
public class MockNetBlockXaConnectionFactory implements BeeXaConnectionFactory {
    private final AtomicInteger counter = new AtomicInteger();

    public XAConnection create() {
        try {
            counter.incrementAndGet();
            LockSupport.park();
            return null;
        } finally {
            counter.decrementAndGet();
        }
    }

    public void waitForCount(int expect) {
        for (; ; ) {
            if (counter.get() == expect) {
                return;
            } else {
                LockSupport.parkNanos(5L);
            }
        }
    }
}
