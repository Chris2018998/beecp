package org.jmin.bee.pool.util;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Time Clock
 * 
 * @author Chris.liao
 */
public final class SystemClock implements Runnable{
	private volatile long millSecond;
	public static final SystemClock clock = new SystemClock(1);
	private SystemClock(long period) {
		ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1,new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "System Clock");
				thread.setDaemon(true);
				return thread;
			}
		});
		this.millSecond=System.currentTimeMillis();
		scheduler.scheduleAtFixedRate(this,period,period,TimeUnit.MILLISECONDS);
	}
	
	public void run(){
		this.millSecond=System.currentTimeMillis();
	}
	public long currentTimeMillis() {
		return millSecond;
	}
}