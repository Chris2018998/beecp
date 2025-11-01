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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Lock extension impl provide interruption methods
 *
 * @author Chris Liao
 * @version 1.0
 */
public class InterruptableReentrantReadWriteLock extends ReentrantReadWriteLock {

    public Thread getOwnerThread() {
        return super.getOwner();
    }

    public List<Thread> getQueuedThreads() {
        return new ArrayList<>(super.getQueuedThreads());
    }

    public List<Thread> interruptAllThreads() {
        List<Thread> threadLlist = new LinkedList<>();
        for (Thread thread : super.getQueuedThreads()) {
            thread.interrupt();
            threadLlist.add(thread);
        }

        Thread owner = super.getOwner();
        if (owner != null) {
            owner.interrupt();
            threadLlist.add(owner);
        }
        return threadLlist;
    }
}
