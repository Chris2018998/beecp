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

import cn.beecp.util.FastTransferQueue;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * QueueConcurrentTest
 *
 * @author Chris.Liao
 */
public class QueueConcurrentTest {
    private static final Object transferObject = new Object();
    public static void main(String[] args) throws Exception {
        int producerSize = 10, consumerSize = 1000, operateSize = 100;
        System.out.println(".................QueueConcurrentTest......................");
        ArrayList<Long> timeList = new ArrayList<Long>(5);

        timeList.add(testTransferQueue("ArrayBlockingQueue", new ArrayBlockingQueue<Object>(1000), producerSize, consumerSize, operateSize));
        timeList.add(testTransferQueue("LinkedBlockingQueue", new LinkedBlockingQueue<Object>(), producerSize, consumerSize, operateSize));
        timeList.add(testTransferQueue("LinkedTransferQueue", new LinkedTransferQueue<Object>(), producerSize, consumerSize, operateSize));
        timeList.add(testTransferQueue("SynchronousQueue", new SynchronousQueue<Object>(), producerSize, consumerSize, operateSize));
        timeList.add(testTransferQueue("FastTransferQueue", new FastTransferQueue<Object>(), producerSize, consumerSize, operateSize));
        Collections.sort(timeList);
        System.out.println(timeList);
    }

    private static long testTransferQueue(String queueName, Queue<Object> queue, int producerSize, int consumerSize, int operateSize) throws Exception {
        Consumer[] consumers = new Consumer[consumerSize];
        CountDownLatch producersDownLatch = new CountDownLatch(producerSize);
        CountDownLatch consumersDownLatch = new CountDownLatch(consumerSize);
        AtomicBoolean existConsumerInd = new AtomicBoolean(true);
        Method pollMethod = queue.getClass().getMethod("poll", Long.TYPE, TimeUnit.class);
        long  startTime=System.nanoTime()+ TimeUnit.SECONDS.toNanos(10);

        //Consumers
        for (int i = 0; i < consumerSize; i++) {
            consumers[i] = new Consumer(queue,startTime,operateSize,consumersDownLatch,pollMethod);
            consumers[i].start();
        }

        //Producers
        for (int i = 0; i < producerSize; i++) {
            new Producer(queue,startTime,existConsumerInd,producersDownLatch).start();
        }

        consumersDownLatch.await();
        existConsumerInd.set(false);
        producersDownLatch.await();

        //Summary and Conclusion
        int totalExeSize = consumerSize * operateSize;
        BigDecimal totTime = new BigDecimal(0);
        for (int i = 0; i < consumerSize; i++) {
            totTime = totTime.add(new BigDecimal(consumers[i].getTookTime()));
        }

        BigDecimal avgTime = totTime.divide(new BigDecimal(totalExeSize), 0, BigDecimal.ROUND_HALF_UP);
        System.out.println("<" + queueName + "> producer-size:" + producerSize + ",consumer-size:"
                + consumerSize + ",poll total size:" + totalExeSize + ",total time:" + totTime.longValue()
                + "(ns),avg time:" + avgTime + "(ns)");

        return avgTime.longValue();
    }

    private static final Object poll(Queue<Object> queue, Method pollMethod) {
        try {
            return pollMethod.invoke(queue, Integer.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    static final class Producer extends Thread {
        private long startTime;
        private Queue<Object> queue;
        private AtomicBoolean activeInd;
        private CountDownLatch producersDownLatch;

        public Producer(Queue<Object> queue, long startTime,AtomicBoolean activeInd, CountDownLatch producersDownLatch) {
            this.queue = queue;
            this.startTime=startTime;
            this.activeInd = activeInd;
            this.producersDownLatch = producersDownLatch;
        }

        public void run() {
            LockSupport.parkNanos(startTime-System.nanoTime());
            while (activeInd.get()) {
                queue.offer(transferObject);
            }
            producersDownLatch.countDown();
        }
    }

    static final class Consumer extends Thread {
        private long startTime;
        private int operateSize;
        private CountDownLatch latch;
        private Method pollMethod;
        private Queue<Object> queue;
        private long tookTime;

        public Consumer(Queue<Object> queue, long startTime,int operateSize,CountDownLatch latch, Method pollMethod) {
            this.queue = queue;
            this.startTime=startTime;
            this.operateSize = operateSize;
            this.latch = latch;
            this.pollMethod = pollMethod;
        }

        public long getTookTime() {
            return tookTime;
        }
        public void run() {
            LockSupport.parkNanos(startTime-System.nanoTime());
            long time1 = System.nanoTime();
            for (int i = 0; i < operateSize; i++) {
                poll(queue, pollMethod);
            }
            tookTime = System.nanoTime() - time1;
            latch.countDown();
        }
    }
}





