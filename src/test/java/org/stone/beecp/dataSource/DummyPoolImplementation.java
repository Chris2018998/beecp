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
import java.sql.SQLException;
import java.util.concurrent.locks.LockSupport;

class DummyPoolImplementation implements BeeConnectionPool {
    static {
        LockSupport.park();
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

    public long getElapsedTimeSinceCreationLock() {
        return 0L;
    }

    public void interruptThreadsOnCreationLock() {
    }

    public void clear(boolean forceCloseUsing) {
    }

    public void clear(boolean forceCloseUsing, BeeDataSourceConfig config) throws SQLException {
    }
}