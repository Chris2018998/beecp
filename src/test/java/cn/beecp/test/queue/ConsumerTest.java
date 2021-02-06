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

package cn.beecp.test.queue;

import cn.beecp.util.BeeTransferQueue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * Consumer Test
 *
 * @author Chris.Liao
 */
public class ConsumerTest {
    private static final Object transferObject = new Object();

    public static void main(String[] args) throws Exception {
        int producerSize = Runtime.getRuntime().availableProcessors();
        int consumerSize = producerSize, operateSize = 1000;
        System.out.println(".................ConsumerTest......................");
        ArrayList<Long> timeList = new ArrayList<Long>(5);

        timeList.add(testQueue("ArrayBlockingQueue", new ArrayBlockingQueue<Object>(1000), producerSize, consumerSize, operateSize));
        timeList.add(testQueue("LinkedBlockingQueue", new LinkedBlockingQueue<Object>(), producerSize, consumerSize, operateSize));
        timeList.add(testQueue("LinkedTransferQueue", new LinkedTransferQueue<Object>(), producerSize, consumerSize, operateSize));
        timeList.add(testQueue("SynchronousQueue", new SynchronousQueue<Object>(), producerSize, consumerSize, operateSize));
        timeList.add(testQueue("BeeTransferQueue", new BeeTransferQueue<Object>(), producerSize, consumerSize, operateSize));
        Collections.sort(timeList);
        System.out.println(timeList);
    }

    private static long testQueue(String queueName, Queue<Object> queue, int producerSize, int consumerSize, int operateSize) throws Exception {
        BlqConsumer[] blkConsumers = null;
        FstQConsumer[] fstConsumers = null;
        CountDownLatch producersDownLatch = new CountDownLatch(producerSize);
        CountDownLatch consumersDownLatch = new CountDownLatch(consumerSize);
        AtomicBoolean existConsumerInd = new AtomicBoolean(true);
        long startTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);

        if (queue instanceof BlockingQueue) {//Blocking Queue Consumers
            BlockingQueue<Object> blockingQueue = (BlockingQueue<Object>) queue;
            blkConsumers = new BlqConsumer[consumerSize];
            for (int i = 0; i < consumerSize; i++) {
                blkConsumers[i] = new BlqConsumer(startTime, blockingQueue, consumersDownLatch, operateSize);
                blkConsumers[i].start();
            }
        } else {//Fast Transfer Queue Consumers
            BeeTransferQueue<Object> BeeTransferQueue = (BeeTransferQueue<Object>) queue;
            fstConsumers = new FstQConsumer[consumerSize];
            for (int i = 0; i < consumerSize; i++) {
                fstConsumers[i] = new FstQConsumer(startTime, BeeTransferQueue, consumersDownLatch, operateSize);
                fstConsumers[i].start();
            }
        }

        // Producers
        for (int i = 0; i < producerSize; i++) {
            new Producer(startTime, queue, existConsumerInd, producersDownLatch).start();
        }

        consumersDownLatch.await();
        existConsumerInd.set(false);
        producersDownLatch.await();

        // Summary and Conclusion
        long totalExeSize = consumerSize * operateSize;
        BigDecimal totTime = new BigDecimal(0);
        if (queue instanceof BlockingQueue) {
            for (int i = 0; i < consumerSize; i++) {
                totTime = totTime.add(new BigDecimal(blkConsumers[i].getEndTime() - blkConsumers[i].getStartTime()));
            }
        } else {
            for (int i = 0; i < consumerSize; i++) {
                totTime = totTime.add(new BigDecimal(fstConsumers[i].getEndTime() - fstConsumers[i].getStartTime()));
            }
        }

        BigDecimal avgTime = totTime.divide(new BigDecimal(totalExeSize), 0, BigDecimal.ROUND_HALF_UP);
        System.out.println("<" + queueName + "> producer-size:" + producerSize + ",consumer-size:"
                + consumerSize + ",poll total size:" + totalExeSize + ",total time:" + totTime.longValue()
                + "(ns),avg time:" + avgTime + "(ns)");

        return avgTime.longValue();
    }

    static final class Producer extends Thread {
        private long startTime;
        private AtomicBoolean activeInd;
        private Queue<Object> queue;
        private CountDownLatch producersDownLatch;

        public Producer(long startTime, Queue<Object> queue, AtomicBoolean activeInd, CountDownLatch producersDownLatch) {
            this.startTime = startTime;
            this.queue = queue;
            this.activeInd = activeInd;
            this.producersDownLatch = producersDownLatch;
        }

        public void run() {
            LockSupport.parkNanos(startTime - System.nanoTime());
            while (activeInd.get()) {
                queue.offer(transferObject);
            }
            producersDownLatch.countDown();
        }
    }

    static final class BlqConsumer extends Thread {
        private long startTime;
        private long endTime;
        private int operateSize;
        private CountDownLatch latch;
        private BlockingQueue<Object> queue;

        public BlqConsumer(long startTime, BlockingQueue<Object> queue, CountDownLatch latch, int operateSize) {
            this.queue = queue;
            this.latch = latch;
            this.startTime = startTime;
            this.operateSize = operateSize;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void run() {
            LockSupport.parkNanos(startTime - System.nanoTime());
            startTime = System.nanoTime();
            for (int i = 0; i < operateSize; i++) {
                try {
                    queue.poll(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                }
            }
            endTime = System.nanoTime();
            latch.countDown();
        }
    }

    static final class FstQConsumer extends Thread {
        private long startTime;
        private long endTime;
        private int operateSize;
        private CountDownLatch latch;
        private BeeTransferQueue<Object> queue;

        public FstQConsumer(long startTime, BeeTransferQueue<Object> queue, CountDownLatch latch, int operateSize) {
            this.queue = queue;
            this.latch = latch;
            this.startTime = startTime;
            this.operateSize = operateSize;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void run() {
            LockSupport.parkNanos(startTime - System.nanoTime());
            startTime = System.nanoTime();
            for (int i = 0; i < operateSize; i++) {
                try {
                    queue.poll(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                }
            }
            endTime = System.nanoTime();
            latch.countDown();
        }
    }
}





