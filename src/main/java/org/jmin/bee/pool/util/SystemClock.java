package org.jmin.bee.pool.util;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Time Clock
 * 
 * @author Chris.liao
 */
public class SystemClock{
	private volatile long millSecond = System.currentTimeMillis();
	public static final SystemClock clock = new SystemClock(1);
	private SystemClock(long period) {
		ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1,new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "System Clock");
				thread.setDaemon(true);
				return thread;
			}
		});
		
		scheduler.scheduleAtFixedRate(
				new Runnable(){public void run(){SystemClock.this.millSecond=System.currentTimeMillis();}}, 
				period, period, TimeUnit.MILLISECONDS);
	}
	public final static long currentTimeMillis() {
		return clock.millSecond;
	}
}