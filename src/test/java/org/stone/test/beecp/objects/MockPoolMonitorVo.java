/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.beecp.objects;

import org.stone.beecp.BeeConnectionPoolMonitorVo;

/**
 * Connection pool Monitor impl
 *
 * @author Chris Liao
 * @version 1.0
 */

public class MockPoolMonitorVo implements BeeConnectionPoolMonitorVo {
    private String poolName;
    private String poolMode;
    private int poolMaxSize;

    private int poolState;
    private int idleSize;
    private int borrowedSize;
    private int semaphoreWaitingSize;
    private int transferWaitingSize;
    private int creatingCount;
    private int creatingTimeoutCount;

    @Override
    public String getPoolMode() {
        return poolMode;
    }

    public void setPoolMode(String poolMode) {
        this.poolMode = poolMode;
    }

    @Override
    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    @Override
    public int getPoolState() {
        return poolState;
    }

    public void setPoolState(int poolState) {
        this.poolState = poolState;
    }

    @Override
    public int getPoolMaxSize() {
        return poolMaxSize;
    }

    public void setPoolMaxSize(int poolMaxSize) {
        this.poolMaxSize = poolMaxSize;
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
    public int getSemaphoreWaitingSize() {
        return semaphoreWaitingSize;
    }

    public void setSemaphoreWaitingSize(int semaphoreWaitingSize) {
        this.semaphoreWaitingSize = semaphoreWaitingSize;
    }

    @Override
    public int getCreatingCount() {
        return creatingCount;
    }

    public void setCreatingCount(int creatingCount) {
        this.creatingCount = creatingCount;
    }

    @Override
    public int getTransferWaitingSize() {
        return transferWaitingSize;
    }

    public void setTransferWaitingSize(int transferWaitingSize) {
        this.transferWaitingSize = transferWaitingSize;
    }

    @Override
    public int getCreatingTimeoutCount() {
        return creatingTimeoutCount;
    }

    public void setCreatingTimeoutCount(int creatingTimeoutCount) {
        this.creatingTimeoutCount = creatingTimeoutCount;
    }
}
