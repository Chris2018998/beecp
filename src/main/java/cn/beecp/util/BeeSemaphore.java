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

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static java.lang.System.nanoTime;
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

    //state updater
    private static final AtomicIntegerFieldUpdater<Waiter> updater = AtomicIntegerFieldUpdater
            .newUpdater(Waiter.class, "state");
    //Thread Interrupted Exception
    private static final InterruptedException RequestInterruptException = new InterruptedException();

    private Sync sync;

    public BeeSemaphore(int size, boolean fair) {
        sync = fair ? new FairSync(size) : new NonfairSync(size);
    }

    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquire(timeout, unit);
    }

    public void release() {
        sync.release();
    }

    public int availablePermits() {
        return sync.availablePermits();
    }

    public boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    public int getQueueLength() {
        return sync.getQueueLength();
    }


    private static abstract class Sync {
        protected int size;
        protected AtomicInteger usingSize = new AtomicInteger(0);
        protected ConcurrentLinkedQueue<Waiter> waiterQueue = new ConcurrentLinkedQueue<Waiter>();

        public Sync(int size) {
            this.size = size;
        }

        public int availablePermits() {
            int availableSize = size - usingSize.get();
            return (availableSize > 0) ? availableSize : 0;
        }

        public boolean hasQueuedThreads() {
            return !waiterQueue.isEmpty();
        }

        public int getQueueLength() {
            return waiterQueue.size();
        }


        private boolean acquirePermit() {
            if (usingSize.get() < size) {
                if (usingSize.incrementAndGet() <= size) {
                    return true;
                } else {
                    usingSize.decrementAndGet();
                }
            }
            return false;
        }

        protected boolean transferToWaiter(Waiter waiter, int stsCode) {
            for (int state = waiter.state; (state == STS_NORMAL || state == STS_WAITING); state = waiter.state) {
                if (updater.compareAndSet(waiter, state, stsCode)) {
                    if (state == STS_WAITING) LockSupport.unpark(waiter.thread);
                    return true;
                }
            }
            return false;
        }

        public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
            if (acquirePermit()) return true;
            if (timeout <= 0) return false;

            boolean isFailed = false;
            boolean isInterrupted = false;
            Waiter waiter = new Waiter();
            Thread thread = waiter.thread;
            waiterQueue.offer(waiter);
            final long deadline = nanoTime() + unit.toNanos(timeout);
            while (true) {
                int state = waiter.state;
                if (state == STS_ACQUIRED) {
                    return true;
                } else if (state == STS_TRY_ACQUIRE) {
                    if (acquirePermit()) {
                        waiterQueue.remove(waiter);
                        return true;
                    }
                }

                if (isFailed) {
                    if (updater.compareAndSet(waiter, state, STS_FAILED)) {
                        waiterQueue.remove(waiter);
                        if (isInterrupted)
                            throw RequestInterruptException;
                        else
                            return false;
                    }
                } else {
                    if ((timeout = deadline - nanoTime()) > 0L) {
                        if (timeout > parkForTimeoutThreshold && updater.compareAndSet(waiter, state, STS_WAITING)) {
                            parkNanos(this, timeout);
                            if (thread.isInterrupted()) {
                                isFailed = true;
                                isInterrupted = true;
                            }
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

        public void release() {
            //transfer permit
            Waiter waiter;
            while ((waiter = waiterQueue.poll()) != null) {
                if (transferToWaiter(waiter, STS_ACQUIRED)) return;
            }
            usingSize.decrementAndGet();//release permit
        }
    }

    private static final class NonfairSync extends Sync {
        public NonfairSync(int size) {
            super(size);
        }

        public void release() {
            usingSize.decrementAndGet();//release permit
            Iterator<Waiter> itor = waiterQueue.iterator();
            while (itor.hasNext()) {
                Waiter waiter = itor.next();
                if (transferToWaiter(waiter, STS_TRY_ACQUIRE)) return;
            }
        }
    }

    private static final class Waiter {
        volatile int state;
        Thread thread = Thread.currentThread();
    }
}