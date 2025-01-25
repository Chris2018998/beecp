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
import java.util.concurrent.Semaphore;

/**
 * Semaphore extension impl provide interruption method
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class InterruptionSemaphore extends Semaphore {

    public InterruptionSemaphore(int permits, boolean fair) {
        super(permits, fair);
    }

    public List<Thread> getQueuedThreads() {
        return new ArrayList<>(super.getQueuedThreads());
    }

    public List<Thread> interruptQueuedWaitThreads() {
        Collection<Thread> waitThreads = super.getQueuedThreads();
        for (Thread thread : waitThreads)
            thread.interrupt();
        return new ArrayList<>(waitThreads);
    }
}