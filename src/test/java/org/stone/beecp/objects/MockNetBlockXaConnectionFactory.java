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
import java.util.concurrent.CountDownLatch;

/**
 * Mock Impl on ConnectionFactory
 *
 * @author Chris Liao
 */
public class MockNetBlockXaConnectionFactory implements BeeXaConnectionFactory {

    private final CountDownLatch arrivalLatch;
    private final CountDownLatch blockingLatch;

    public MockNetBlockXaConnectionFactory() {
        this.arrivalLatch = new CountDownLatch(1);
        this.blockingLatch = new CountDownLatch(1);
    }

    public CountDownLatch getArrivalLatch() {
        return arrivalLatch;
    }

    public CountDownLatch getBlockingLatch() {
        return blockingLatch;
    }

    public XAConnection create() {
        arrivalLatch.countDown();
        try {
            blockingLatch.await();
        } catch (InterruptedException e) {
            //do nothing
        }
        return null;
    }
}
