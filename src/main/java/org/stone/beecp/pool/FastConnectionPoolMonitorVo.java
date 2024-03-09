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

/**
 * Connection pool Monitor Vo
 *
 * @author Chris Liao
 * @version 1.0
 */

public class FastConnectionPoolMonitorVo implements BeeConnectionPoolMonitorVo {
    private String dsId;
    private String dsUUID;
    private String hostIP;
    private long threadId;
    private String threadName;
    private String poolName;
    private String poolMode;
    private int poolMaxSize;

    private int poolState;
    private int idleSize;
    private int usingSize;
    private int semaphoreWaitingSize;
    private int transferWaitingSize;

    public String getDsId() {
        return dsId;
    }

    void setDsId(String dsId) {
        this.dsId = dsId;
    }

    public String getDsUUID() {
        return dsUUID;
    }

    void setDsUUID(String dsUUID) {
        this.dsUUID = dsUUID;
    }

    public String getHostIP() {
        return hostIP;
    }

    void setHostIP(String hostIP) {
        this.hostIP = hostIP;
    }

    public long getThreadId() {
        return threadId;
    }

    void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public String getThreadName() {
        return threadName;
    }

    void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getPoolName() {
        return poolName;
    }

    void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public String getPoolMode() {
        return poolMode;
    }

    void setPoolMode(String poolMode) {
        this.poolMode = poolMode;
    }

    public int getPoolMaxSize() {
        return poolMaxSize;
    }

    void setPoolMaxSize(int poolMaxSize) {
        this.poolMaxSize = poolMaxSize;
    }

    public int getPoolState() {
        return poolState;
    }

    void setPoolState(int poolState) {
        this.poolState = poolState;
    }

    public int getIdleSize() {
        return idleSize;
    }

    void setIdleSize(int idleSize) {
        this.idleSize = idleSize;
    }

    public int getUsingSize() {
        return usingSize;
    }

    void setUsingSize(int usingSize) {
        this.usingSize = usingSize;
    }

    public int getSemaphoreWaitingSize() {
        return semaphoreWaitingSize;
    }

    void setSemaphoreWaitingSize(int semaphoreWaitingSize) {
        this.semaphoreWaitingSize = semaphoreWaitingSize;
    }

    public int getTransferWaitingSize() {
        return transferWaitingSize;
    }

    void setTransferWaitingSize(int transferWaitingSize) {
        this.transferWaitingSize = transferWaitingSize;
    }
}
