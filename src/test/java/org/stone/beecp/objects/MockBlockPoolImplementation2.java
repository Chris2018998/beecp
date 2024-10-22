/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.objects;

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
public class MockBlockPoolImplementation2 implements BeeConnectionPool {

    public MockBlockPoolImplementation2() {
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
    }

    public void init(BeeDataSourceConfig config) {
        //do noting
    }

    public Connection getConnection() {
        return null;
    }

    public XAConnection getXAConnection() {
        return null;
    }

    public void close() {
        //do noting
    }

    public boolean isClosed() {
        return false;
    }

    public void setPrintRuntimeLog(boolean indicator) {
        //do noting
    }

    public BeeConnectionPoolMonitorVo getPoolMonitorVo() {
        return null;
    }

    public int getConnectionCreatingCount() {
        return 0;
    }

    public int getConnectionCreatingTimeoutCount() {
        return 0;
    }

    public Thread[] interruptConnectionCreating(boolean interruptTimeout) {
        return null;
    }

    public void clear(boolean forceCloseUsing) {
        //do noting
    }

    public void clear(boolean forceCloseUsing, BeeDataSourceConfig config) {
        //do noting
    }
}
