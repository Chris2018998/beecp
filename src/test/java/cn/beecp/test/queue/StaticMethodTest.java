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

    public static void main(String[] args) throws Exception {
        int threadSize = 1000, operateSize = 1000;
        test("SingleInstance", threadSize, operateSize, 1);
        test("StaticMethod", threadSize, operateSize, 2);
        test("ObjectMethod", threadSize, operateSize, 3);
    }

    private static void test(String name, int threadSize, int operateSize, int testType) throws Exception {
        BigDecimal totTime = new BigDecimal(0);
        if (testType == 1) {//SingleInstance
            StaticMethodTest test = new StaticMethodTest();
            SingleInstanceThread[] testThreads = new SingleInstanceThread[threadSize];
            CountDownLatch threadsDownLatch = new CountDownLatch(threadSize);
            for (int i = 0; i < threadSize; i++) {
                testThreads[i] = new SingleInstanceThread(test, operateSize, threadsDownLatch);
                testThreads[i].start();
            }
            threadsDownLatch.await();

            //Summary and Conclusion
            for (int i = 0; i < threadSize; i++) {
                totTime = totTime.add(new BigDecimal(testThreads[i].getTookTime()));
            }
        } else if (testType == 2) {//StaticMethod
            StaticMethodThread[] testThreads = new StaticMethodThread[threadSize];
            CountDownLatch threadsDownLatch = new CountDownLatch(threadSize);
            for (int i = 0; i < threadSize; i++) {
                testThreads[i] = new StaticMethodThread(operateSize, threadsDownLatch);
                testThreads[i].start();
            }
            threadsDownLatch.await();

            //Summary and Conclusion
            for (int i = 0; i < threadSize; i++) {
                totTime = totTime.add(new BigDecimal(testThreads[i].getTookTime()));
            }
        } else if (testType == 3) {//ObjectMethod
            ObjectMethodThread[] testThreads = new ObjectMethodThread[threadSize];
            CountDownLatch threadsDownLatch = new CountDownLatch(threadSize);
            for (int i = 0; i < threadSize; i++) {
                testThreads[i] = new ObjectMethodThread(operateSize, threadsDownLatch);
                testThreads[i].start();
            }
            threadsDownLatch.await();

            //Summary and Conclusion
            for (int i = 0; i < threadSize; i++) {
                totTime = totTime.add(new BigDecimal(testThreads[i].getTookTime()));
            }
        }

        int totalExeSize = threadSize * operateSize;
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

        public SingleInstanceThread(StaticMethodTest test, int operateTimes, CountDownLatch threadsDownLatch) {
            this.test = test;
            this.operateTimes = operateTimes;
            this.threadsDownLatch = threadsDownLatch;
        }

        public long getTookTime() {
            return tookTime;
        }

        public void run() {
            long time1 = System.nanoTime();
            for (int i = 0; i < operateTimes; i++) {
                test.work();
            }
            tookTime = System.nanoTime() - time1;
            threadsDownLatch.countDown();
        }
    }

    static final class StaticMethodThread extends Thread {
        private long tookTime;
        private int operateTimes;
        private CountDownLatch threadsDownLatch;

        public StaticMethodThread(int operateTimes, CountDownLatch threadsDownLatch) {
            this.operateTimes = operateTimes;
            this.threadsDownLatch = threadsDownLatch;
        }

        public long getTookTime() {
            return tookTime;
        }

        public void run() {
            long time1 = System.nanoTime();
            for (int i = 0; i < operateTimes; i++) {
                StaticMethodTest.work();
            }
            tookTime = System.nanoTime() - time1;
            threadsDownLatch.countDown();
        }
    }

    static final class ObjectMethodThread extends Thread {
        private long tookTime;
        private int operateTimes;
        private CountDownLatch threadsDownLatch;

        public ObjectMethodThread(int operateTimes, CountDownLatch threadsDownLatch) {
            this.operateTimes = operateTimes;
            this.threadsDownLatch = threadsDownLatch;
        }

        public long getTookTime() {
            return tookTime;
        }

        public void run() {
            long time1 = System.nanoTime();
            for (int i = 0; i < operateTimes; i++) {
                new StaticMethodTest().work();
            }
            tookTime = System.nanoTime() - time1;
            threadsDownLatch.countDown();
        }
    }
}
