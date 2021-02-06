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
 * Transfer Packet fly Test
 *
 * @author Chris.Liao
 */
public class PacketFlyTest {
    public static void main(String[] args) throws Exception {
        int producerSize = Runtime.getRuntime().availableProcessors();
        int consumerSize = producerSize;
        System.out.println(".................PacketFlyTest......................");
        ArrayList<Long> timeList = new ArrayList<Long>(5);
        timeList.add(testQueue("BeeTransferQueue", new BeeTransferQueue<TransferPacket>(), producerSize,
                consumerSize));
        timeList.add(testQueue("ArrayBlockingQueue", new ArrayBlockingQueue<TransferPacket>(producerSize), producerSize,
                consumerSize));
        timeList.add(testQueue("LinkedBlockingQueue", new LinkedBlockingQueue<TransferPacket>(), producerSize,
                consumerSize));
        timeList.add(testQueue("LinkedTransferQueue", new LinkedTransferQueue<TransferPacket>(), producerSize,
                consumerSize));
        timeList.add(testQueue("SynchronousQueue", new SynchronousQueue<TransferPacket>(), producerSize,
                consumerSize));

        Collections.sort(timeList);
        System.out.println(timeList);
    }

    private static long testQueue(String queueName, Queue<TransferPacket> queue, int producerSize, int consumerSize) throws Exception {
        BlockingQueueConsumer[] blkConsumers = null;
        BeeTransferQueueConsumer[] fstConsumers = null;
        CountDownLatch producersDownLatch = new CountDownLatch(producerSize);

        CountDownLatch pollStartCountLatch = new CountDownLatch(consumerSize);
        CountDownLatch pollEndCountLatch = new CountDownLatch(consumerSize);
        AtomicBoolean existConsumerInd = new AtomicBoolean(true);

        if (queue instanceof BlockingQueue) {//Blocking Queue Consumers
            BlockingQueue<TransferPacket> blockingQueue = (BlockingQueue<TransferPacket>) queue;
            blkConsumers = new BlockingQueueConsumer[consumerSize];
            for (int i = 0; i < consumerSize; i++) {
                blkConsumers[i] = new BlockingQueueConsumer(blockingQueue, pollStartCountLatch, pollEndCountLatch);
                blkConsumers[i].start();
            }
        } else {//Fast Transfer Queue Consumers
            BeeTransferQueue<TransferPacket> BeeTransferQueue = (BeeTransferQueue<TransferPacket>) queue;
            fstConsumers = new BeeTransferQueueConsumer[consumerSize];
            for (int i = 0; i < consumerSize; i++) {
                fstConsumers[i] = new BeeTransferQueueConsumer(BeeTransferQueue, pollStartCountLatch, pollEndCountLatch);
                fstConsumers[i].start();
            }
        }

        pollStartCountLatch.await();
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(10));

        // Producers
        if (queue instanceof BlockingQueue) {//Blocking Queue Consumers
            BlockingQueue<TransferPacket> blockingQueue = (BlockingQueue<TransferPacket>) queue;
            for (int i = 0; i < producerSize; i++) {
                new BlockingQueueOfferProducer(blockingQueue, existConsumerInd, producersDownLatch).start();
            }
        } else {//Fast Transfer Queue Consumers
            BeeTransferQueue<TransferPacket> BeeTransferQueue = (BeeTransferQueue<TransferPacket>) queue;
            for (int i = 0; i < producerSize; i++) {
                new BeeTransferQueueOfferProducer(BeeTransferQueue, existConsumerInd, producersDownLatch).start();
            }
        }

        pollEndCountLatch.await();
        existConsumerInd.set(false);
        producersDownLatch.await();

        // Summary and Conclusion
        int totPacketSize = 0;
        BigDecimal totTime = new BigDecimal(0);
        if (queue instanceof BlockingQueue) {
            for (int i = 0; i < consumerSize; i++) {
                TransferPacket packet = blkConsumers[i].getTransferPacket();
                totPacketSize++;
                totTime = totTime.add(new BigDecimal(packet.arriveTime - packet.sendTime));
            }
        } else {
            for (int i = 0; i < consumerSize; i++) {
                TransferPacket packet = fstConsumers[i].getTransferPacket();
                totPacketSize++;
                totTime = totTime.add(new BigDecimal(packet.arriveTime - packet.sendTime));
            }
        }

        BigDecimal avgTime = totTime.divide(new BigDecimal(totPacketSize), 0, BigDecimal.ROUND_HALF_UP);
        System.out.println("<" + queueName + "> producer-size:" + producerSize + ",consumer-size:" + consumerSize
                + ",total packet size:" + totPacketSize + ",total time:" + totTime.longValue() + "(ns),avg time:"
                + avgTime + "(ns)");

        return avgTime.longValue();
    }

    //base
    static final class TransferPacket {
        public long sendTime = System.nanoTime();
        public long arriveTime;
    }

    static abstract class Consumer extends Thread {
        protected TransferPacket packet;
        protected CountDownLatch pollStartCountLatch;
        protected CountDownLatch pollEndCountLatch;

        public Consumer(CountDownLatch pollStartCountLatch, CountDownLatch pollEndCountLatch) {
            this.pollStartCountLatch = pollStartCountLatch;
            this.pollEndCountLatch = pollEndCountLatch;
        }

        public TransferPacket getTransferPacket() {
            return packet;
        }

        public void run() {
            try {
                pollStartCountLatch.countDown();
                packet = poll(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                packet.arriveTime = System.nanoTime();
            } catch (InterruptedException e) {
            }

            pollEndCountLatch.countDown();
        }

        abstract TransferPacket poll(long time, TimeUnit unit) throws InterruptedException;
    }

    static abstract class Producer extends Thread {
        protected AtomicBoolean activeInd;
        protected CountDownLatch producersDownLatch;

        public Producer(AtomicBoolean activeInd, CountDownLatch producersDownLatch) {
            this.activeInd = activeInd;
            this.producersDownLatch = producersDownLatch;
        }

        public void run() {
            while (activeInd.get()) {
                offer(new TransferPacket());
            }
            producersDownLatch.countDown();
        }

        abstract void offer(TransferPacket packet);
    }

    //BeeTransferQueue
    static final class BeeTransferQueueConsumer extends Consumer {
        private BeeTransferQueue<TransferPacket> queue;

        public BeeTransferQueueConsumer(BeeTransferQueue<TransferPacket> queue, CountDownLatch pollStartLatch, CountDownLatch pollEndDownLatch) {
            super(pollStartLatch, pollEndDownLatch);
            this.queue = queue;
        }

        public TransferPacket poll(long time, TimeUnit unit) throws InterruptedException {
            return queue.poll(time, unit);
        }
    }

    static final class BeeTransferQueueOfferProducer extends Producer {
        private BeeTransferQueue<TransferPacket> queue;

        public BeeTransferQueueOfferProducer(BeeTransferQueue<TransferPacket> queue, AtomicBoolean activeInd, CountDownLatch producersDownLatch) {
            super(activeInd, producersDownLatch);
            this.queue = queue;
        }

        public void offer(TransferPacket packet) {
            queue.offer(packet);
        }
    }

    // BlockingQueue
    static final class BlockingQueueConsumer extends Consumer {
        private BlockingQueue<TransferPacket> queue;

        public BlockingQueueConsumer(BlockingQueue<TransferPacket> queue, CountDownLatch pollStartLatch, CountDownLatch pollEndDownLatch) {
            super(pollStartLatch, pollEndDownLatch);
            this.queue = queue;
        }

        public TransferPacket poll(long time, TimeUnit unit) throws InterruptedException {
            return queue.poll(time, unit);
        }
    }

    static final class BlockingQueueOfferProducer extends Producer {
        private BlockingQueue<TransferPacket> queue;

        public BlockingQueueOfferProducer(BlockingQueue<TransferPacket> queue, AtomicBoolean activeInd, CountDownLatch producersDownLatch) {
            super(activeInd, producersDownLatch);
            this.queue = queue;
        }

        public void offer(TransferPacket packet) {
            queue.offer(packet);
        }
    }
}
