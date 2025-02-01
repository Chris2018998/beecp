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
import org.stone.beecp.driver.MockConnection;
import org.stone.beecp.driver.MockXaConnection;

import javax.sql.XAConnection;
import java.sql.Connection;

public class MockBlockPoolImplementation implements BeeConnectionPool {

    public void init(BeeDataSourceConfig config) {
        //do nothing
    }

    public Connection getConnection() {
        return new MockConnection();
    }

    public XAConnection getXAConnection() {
        return new MockXaConnection(new MockConnection(), null);
    }

    public void close() {
        //do nothing
    }

    public boolean isClosed() {
        return false;
    }

    public void setPrintRuntimeLog(boolean indicator) {
        //do nothing
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
        //do nothing
    }

    public void clear(boolean forceCloseUsing, BeeDataSourceConfig config) {
        //do nothing
    }
}
