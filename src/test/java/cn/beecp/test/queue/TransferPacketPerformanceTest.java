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
import java.util.LinkedList;
import java.util.List;
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
 * Transfer Packet Performance Test
 *
 * @author Chris.Liao
 */
public class TransferPacketPerformanceTest {
    public static void main(String[] args) throws Exception {
        int producerSize = 10, consumerSize = 100, takeTimes = 100;
        System.out.println(".................TransferPacketPerformanceTest......................");
        testTransferQueue("ArrayBlockingQueue", new ArrayBlockingQueue<TransferPacket>(1000), producerSize, consumerSize, takeTimes);
        testTransferQueue("LinkedBlockingQueue", new LinkedBlockingQueue<TransferPacket>(), producerSize, consumerSize, takeTimes);
        testTransferQueue("LinkedTransferQueue", new LinkedTransferQueue<TransferPacket>(), producerSize, consumerSize, takeTimes);
        testTransferQueue("SynchronousQueue", new SynchronousQueue<TransferPacket>(), producerSize, consumerSize, takeTimes);
        testTransferQueue("FastTransferQueue", new FastTransferQueue<TransferPacket>(), producerSize, consumerSize, takeTimes);
    }

    private static void testTransferQueue(String queueName, Queue<TransferPacket> queue, int producerSize, int consumerSize, int pollSize) throws Exception {
        Consumer[] consumers = new Consumer[consumerSize];
        CountDownLatch producersDownLatch = new CountDownLatch(producerSize);
        CountDownLatch consumersDownLatch = new CountDownLatch(consumerSize);
        AtomicBoolean existConsumerInd = new AtomicBoolean(true);
        Method pollMethod = queue.getClass().getMethod("poll", Long.TYPE, TimeUnit.class);

        //Consumers
        for (int i = 0; i < consumerSize; i++) {
            consumers[i] = new Consumer(queue, pollSize, consumersDownLatch, pollMethod);
            consumers[i].start();
        }

        //Producers
        long startTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        for (int i = 0; i < producerSize; i++) {
            new Producer(queue, startTime, existConsumerInd, producersDownLatch).start();
        }

        consumersDownLatch.await();
        existConsumerInd.set(false);
        producersDownLatch.await();

        //Summary and Conclusion
        int totPacketSize = 0;
        BigDecimal totTime = new BigDecimal(0);
        for (int i = 0; i < consumerSize; i++) {
            for (TransferPacket packet : consumers[i].getPacketList()) {
                totPacketSize++;
                totTime = totTime.add(new BigDecimal(packet.arriveTime - packet.sendTime));
            }
        }
        if (totTime.longValue() > 0 && totPacketSize > 0) {
            BigDecimal avgTime = totTime.divide(new BigDecimal(totPacketSize), 0, BigDecimal.ROUND_HALF_UP);
            System.out.println("<" + queueName + "> producer-size:" + producerSize + ",consumer-size:"
                    + consumerSize + ",total packet size:" + totPacketSize + ",total time:" + totTime.longValue()
                    + "(ns),avg time:" + avgTime + "(ns)");
        }
    }

    private static final TransferPacket poll(Queue<TransferPacket> queue, Method pollMethod) {
        try {
            return (TransferPacket) pollMethod.invoke(queue, new Object[]{Integer.MAX_VALUE, TimeUnit.NANOSECONDS});
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    static final class TransferPacket {
        long sendTime = System.nanoTime();
        long arriveTime;
    }

    static final class Producer extends Thread {
        private long startTime;
        private AtomicBoolean activeInd;
        private Queue<TransferPacket> queue;
        private CountDownLatch producersDownLatch;

        public Producer(Queue<TransferPacket> queue, long startTime, AtomicBoolean activeInd, CountDownLatch producersDownLatch) {
            this.queue = queue;
            this.startTime = startTime;
            this.activeInd = activeInd;
            this.producersDownLatch = producersDownLatch;
        }

        public void run() {
            LockSupport.parkNanos(startTime - System.nanoTime());
            while (activeInd.get()) {
                queue.offer(new TransferPacket());
            }
            producersDownLatch.countDown();
        }
    }

    static final class Consumer extends Thread {
        private int loopTimes;
        private CountDownLatch latch;
        private Method pollMethod;
        private Queue<TransferPacket> queue;
        private List<TransferPacket> packetList = new LinkedList();

        public Consumer(Queue<TransferPacket> queue, int pollTimes, CountDownLatch latch, Method pollMethod) {
            this.queue = queue;
            this.loopTimes = pollTimes;
            this.latch = latch;
            this.pollMethod = pollMethod;
        }

        public List<TransferPacket> getPacketList() {
            return packetList;
        }

        public void run() {
            for (int i = 0; i < loopTimes; i++) {
                TransferPacket packet = poll(queue, pollMethod);
                if (packet != null) {
                    packet.arriveTime = System.nanoTime();
                    packetList.add(packet);
                }
            }
            latch.countDown();
        }
    }
}

