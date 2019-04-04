package org.jmin.bee.pool.util;

import java.util.Timer;
import java.util.TimerTask;

/**
 * System Clock
 * 
 * @author Chris.liao
 */
public class SystemClock extends TimerTask {
	private volatile long currentTimeMillis = 0;
	private static final SystemClock CLOCK = new SystemClock(1);

	private SystemClock(long period) {
		this.currentTimeMillis = System.currentTimeMillis();
		new Timer(true).schedule(this, 1, period);
	}

	public void run() {
		this.currentTimeMillis = System.currentTimeMillis();
	}

	public static long currentTimeMillis() {
		return CLOCK.currentTimeMillis;
	}
}
