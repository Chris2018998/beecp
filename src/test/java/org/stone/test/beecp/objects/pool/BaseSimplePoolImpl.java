/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.objects.pool;

import org.stone.beecp.*;
import org.stone.test.beecp.driver.MockConnection;
import org.stone.test.beecp.driver.MockXaConnection;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;

/**
 * @author Chris Liao
 */
public class BaseSimplePoolImpl implements BeeConnectionPool {

    public void start(BeeDataSourceConfig config) {
        //do nothing
    }

    public Connection getConnection() {
        return new MockConnection();
    }

    public XAConnection getXAConnection() {
        return new MockXaConnection(new MockConnection(), null);
    }

    public Connection getConnection(String username, String password) {
        return new MockConnection();
    }

    public XAConnection getXAConnection(String username, String password) {
        return new MockXaConnection(new MockConnection(), null);
    }


    public void close() {
    }

    public boolean suspendPool() {
        return false;
    }

    public boolean resumePool() {
        return false;
    }

    public boolean isClosed() {
        return false;
    }

    public void enableLogPrinter(boolean enable) {
    }

    public void enableLogCache(boolean enable) {
        //do nothing
    }

    public boolean cancelStatement(String logId) {
        return false;
    }

    /**
     * Get Jdbc logs with a give type
     *
     * @param type is log type to query
     */
    public List<BeeMethodLog> getLogs(int type) {
        return Collections.emptyList();
    }

    /**
     * Clear All logs in log collector.
     */
    public void clearLogs(int type) {
    }

    /**
     * Set a new log handler to pool.
     *
     * @param handler to handle method logs
     */
    public void changeLogListener(BeeMethodLogListener handler) {

    }

    public BeeConnectionPoolMonitorVo getPoolMonitorVo() {
        return null;
    }

    public List<Thread> interruptWaitingThreads() {
        return null;
    }

    public void restart(boolean forceCloseUsing) {
        //do nothing
    }

    public void restart(boolean forceCloseUsing, BeeDataSourceConfig config) {
        //do nothing
    }
}
