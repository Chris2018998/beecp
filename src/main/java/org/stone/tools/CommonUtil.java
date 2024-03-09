/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.tools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * common util
 *
 * @author Chris Liao
 * @version 1.0
 */

public class CommonUtil {
    public static final int NCPU = Runtime.getRuntime().availableProcessors();
    public static final long spinForTimeoutThreshold = 1023L;
    public static final int maxTimedSpins = (NCPU < 2) ? 0 : 32;

    public static String trimString(String value) {
        return value == null ? null : value.trim();
    }

    public static boolean objectEquals(Object a, Object b) {
        return a == b || a != null && a.equals(b);
    }

    public static boolean isBlank(String str) {
        if (str == null) return true;
        for (int i = 0, l = str.length(); i < l; ++i) {
            if (!Character.isWhitespace((int) str.charAt(i)))
                return false;
        }
        return true;
    }

    //xor
    public static int advanceProbe(int probe) {
        probe ^= probe << 13;
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        return probe;
    }

    public static void interruptExclusiveOwnerThread(ReentrantLock lock) throws Exception {
        Field syncField = lock.getClass().getDeclaredField("sync");
        if (!syncField.isAccessible()) syncField.setAccessible(true);
        Object sync = syncField.get(lock);

        Method ownerThreadsMethod = AbstractOwnableSynchronizer.class.getDeclaredMethod("getExclusiveOwnerThread");//protected
        ownerThreadsMethod.setAccessible(true);
        Thread ownerThread = (Thread) ownerThreadsMethod.invoke(sync, new Object[0]);
       if(ownerThread!=null) ownerThread.interrupt();
    }

    public static void interruptQueuedWaitersOnLock(ReentrantLock lock) throws Exception {
        Field syncField = lock.getClass().getDeclaredField("sync");
        if (!syncField.isAccessible()) syncField.setAccessible(true);
        Object sync = syncField.get(lock);//get sync object under ReentrantLock

        Method getMethod = AbstractQueuedSynchronizer.class.getDeclaredMethod("getExclusiveQueuedThreads");
        getMethod.setAccessible(true);
        Collection<Thread> waitingThreads = (Collection<Thread>) getMethod.invoke(sync, new Object[0]);
        if (waitingThreads != null) {
            for (Thread thread : waitingThreads) {
                thread.interrupt();
            }
        }
    }
}
