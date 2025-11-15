/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.pool;

import org.stone.beecp.BeeConnectionPoolMonitorVo;

import static org.stone.beecp.pool.ConnectionPoolStatics.*;

/**
 * Connection pool Monitor impl
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class FastConnectionPoolMonitorVo implements BeeConnectionPoolMonitorVo {
    private final String poolName;
    private final String poolMode;
    private final int maxSize;
    private final int semaphoreSize;

    private int poolState;
    private int idleSize;
    private int borrowedSize;
    private int semaphoreAcquiredSize;
    private int semaphoreWaitingSize;
    private int transferWaitingSize;
    private int creatingSize;
    private int creatingTimeoutSize;

    private boolean enabledLogPrint;
    private boolean enableMethodExecutionLogCache;

    public FastConnectionPoolMonitorVo(String poolName, String poolMode, int maxSize, int semaphoreSize) {
        this.poolName = poolName;
        this.poolMode = poolMode;
        this.maxSize = maxSize;
        this.semaphoreSize = semaphoreSize;
    }

    @Override
    public String getPoolName() {
        return poolName;
    }

    @Override
    public String getPoolMode() {
        return poolMode;
    }

    @Override
    public int getPoolState() {
        return poolState;
    }

    void setPoolState(int poolState) {
        this.poolState = poolState;
    }

    @Override
    public boolean isClosed() {
        return poolState == POOL_CLOSED;
    }

    @Override
    public boolean isReady() {
        return poolState == POOL_READY;
    }

    @Override
    public boolean isStarting() {
        return poolState == POOL_STARTING || poolState == POOL_RESTARTING;
    }

    @Override
    public int getMaxSize() {
        return maxSize;
    }

    @Override
    public int getSemaphoreSize() {
        return semaphoreSize;
    }

    @Override
    public int getBorrowedSize() {
        return borrowedSize;
    }

    void setBorrowedSize(int borrowedSize) {
        this.borrowedSize = borrowedSize;
    }

    @Override
    public int getIdleSize() {
        return idleSize;
    }

    void setIdleSize(int idleSize) {
        this.idleSize = idleSize;
    }

    @Override
    public int getCreatingSize() {
        return creatingSize;
    }

    void setCreatingSize(int creatingSize) {
        this.creatingSize = creatingSize;
    }

    @Override
    public int getCreatingTimeoutSize() {
        return creatingTimeoutSize;
    }

    void setCreatingTimeoutSize(int creatingTimeoutSize) {
        this.creatingTimeoutSize = creatingTimeoutSize;
    }

    @Override
    public int getSemaphoreAcquiredSize() {
        return semaphoreAcquiredSize;
    }

    void setSemaphoreAcquiredSize(int semaphoreAcquiredSize) {
        this.semaphoreAcquiredSize = semaphoreAcquiredSize;
    }

    @Override
    public int getSemaphoreWaitingSize() {
        return semaphoreWaitingSize;
    }

    void setSemaphoreWaitingSize(int semaphoreWaitingSize) {
        this.semaphoreWaitingSize = semaphoreWaitingSize;
    }

    @Override
    public int getTransferWaitingSize() {
        return transferWaitingSize;
    }

    void setTransferWaitingSize(int transferWaitingSize) {
        this.transferWaitingSize = transferWaitingSize;
    }

    @Override
    public boolean isEnabledLogPrint() {
        return enabledLogPrint;
    }

    void setEnabledLogPrint(boolean enabledLogPrint) {
        this.enabledLogPrint = enabledLogPrint;
    }

    @Override
    public boolean isEnabledMethodExecutionLogCache() {
        return enableMethodExecutionLogCache;
    }

    void setEnableMethodExecutionLogCache(boolean enableMethodExecutionLogCache) {
        this.enableMethodExecutionLogCache = enableMethodExecutionLogCache;
    }
}
