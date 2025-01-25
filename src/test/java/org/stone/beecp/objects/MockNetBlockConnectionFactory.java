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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

/**
 * Mock Impl on ConnectionFactory
 *
 * @author Chris Liao
 */
public class MockNetBlockConnectionFactory implements BeeConnectionFactory {
    private final ConcurrentLinkedQueue<Thread> waitQueue = new ConcurrentLinkedQueue<>();
    private volatile int state = 0;
    private volatile Thread waitThread = null;

    public void interruptAll() {
        this.state = 1;
        for (Thread thread : waitQueue) {
            LockSupport.unpark(thread);
        }
    }

    public boolean inWaitQueue(Thread thread) {
        return waitQueue.contains(thread);
    }

    public void interrupt(Thread thread) {
        LockSupport.unpark(thread);
        thread.interrupt();
    }

    public void waitUtilCreationArrival() {
        this.waitThread = Thread.currentThread();
        Thread.interrupted();//just clean interruption flag
        do {
            if (waitQueue.isEmpty())
                LockSupport.parkNanos(50L);
            else
                break;
        } while (state == 0);
    }

    public Connection create() {
        Thread.interrupted();
        Thread currentThread = Thread.currentThread();
        try {
            if (state == 0) {
                waitQueue.add(currentThread);
                if (waitThread != null) LockSupport.unpark(waitThread);
                do {
                    LockSupport.park();
                    if (Thread.interrupted()) {
                        break;
                    }
                } while (state == 0);
            }
        } finally {
            waitQueue.remove(currentThread);
        }
        return null;
    }
}
