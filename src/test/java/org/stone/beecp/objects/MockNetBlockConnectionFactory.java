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
import org.stone.beecp.driver.MockConnection;

import java.sql.Connection;
import java.util.concurrent.locks.LockSupport;

/**
 * Mock Impl on ConnectionFactory
 *
 * @author Chris Liao
 */
public class MockNetBlockConnectionFactory extends MockNetBlockBaseFactory implements BeeConnectionFactory {

    public Connection create() {
        Thread creationThread = Thread.currentThread();
        try {
            BlockingState state = new BlockingState();
            blockingMap.put(creationThread, state);

            while (state.getState() == 0) {
                LockSupport.park();
                if (creationThread.isInterrupted()) {
                    return null;
                }
            }
            return state.getState() == 1 ? new MockConnection() : null;
        } finally {
            blockingMap.remove(creationThread);
        }
    }
}
