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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Lock extension impl provide interruption methods
 *
 * @author Chris Liao
 * @version 1.0
 */
public class InterruptionReentrantReadWriteLock extends ReentrantReadWriteLock {
    public InterruptionReentrantReadWriteLock() {
    }

    public InterruptionReentrantReadWriteLock(boolean fair) {
        super(fair);
    }

    public ReadLock readLock() {
        return super.readLock();
    }

    public WriteLock writeLock() {
        return super.writeLock();
    }

    public Thread interruptOwnerThread() {
        Thread owner = super.getOwner();
        if (owner != null) owner.interrupt();
        return owner;
    }

    public List<Thread> interruptQueuedWaitThreads() {
        Collection<Thread> waitThreads = super.getQueuedThreads();
        for (Thread thread : waitThreads)
            thread.interrupt();
        return new ArrayList<>(waitThreads);
    }
}
