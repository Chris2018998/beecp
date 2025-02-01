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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;

/**
 * Mock Impl on ConnectionFactory
 *
 * @author Chris Liao
 */
public class MockNetBlockBaseFactory {
    protected final Map<Thread, BlockingState> blockingMap = new ConcurrentHashMap<>(1);

    public void interrupt(Thread thread) {
        if (blockingMap.containsKey(thread)) thread.interrupt();
    }

    public void unparkToCreate(Thread thread) {
        BlockingState blockingInfo = blockingMap.get(thread);
        if (blockingInfo != null) {
            blockingInfo.setState(1);
            LockSupport.unpark(thread);
        }
    }

    public void unparkToExit(Thread thread) {
        BlockingState blockingInfo = blockingMap.get(thread);
        if (blockingInfo != null) {
            blockingInfo.setState(2);
            LockSupport.unpark(thread);
        }
    }
}
