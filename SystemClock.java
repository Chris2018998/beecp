package org.jmin.bee.pool.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Time Clock
 * 
 * @author Chris.liao
 */
public final class SystemClock extends Thread{
	static final SystemClock clock = new SystemClock();
	private static final long period=TimeUnit.MILLISECONDS.toNanos(1);
	private volatile long currentTimeMillis=System.currentTimeMillis();
	private SystemClock() {
		this.setDaemon(true);
		this.start();
	}
	public void run(){
		while (true) {
			currentTimeMillis = System.currentTimeMillis();
			LockSupport.parkNanos(period);
		}
	}
	public static long currentTimeMillis() {
		return clock.currentTimeMillis;
	}
}