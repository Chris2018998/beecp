package org.jmin.bee.pool.util;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.locks.LockSupport;
/**
 * Time Clock
 * 
 * @author Chris.liao
 */
public final class SystemClock extends Thread{
	private final long period;
	private boolean isNormal=true;
	private volatile long currentTimeMillis=currentTimeMillis();
	static final SystemClock clock = new SystemClock(MILLISECONDS.toNanos(1));
	private SystemClock(long period) {
		this.period=period;
		this.setDaemon(true);
		this.start();
	}
	public void run(){
		while (isNormal) {
			currentTimeMillis = currentTimeMillis();
			LockSupport.parkNanos(this,period);
		}
	}
	public static void terminate() {
		clock.isNormal=false;
		LockSupport.unpark(clock);
	}
	public static long curTimeMillis() {
		return clock.currentTimeMillis;
	}
}