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

import java.math.BigDecimal;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Static Method performance
 *
 * @author Chris.Liao
 */
public class StaticMethodTest {
    private static long time = TimeUnit.MILLISECONDS.toNanos(10);
    public static void main(String[]args)throws Exception{
        int threadSize=1000,operateSize=1000;
        testSingleInstance("SingleInstance",threadSize,operateSize);
        testStaticMethod("StaticMethod",threadSize,operateSize);
    }

    private static void testSingleInstance(String name,int threadSize,int operateSize) throws Exception {
        StaticMethodTest test=new StaticMethodTest();
        SingleInstanceThread[] threads = new SingleInstanceThread[threadSize];
        CountDownLatch threadsDownLatch = new CountDownLatch(threadSize);
        for (int i = 0; i < threadSize; i++) {
            threads[i] = new SingleInstanceThread(test,operateSize,threadsDownLatch);
            threads[i].start();
        }
        threadsDownLatch.await();

        //Summary and Conclusion
        int totalExeSize = threadSize * operateSize;
        BigDecimal totTime = new BigDecimal(0);

        for (int i = 0; i < threadSize; i++) {
            totTime = totTime.add(new BigDecimal(threads[i].getTookTime()));
        }

        BigDecimal avgTime = totTime.divide(new BigDecimal(totalExeSize), 0, BigDecimal.ROUND_HALF_UP);
        System.out.println("<" + name + "> thread-size:" + threadSize + ",operate-size:"
                + operateSize + ",avg time:" + avgTime + "(ns)");
    }

    private static void testStaticMethod(String name,int threadSize,int operateSize) throws Exception {
        StaticMethodTest test=new StaticMethodTest();
        StaticMethodThread[] threads = new StaticMethodThread[threadSize];
        CountDownLatch threadsDownLatch = new CountDownLatch(threadSize);
        for (int i = 0; i < threadSize; i++) {
            threads[i] = new StaticMethodThread(operateSize,threadsDownLatch);
            threads[i].start();
        }
        threadsDownLatch.await();

        //Summary and Conclusion
        int totalExeSize = threadSize * operateSize;
        BigDecimal totTime = new BigDecimal(0);

        for (int i = 0; i < threadSize; i++) {
            totTime = totTime.add(new BigDecimal(threads[i].getTookTime()));
        }

        BigDecimal avgTime = totTime.divide(new BigDecimal(totalExeSize), 0, BigDecimal.ROUND_HALF_UP);
        System.out.println("<" + name + "> thread-size:" + threadSize + ",operate-size:"
                + operateSize + ",write avg time:" + avgTime + "(ns)");
    }

    public static void work() {
        LockSupport.parkNanos(time);
    }

    static final class SingleInstanceThread extends Thread {
        private long tookTime;
        private int operateTimes;
        private StaticMethodTest test;
        private CountDownLatch threadsDownLatch;

        public SingleInstanceThread(StaticMethodTest test, int operateTimes,CountDownLatch threadsDownLatch) {
            this.test = test;
            this.operateTimes = operateTimes;
            this.threadsDownLatch=threadsDownLatch;
        }

        public long getTookTime() {
            return tookTime;
        }

        public void run() {
            long time1 = System.nanoTime();
            for (int i = 0; i < operateTimes; i++) {
                work();
            }
            long time2 = System.nanoTime();
            tookTime = time2 - time1;
            threadsDownLatch.countDown();
        }
    }

    static final class StaticMethodThread extends Thread {
        private long tookTime;
        private int operateTimes;
        private CountDownLatch threadsDownLatch;

        public StaticMethodThread(int operateTimes, CountDownLatch threadsDownLatch) {
            this.operateTimes = operateTimes;
            this.threadsDownLatch=threadsDownLatch;
        }

        public long getTookTime() {
            return tookTime;
        }

        public void run() {
            long time1 = System.nanoTime();
            for (int i = 0; i < operateTimes; i++) {
                StaticMethodTest.work();
            }
            long time2 = System.nanoTime();
            tookTime = time2 - time1;
            threadsDownLatch.countDown();
        }
    }
}
