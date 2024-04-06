/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.tools.extension;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Lock extension impl provide interruption methods
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class InterruptionReentrantLock extends ReentrantLock {

    public InterruptionReentrantLock() {
    }

    public InterruptionReentrantLock(boolean fair) {
        super(fair);
    }

    public void interruptOwnerThread() {
        Thread owner = super.getOwner();
        if (owner != null) owner.interrupt();
    }

    public void interruptQueuedWaitThreads() {
        for (Thread thread : super.getQueuedThreads())
            thread.interrupt();
    }
}
