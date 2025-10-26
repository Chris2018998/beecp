/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.objects.threads;

import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A blocker implementation
 *
 * @author Chris Liao
 */
public class MockBlocking {
    public static final int Blocking_By_Park = 1;
    public static final int Blocking_By_Park_Time = 2;

    public static final int Blocking_By_CountDownLatch = 3;
    public static final int Blocking_By_CountDownLatch_Time = 4;

    public static final int Blocking_By_CyclicBarrier = 5;
    public static final int Blocking_By_CyclicBarrier_Time = 6;

    public static final int Blocking_By_LockCondition = 7;
    public static final int Blocking_By_LockCondition_Time = 8;

    public static final int Blocking_By_Synchronized_Object = 9;
    public static final int Blocking_By_Synchronized_Object_Time = 10;

    private int blockingWay;
    private long blockingTime;
    private TimeUnit blockingTimeUnit;
    private CountDownLatch countDownLatch;
    private CyclicBarrier cyclicBarrier;
    private ReentrantLock lock;
    private Condition lockCondition;
    private Object synchronizedObject;

    public int getBlockingWay() {
        return blockingWay;
    }

    public void setBlocker() {
        this.blockingWay = Blocking_By_Park;
    }

    public void setBlocker(long blockingTime, TimeUnit blockingTimeUnit) {
        this.blockingWay = Blocking_By_Park_Time;
        this.blockingTime = blockingTime;
        this.blockingTimeUnit = blockingTimeUnit;
    }

    public void setBlocker(CountDownLatch countDownLatch) {
        this.blockingWay = Blocking_By_CountDownLatch;
        this.countDownLatch = countDownLatch;
    }

    public void setBlocker(CountDownLatch countDownLatch, long blockingTime, TimeUnit blockingTimeUnit) {
        this.blockingWay = Blocking_By_CountDownLatch_Time;
        this.countDownLatch = countDownLatch;
        this.blockingTime = blockingTime;
        this.blockingTimeUnit = blockingTimeUnit;
    }

    public void setBlocker(CyclicBarrier cyclicBarrier) {
        this.blockingWay = Blocking_By_CyclicBarrier;
        this.cyclicBarrier = cyclicBarrier;
    }

    public void setBlocker(CyclicBarrier cyclicBarrier, long blockingTime, TimeUnit blockingTimeUnit) {
        this.blockingWay = Blocking_By_CyclicBarrier_Time;
        this.cyclicBarrier = cyclicBarrier;
        this.blockingTime = blockingTime;
        this.blockingTimeUnit = blockingTimeUnit;
    }

    public void setBlocker(ReentrantLock lock, Condition lockCondition) {
        this.blockingWay = Blocking_By_LockCondition;
        this.lock = lock;
        this.lockCondition = lockCondition;
    }

    public void setBlocker(ReentrantLock lock, Condition lockCondition, long blockingTime, TimeUnit blockingTimeUnit) {
        this.blockingWay = Blocking_By_LockCondition_Time;
        this.lock = lock;
        this.lockCondition = lockCondition;
        this.blockingTime = blockingTime;
        this.blockingTimeUnit = blockingTimeUnit;
    }

    public void setBlocker(Object synchronizedObject) {
        this.blockingWay = Blocking_By_Synchronized_Object;
        this.synchronizedObject = synchronizedObject;
    }

    public void setBlocker(Object synchronizedObject, long blockingTime, TimeUnit blockingTimeUnit) {
        this.blockingWay = Blocking_By_Synchronized_Object;
        this.synchronizedObject = synchronizedObject;
        this.blockingTime = blockingTime;
        this.blockingTimeUnit = blockingTimeUnit;
    }

    public void mockBlocking() throws InterruptedException, BrokenBarrierException, TimeoutException {
        switch (blockingWay) {
            case Blocking_By_Park: {
                LockSupport.park();
                break;
            }

            case Blocking_By_Park_Time: {
                LockSupport.parkNanos(blockingTimeUnit.toNanos(this.blockingTime));
                break;
            }

            case Blocking_By_CountDownLatch: {
                countDownLatch.await();
                break;
            }

            case Blocking_By_CountDownLatch_Time: {
                countDownLatch.await(blockingTime, blockingTimeUnit);
                break;
            }

            case Blocking_By_CyclicBarrier: {
                cyclicBarrier.await();
                break;
            }

            case Blocking_By_CyclicBarrier_Time: {
                cyclicBarrier.await(blockingTime, blockingTimeUnit);
                break;
            }

            case Blocking_By_LockCondition: {
                lock.lock();
                try {
                    lockCondition.await();
                } finally {
                    lock.unlock();
                }
            }

            case Blocking_By_LockCondition_Time: {
                lock.lock();
                try {
                    lockCondition.await(blockingTime, blockingTimeUnit);
                } finally {
                    lock.unlock();
                }
            }

            case Blocking_By_Synchronized_Object: {
                synchronized (synchronizedObject) {
                    synchronizedObject.wait();
                }
            }

            case Blocking_By_Synchronized_Object_Time: {
                synchronized (synchronizedObject) {
                    synchronizedObject.wait(blockingTimeUnit.toMillis(this.blockingTime));
                }
            }

            default:
                break;
        }
    }
}
