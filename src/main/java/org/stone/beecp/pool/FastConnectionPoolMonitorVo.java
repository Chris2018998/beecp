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
    private String poolName;
    private boolean isFairMode;
    private int maxSize;
    private int semaphoreSize;
    private boolean useThreadLocal;

    private int poolState;
    private int idleSize;
    private int borrowedSize;
    private int semaphoreRemainSize;
    private int semaphoreWaitingSize;
    private int transferWaitingSize;
    private int creatingSize;
    private int creatingTimeoutSize;
    private boolean enabledLogPrinter;
    private boolean enabledLogCache;

    public FastConnectionPoolMonitorVo(String poolName,
                                       boolean isFairMode,
                                       int maxSize,
                                       int semaphoreSize,
                                       boolean useThreadLocal,

                                       int poolState,
                                       int idleSize,
                                       int borrowedSize,
                                       int semaphoreRemainSize,
                                       int semaphoreWaitingSize,
                                       int transferWaitingSize,
                                       int creatingSize,
                                       int creatingTimeoutSize,
                                       boolean enabledLogPrint,
                                       boolean enabledLogCache) {
        this.poolName = poolName;
        this.isFairMode = isFairMode;
        this.maxSize = maxSize;
        this.semaphoreSize = semaphoreSize;
        this.useThreadLocal = useThreadLocal;

        this.poolState = poolState;
        this.idleSize = idleSize;
        this.borrowedSize = borrowedSize;
        this.semaphoreRemainSize = semaphoreRemainSize;
        this.semaphoreWaitingSize = semaphoreWaitingSize;
        this.transferWaitingSize = transferWaitingSize;
        this.creatingSize = creatingSize;
        this.creatingTimeoutSize = creatingTimeoutSize;
        this.enabledLogPrinter = enabledLogPrint;
        this.enabledLogCache = enabledLogCache;
    }

    //for simple
    FastConnectionPoolMonitorVo() {
    }

    void fillSimple(int idleSize, int borrowedSize, int semaphoreRemainSize, int semaphoreWaitingSize) {
        this.idleSize = idleSize;
        this.borrowedSize = borrowedSize;
        this.semaphoreRemainSize = semaphoreRemainSize;
        this.semaphoreWaitingSize = semaphoreWaitingSize;
    }

    //***************************************************************************************************************//
    //                                        1: Pool base fields                                                    //
    //***************************************************************************************************************//
    @Override
    public String getPoolName() {
        return poolName;
    }

    @Override
    public boolean isFairMode() {
        return isFairMode;
    }

    @Override
    public boolean useThreadLocal() {
        return useThreadLocal;
    }

    //***************************************************************************************************************//
    //                                     2: State methods                                                          //
    //***************************************************************************************************************//
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

    //***************************************************************************************************************//
    //                                    3: Other methods                                                           //
    //***************************************************************************************************************//
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

    @Override
    public int getIdleSize() {
        return idleSize;
    }

    @Override
    public int getCreatingSize() {
        return creatingSize;
    }

    @Override
    public int getCreatingTimeoutSize() {
        return creatingTimeoutSize;
    }

    @Override
    public int getSemaphoreRemainSize() {
        return semaphoreRemainSize;
    }

    @Override
    public int getSemaphoreWaitingSize() {
        return semaphoreWaitingSize;
    }

    @Override
    public int getTransferWaitingSize() {
        return transferWaitingSize;
    }

    @Override
    public boolean isEnabledLogPrinter() {
        return enabledLogPrinter;
    }

    @Override
    public boolean isEnabledLogCache() {
        return enabledLogCache;
    }

    @Override
    public String toString() {
        return getPoolStateDesc(this.poolState);
    }
}
