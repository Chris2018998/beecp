/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp;

/**
 * Pool monitoring interface.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionPoolMonitorVo {

    String getDsId();

    String getDsUUID();

    String getHostIP();

    long getThreadId();

    String getThreadName();

    String getPoolName();

    String getPoolMode();

    int getPoolMaxSize();

    int getPoolState();

    int getIdleSize();

    int getBorrowedSize();

    int getSemaphoreWaitingSize();

    int getTransferWaitingSize();

    int getCreatingCount();

    int getCreatingTimeoutCount();
}
