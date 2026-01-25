/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.beecp.objects.pool;

import org.stone.beecp.BeeConnectionPoolMonitorVo;

import static org.stone.beecp.pool.ConnectionPoolStatics.*;

/**
 * Connection pool Monitor impl
 *
 * @author Chris Liao
 * @version 1.0
 */

public class PoolMonitorVoImpl implements BeeConnectionPoolMonitorVo {
    private String poolName;
    private boolean poolMode;
    private int poolState;
    private int maxSize;
    private int semaphoreSize;
    private boolean useThreadLocal;

    private int idleSize;
    private int borrowedSize;
    private int semaphoreAcquiredSize;
    private int semaphoreWaitingSize;
    private int transferWaitingSize;
    private int creatingSize;
    private int creatingTimeoutSize;

    private boolean enabledLogPrint;
    private boolean enabledJdbcEventLogManager;

    @Override
    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    @Override
    public boolean isFairMode() {
        return poolMode;
    }

    public boolean useThreadLocal() {
        return useThreadLocal;
    }

    public void setUsingThreadLocal(boolean useThreadLocal) {
        this.useThreadLocal = useThreadLocal;
    }

    public void setPoolMode(boolean poolMode) {
        this.poolMode = poolMode;
    }

    public int getPoolState() {
        return this.poolState;
    }

    public void setPoolState(int poolState) {
        this.poolState = poolState;
    }

    @Override
    public boolean isLazy() {
        return poolState == POOL_LAZY;
    }

    @Override
    public boolean isNew() {
        return poolState == POOL_NEW;
    }

    @Override
    public boolean isClosing() {
        return poolState == POOL_CLOSING;
    }

    @Override
    public boolean isReady() {
        return poolState == POOL_READY;
    }

    @Override
    public boolean isStarting() {
        return poolState == POOL_STARTING;
    }

    @Override
    public boolean isRestarting() {
        return poolState == POOL_RESTARTING;
    }

    @Override
    public boolean isRestartFailed() {
        return poolState == POOL_RESTART_FAILED;
    }

    @Override
    public boolean isSuspended() {
        return poolState == POOL_SUSPENDED;
    }

    @Override
    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public int getSemaphoreSize() {
        return semaphoreSize;
    }

    public void setSemaphoreSize(int semaphoreSize) {
        this.semaphoreSize = semaphoreSize;
    }

    @Override
    public int getIdleSize() {
        return idleSize;
    }

    public void setIdleSize(int idleSize) {
        this.idleSize = idleSize;
    }

    @Override
    public int getBorrowedSize() {
        return borrowedSize;
    }

    public void setBorrowedSize(int borrowedSize) {
        this.borrowedSize = borrowedSize;
    }

    @Override
    public int getSemaphoreRemainSize() {
        return semaphoreAcquiredSize;
    }

    public void setSemaphoreAcquiredSize(int semaphoreAcquiredSize) {
        this.semaphoreAcquiredSize = semaphoreAcquiredSize;
    }

    @Override
    public int getSemaphoreWaitingSize() {
        return semaphoreWaitingSize;
    }

    public void setSemaphoreWaitingSize(int semaphoreWaitingSize) {
        this.semaphoreWaitingSize = semaphoreWaitingSize;
    }

    @Override
    public int getTransferWaitingSize() {
        return transferWaitingSize;
    }

    public void setTransferWaitingSize(int transferWaitingSize) {
        this.transferWaitingSize = transferWaitingSize;
    }

    @Override
    public int getCreatingSize() {
        return creatingSize;
    }

    public void setCreatingSize(int creatingSize) {
        this.creatingSize = creatingSize;
    }

    @Override
    public int getCreatingTimeoutSize() {
        return creatingTimeoutSize;
    }

    public void setCreatingTimeoutSize(int creatingTimeoutSize) {
        this.creatingTimeoutSize = creatingTimeoutSize;
    }

    @Override
    public boolean isEnabledLogPrinter() {
        return enabledLogPrint;
    }

    public void setEnabledLogPrint(boolean enabledLogPrint) {
        this.enabledLogPrint = enabledLogPrint;
    }

    @Override
    public boolean isEnabledLogCache() {
        return enabledJdbcEventLogManager;
    }

    public void setEnabledJdbcEventLogManager(boolean enabledJdbcEventLogManager) {
        this.enabledJdbcEventLogManager = enabledJdbcEventLogManager;
    }
}

