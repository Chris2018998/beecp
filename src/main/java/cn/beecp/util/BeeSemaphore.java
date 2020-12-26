/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.beecp.util;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static java.lang.System.nanoTime;
import static java.lang.Thread.yield;
import static java.util.concurrent.locks.LockSupport.parkNanos;

/**
 * Semaphore Implementation mock class:{@link java.util.concurrent.Semaphore}
 *
 * @author Chris.Liao
 */
public class BeeSemaphore {
    //normal
    private static final int STS_NORMAL = 0;
    //waiting
    private static final int STS_WAITING = 1;
    //Acquired
    private static final int STS_ACQUIRED = 2;
    //allow to try acquire
    private static final int STS_TRY_ACQUIRE = 3;
    //timeout or interrupted
    private static final int STS_FAILED = 4;
    //park min nanoSecond,spin min time value
    private static final long parkForTimeoutThreshold = 1000L;
    //The number of times to spin before blocking in timed waits.
    private static final int maxTimedSpins = (Runtime.getRuntime().availableProcessors() < 2) ? 0 : 32;
    //Thread Interrupted Exception
    private static final InterruptedException RequestInterruptException = new InterruptedException();
    //state updater
    private static final AtomicIntegerFieldUpdater<Waiter> updater = AtomicIntegerFieldUpdater
            .newUpdater(Waiter.class, "state");

    /**
     * Synchronization implementation for semaphore.
     */
    private Sync sync;

    public BeeSemaphore(int size, boolean fair) {
        sync = fair ? new FairSync(size) : new NonfairSync(size);
    }

    /**
     * Acquires a permit from this semaphore, if one becomes available
     * within the given waiting time and the current thread has not
     * been {@linkplain Thread#interrupt interrupted}.
     *
     * @param timeout the maximum time to wait for a permit
     * @param unit    the time unit of the {@code timeout} argument
     * @return {@code true} if a permit was acquired and {@code false}
     * if the waiting time elapsed before a permit was acquired
     * @throws InterruptedException if the current thread is interrupted
     */
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquire(timeout, unit);
    }

    /**
     * Releases a permit, returning it to the semaphore.
     */
    public void release() {
        sync.release();
    }

    /**
     * Returns the current number of permits available in this semaphore.
     *
     * @return the number of permits available in this semaphore
     */

    public int availablePermits() {
        return sync.availablePermits();
    }

    /**
     * Queries whether any threads are waiting to acquire.
     *
     * @return {@code true} if there may be other threads waiting to acquire the lock
     */
    public boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * Returns an estimate of the number of threads waiting to acquire.
     *
     * @return the estimated number of threads waiting for this lock
     */
    public int getQueueLength() {
        return sync.getQueueLength();
    }

    //base Sync
    private static abstract class Sync {
        protected int size;
        protected AtomicInteger usingSize = new AtomicInteger(0);
        protected ConcurrentLinkedQueue<Waiter> waiterQueue = new ConcurrentLinkedQueue<Waiter>();

        public Sync(int size) {
            this.size = size;
        }

        /**
         * Transfer a permit to a waiter
         *
         * @param waiter
         * @param stsCode
         * @return true success,false failed
         */
        protected static final boolean transferToWaiter(Waiter waiter, int stsCode) {
            for (int state = waiter.state; (state == STS_NORMAL || state == STS_WAITING); state = waiter.state) {
                if (updater.compareAndSet(waiter, state, stsCode)) {
                    if (state == STS_WAITING) LockSupport.unpark(waiter.thread);
                    return true;
                }
            }
            return false;
        }

        public boolean hasQueuedThreads() {
            return !waiterQueue.isEmpty();
        }

        public int getQueueLength() {
            return waiterQueue.size();
        }

        public int availablePermits() {
            int availableSize = size - usingSize.get();
            return (availableSize > 0) ? availableSize : 0;
        }

        private final boolean acquirePermit() {
            while (true) {
                int expect = usingSize.get();
                int update = expect + 1;
                if (update > size) return false;
                if (usingSize.compareAndSet(expect, update)) return true;
            }
        }

        /**
         * @param timeout
         * @param unit
         * @return
         * @throws InterruptedException
         */
        public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
            if (acquirePermit()) return true;

            boolean isFailed = false;
            boolean isInterrupted = false;
            Waiter waiter = new Waiter();
            Thread thread = waiter.thread;
            waiterQueue.offer(waiter);
            int spinSize = (waiterQueue.peek() == waiter) ? maxTimedSpins : 0;
            final long deadline = nanoTime() + unit.toNanos(timeout);

            while (true) {
                int state = waiter.state;
                if (state == STS_ACQUIRED) {
                    return true;
                } else if (state == STS_TRY_ACQUIRE) {
                    if (acquirePermit()) {
                        waiterQueue.remove(waiter);
                        return true;
                    } else {
                        state = STS_NORMAL;
                        waiter.state = state;
                        yield();
                    }
                }

                if (isFailed) {
                    if (waiter.state == state && updater.compareAndSet(waiter, state, STS_FAILED)) {
                        waiterQueue.remove(waiter);
                        if (isInterrupted)
                            throw RequestInterruptException;
                        else
                            return false;
                    }
                } else {
                    timeout = deadline - nanoTime();
                    if (timeout > 0L) {
                        if (spinSize > 0) {
                            --spinSize;
                        } else if (timeout - parkForTimeoutThreshold > parkForTimeoutThreshold && waiter.state == state && updater.compareAndSet(waiter, state, STS_WAITING)) {
                            parkNanos(waiter, timeout);
                            if (thread.isInterrupted()) {
                                isFailed = true;
                                isInterrupted = true;
                            }
                            if (waiter.state == STS_WAITING)//reset to normal
                                updater.compareAndSet(waiter, STS_WAITING, STS_NORMAL);
                        }
                    } else {//timeout
                        isFailed = true;
                    }
                }
            }
        }

        abstract void release();
    }

    private static final class FairSync extends Sync {
        public FairSync(int size) {
            super(size);
        }

        public final void release() { //transfer permit
            Waiter waiter;
            while ((waiter = waiterQueue.poll()) != null)
                if (transferToWaiter(waiter, STS_ACQUIRED)) return;
            usingSize.decrementAndGet();//release permit
        }
    }

    private static final class NonfairSync extends Sync {
        public NonfairSync(int size) {
            super(size);
        }

        public final void release() {//transfer permit
            usingSize.decrementAndGet();
            for (Waiter waiter : waiterQueue)
                if (transferToWaiter(waiter, STS_TRY_ACQUIRE)) return;
        }
    }

    /**
     * permit waiter
     */
    private static final class Waiter {
        volatile int state;
        Thread thread = Thread.currentThread();
    }
}