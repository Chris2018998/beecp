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

import java.math.BigDecimal;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.SynchronousQueue;

/**
 * Queue Write ReadTest
 *
 * @author Chris.Liao
 */
public class QueueWriteReadTest {
    public static void main(String[] args) throws Exception {
        int threadSize = 1000, takeTimes = 10000;
        System.out.println(".................Queue Write/Read......................");
        testWriteAndRead("ArrayBlockingQueue", new ArrayBlockingQueue<Object>(1000), threadSize, takeTimes);
        testWriteAndRead("LinkedBlockingQueue", new LinkedBlockingQueue<Object>(), threadSize,takeTimes);
        testWriteAndRead("LinkedTransferQueue", new LinkedTransferQueue<Object>(), threadSize,takeTimes);
        //testWriteAndRead("SynchronousQueue", new SynchronousQueue<Object>(), threadSize,takeTimes);
        testWriteAndRead("FastTransferQueue", new FastTransferQueue<Object>(), threadSize,takeTimes);
        testWriteAndRead("ConcurrentLinkedQueue", new ConcurrentLinkedQueue<Object>(), threadSize,takeTimes);
    }

    private static void testWriteAndRead(String queueName, Queue<Object> queue, int testThreadSize, int operateSize) throws Exception {
        WriteAndReadThread[] threads = new WriteAndReadThread[testThreadSize];
        CountDownLatch threadsDownLatch = new CountDownLatch(testThreadSize);

        //Consumers
        for (int i = 0; i < testThreadSize; i++) {
            threads[i] = new WriteAndReadThread(queue,operateSize,threadsDownLatch);
            threads[i].start();
        }
        threadsDownLatch.await();

        //Summary and Conclusion
        int totalExeSize = testThreadSize * operateSize;
        BigDecimal writeTotTime = new BigDecimal(0);
        BigDecimal readTotTime = new BigDecimal(0);

        for (int i = 0; i < testThreadSize; i++) {
            writeTotTime = writeTotTime.add(new BigDecimal(threads[i].getWriteTime()));
            readTotTime = readTotTime.add(new BigDecimal(threads[i].getReadTime()));
        }

        BigDecimal writeAvgTime = writeTotTime.divide(new BigDecimal(totalExeSize), 0, BigDecimal.ROUND_HALF_UP);
        BigDecimal readAvgTime = readTotTime.divide(new BigDecimal(totalExeSize), 0, BigDecimal.ROUND_HALF_UP);

        System.out.println("<" + queueName + "> thread-size:" + testThreadSize + ",operate-size:"
                + operateSize + ",write avg time:" + writeAvgTime + "(ns),read avg time:" + readAvgTime.longValue()+"(ns)");
    }

    static final class WriteAndReadThread extends Thread {
        private long writeTime;
        private long readTime;
        private int operateTimes;
        private CountDownLatch latch;
        private Queue<Object> queue;

        public WriteAndReadThread(Queue<Object> queue,int operateTimes,CountDownLatch latch) {
            this.queue = queue;
            this.latch = latch;
            this.operateTimes = operateTimes;
        }
        public long getWriteTime() {
            return writeTime;
        }
        public long getReadTime() {
            return readTime;
        }
        public void run() {
            long time1 = System.nanoTime();
            for (int i = 0; i < operateTimes; i++) {
                queue.offer(i);
            }
            long time2 = System.nanoTime();
            for (int i = 0; i < operateTimes; i++) {
                queue.poll();
            }
            long time3 = System.nanoTime();
            writeTime=time2-time1;
            readTime=time3-time2;
            latch.countDown();
        }
    }
}