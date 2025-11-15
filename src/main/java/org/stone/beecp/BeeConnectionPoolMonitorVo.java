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

    String getPoolName();

    String getPoolMode();

    int getPoolState();

    boolean isClosed();

    boolean isReady();

    boolean isStarting();

    int getMaxSize();

    int getIdleSize();

    int getBorrowedSize();

    int getCreatingSize();

    int getCreatingTimeoutSize();

    int getSemaphoreSize();

    int getSemaphoreAcquiredSize();

    int getSemaphoreWaitingSize();

    int getTransferWaitingSize();

    boolean isEnabledLogPrint();

    boolean isEnabledMethodExecutionLogCache();
}
