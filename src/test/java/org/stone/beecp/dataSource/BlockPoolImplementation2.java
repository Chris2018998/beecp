/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.dataSource;

import org.stone.beecp.BeeConnectionPool;
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSourceConfig;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris Liao
 */
public class BlockPoolImplementation2 implements BeeConnectionPool {

    public BlockPoolImplementation2() {
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
    }

    public void init(BeeDataSourceConfig config) {
    }

    public Connection getConnection() {
        return null;
    }

    public XAConnection getXAConnection() {
        return null;
    }

    public void close() {
    }

    public boolean isClosed() {
        return false;
    }

    public void setPrintRuntimeLog(boolean indicator) {
    }

    public BeeConnectionPoolMonitorVo getPoolMonitorVo() {
        return null;
    }

    public long getPoolLockHoldTime() {
        return 0L;
    }

    public Thread[] interruptOnPoolLock() {
        return null;
    }

    public void clear(boolean forceCloseUsing) {
    }

    public void clear(boolean forceCloseUsing, BeeDataSourceConfig config) {
    }
}
