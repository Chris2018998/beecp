package cn.beecp.test.other;

import cn.beecp.util.ConcurrentTransferQueue;

import java.math.BigDecimal;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TransferQueue Performance Test
 *
 * @author Chris.Liao
 */
public class TransferQueueTest{
    public static void main(String[]args)throws Exception{
        int offerThreadSize=10,pollThreadSize=10,takeTimes=100;

//      testTransferQueue("ArrayBlockingQueue",new ArrayBlockingQueue<Object>(10000),offerThreadSize,pollThreadSize,takeTimes);
//      testTransferQueue("LinkedBlockingQueue",new LinkedBlockingQueue<Object>(),offerThreadSize,pollThreadSize,takeTimes);
//      testTransferQueue("LinkedTransferQueue",new LinkedTransferQueue<Object>(),offerThreadSize,pollThreadSize,takeTimes);
        testTransferQueue("ConcurrentTransferQueue",new ConcurrentTransferQueue<Object>(),offerThreadSize,pollThreadSize,takeTimes);
    }

    private static void testTransferQueue(String queueName,Queue<Object> queue,int offerThreadSize, int pollThreadSize,int takeTimes) throws Exception{
        long startTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        CountDownLatch latch = new CountDownLatch(pollThreadSize);
        PutThread[] putThreads = new PutThread[offerThreadSize];
        PollThread[] pollThreads = new PollThread[pollThreadSize];
        AtomicBoolean activeInd=new AtomicBoolean(true);

        //put
        for(int i=0;i<offerThreadSize;i++){
            putThreads[i]=new PutThread(queue,startTime,activeInd);
            putThreads[i].start();
        }

        //poll
        for(int i=0;i<pollThreadSize;i++){
            pollThreads[i]=new PollThread(queue,startTime,takeTimes,latch);
            pollThreads[i].start();
        }
        latch.await();
        activeInd.set(false);

        //summary tot time and avr time
        BigDecimal totalTime = new BigDecimal(0);
        for(int i=0;i<pollThreadSize;i++){
            totalTime= totalTime.add(new BigDecimal(pollThreads[i].tookTime));
        }
        BigDecimal avgTime =  totalTime.divide(new BigDecimal(pollThreadSize*takeTimes));
        System.out.println("<"+queueName+">offerThreadSize:"+offerThreadSize+",pollThreadSize:"
                +pollThreadSize +",poll times:"+pollThreadSize*takeTimes+",total time:"+ totalTime.longValue()
                +"(ns),avg time:"+avgTime+"(ns)");
    }
}

class PutThread extends Thread {
    private long startTime;
    private Queue<Object> queue;
    private AtomicBoolean activeInd;
    private Random random = new Random();
    public PutThread(Queue<Object> queue, long startTime,AtomicBoolean activeInd) {
        this.queue = queue;
        this.startTime = startTime;
        this.activeInd=activeInd;
    }
    public void run() {
        while(activeInd.get()) {
            queue.offer(random.nextInt());
        }
    }
}
class PollThread extends Thread {
    long tookTime;
    private long startTime;
    private int loopTimes;
    private Queue<Object> queue;
    private CountDownLatch latch;

    public PollThread(Queue<Object> queue, long startTime, int loopTimes,CountDownLatch latch) {
        this.queue = queue;
        this.loopTimes = loopTimes;
        this.startTime = startTime;
        this.latch = latch;
    }

    public void run() {
        LockSupport.parkNanos(startTime - System.nanoTime());
        long time1 = System.nanoTime();
        for (int i = 0; i < loopTimes; i++) {
            queue.poll();
        }
        tookTime = System.nanoTime() - time1;
        latch.countDown();
    }
}

