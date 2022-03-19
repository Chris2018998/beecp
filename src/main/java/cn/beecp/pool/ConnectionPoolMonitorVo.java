/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool;

/**
 * Connection pool Monitor Vo
 *
 * @author Chris.Liao
 * @version 1.0
 */

public class ConnectionPoolMonitorVo {
    private final String hostIP;
    private final long threadId;
    private final String threadName;
    private final String poolName;
    private final String poolMode;
    private final int poolMaxSize;

    private int poolState;
    private int idleSize;
    private int usingSize;
    private int semaphoreWaitingSize;
    private int transferWaitingSize;

    ConnectionPoolMonitorVo(String poolName, String poolMode, int poolMaxSize,
                            String hostIP, long threadId, String threadName) {
        this.poolName = poolName;
        this.poolMode = poolMode;
        this.poolMaxSize = poolMaxSize;
        this.hostIP = hostIP;
        this.threadId = threadId;
        this.threadName = threadName;
    }

    public String getHostIP() {
        return this.hostIP;
    }

    public long getThreadId() {
        return this.threadId;
    }

    public String getThreadName() {
        return this.threadName;
    }

    public String getPoolName() {
        return this.poolName;
    }

    public String getPoolMode() {
        return this.poolMode;
    }

    public int getPoolMaxSize() {
        return this.poolMaxSize;
    }

    public int getPoolState() {
        return this.poolState;
    }

    void setPoolState(int poolState) {
        this.poolState = poolState;
    }

    public int getIdleSize() {
        return this.idleSize;
    }

    void setIdleSize(int idleSize) {
        this.idleSize = idleSize;
    }

    public int getUsingSize() {
        return this.usingSize;
    }

    void setUsingSize(int usingSize) {
        this.usingSize = usingSize;
    }

    public int getSemaphoreWaitingSize() {
        return this.semaphoreWaitingSize;
    }

    void setSemaphoreWaitingSize(int semaphoreWaitingSize) {
        this.semaphoreWaitingSize = semaphoreWaitingSize;
    }

    public int getTransferWaitingSize() {
        return this.transferWaitingSize;
    }

    void setTransferWaitingSize(int transferWaitingSize) {
        this.transferWaitingSize = transferWaitingSize;
    }
}
