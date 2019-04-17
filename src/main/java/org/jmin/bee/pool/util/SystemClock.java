package org.jmin.bee.pool.util;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Time Clock
 * 
 * @author Chris.liao
 */
public class SystemClock implements Runnable {
	private volatile long nowTimeMillis = System.currentTimeMillis();
	public static final SystemClock clock = new SystemClock(1);

	private SystemClock(long period) {
		ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "System Clock");
				thread.setDaemon(true);
				return thread;
			}
		});
		scheduler.scheduleAtFixedRate(this, period, period, TimeUnit.MILLISECONDS);
	}
	public void run() {
		this.nowTimeMillis = System.currentTimeMillis();
	}
	public static long nanoTime() {
		return System.nanoTime();
	}
	public static long currentTimeMillis() {
		return clock.nowTimeMillis;
	}
	public static void main(String[] args) {
		long time1= System.nanoTime();
		for (int i = 0; i < 1000; i++) {
			SystemClock.currentTimeMillis();
		}
		long time2= System.nanoTime();
		
		
		long time3= System.nanoTime();
		for (int i = 0; i < 1000; i++) {
			System.currentTimeMillis();
		}
		long time4= System.nanoTime();
		
		
		System.out.println("s1:" + (time2-time1));
		System.out.println("t2:" + (time4-time3));
	}
}