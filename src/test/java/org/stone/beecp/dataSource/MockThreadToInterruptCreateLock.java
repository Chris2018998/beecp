package org.stone.beecp.dataSource;

import org.stone.beecp.BeeConnectionPool;
import org.stone.beecp.BeeDataSource;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class MockThreadToInterruptCreateLock extends Thread {
    private BeeDataSource ds;
    private BeeConnectionPool pool;

    public MockThreadToInterruptCreateLock(BeeDataSource ds) {
        this.ds = ds;
    }

    public MockThreadToInterruptCreateLock(BeeConnectionPool pool) {
        this.pool = pool;
    }

    public void run() {
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        try {
            if (ds != null) {
                long holdTimeMillsOnLock = ds.getPoolLockHoldTime();
                if (holdTimeMillsOnLock > 0L) {
                    ds.interruptOnPoolLock();
                }
            } else {
                long holdTimeMillsOnLock = pool.getPoolLockHoldTime();
                if (holdTimeMillsOnLock > 0L) {
                    pool.interruptOnPoolLock();
                }
            }
        } catch (SQLException e) {
            //do nothing
        }
    }
}

