package cn.beecp.test.other;

import org.jmin.util.queue.ConcurrentLinkedQueue2;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConcurrentLinkedQueueTest {
    private static int size = 1000;

    public static void main(String[] args) {
        ConcurrentLinkedQueue<Integer> queue1 = new ConcurrentLinkedQueue<Integer>();
        ConcurrentLinkedQueue2<Integer> queue2 = new ConcurrentLinkedQueue2<Integer>();
        testQueue(queue1, "ConcurrentLinkedQueue1");
        testQueue(queue2, "ConcurrentLinkedQueue2");
    }

    private static void testQueue(Queue queue, String typeName) {
        long time1 = System.nanoTime();
        for (int i = 1; i < size; i++) {
            queue.offer(i);
        }
        long time2 = System.nanoTime();
        for (int i = 1; i < size; i++) {
            queue.poll();
        }
        long time3 = System.nanoTime();
        System.out.println("<" + typeName + ">offer time:" + (time2 - time1)+"ns");
        System.out.println("<" + typeName + ">poll time:" + (time3 - time2)+"ns");
    }
}
