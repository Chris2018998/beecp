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
        //do nothing
    }

    public boolean isClosed() {
        return false;
    }

    public boolean isReady() {
        return false;
    }

    public void enableLogPrint(boolean indicator) {
        //do nothing
    }

    public boolean isEnabledLogPrint() {
        return false;
    }

    public void enableMethodExecutionLogCache(boolean enable) {
        //do nothing
    }

    public boolean isEnabledMethodExecutionLogCache() {
        return false;
    }

    public boolean cancelStatement(Object logId) {
        return false;
    }

    /**
     * Get Jdbc logs with a give type
     *
     * @param type is log type to query
     */
    public List<BeeMethodExecutionLog> getMethodExecutionLog(int type) {
        return Collections.emptyList();
    }

    /**
     * Clear All logs in log collector.
     */
    public List<BeeMethodExecutionLog> clearMethodExecutionLog(int type) {
        return null;
    }

    /**
     * Set a new log handler to pool.
     *
     * @param handler to handle method logs
     */
    public void setMethodExecutionListener(BeeMethodExecutionListener handler) {

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
