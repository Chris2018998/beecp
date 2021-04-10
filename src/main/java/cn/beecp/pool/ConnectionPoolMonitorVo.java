/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp.pool;

/**
 * Connection pool Monitor Vo
 *
 * @author Chris.Liao
 * @version 1.0
 */

public class ConnectionPoolMonitorVo {
    private String poolName;
    private String poolMode;
    private int poolState;
    private int maxActive;
    private int idleSize;
    private int usingSize;
    private int semaphoreWaiterSize;
    private int transferWaiterSize;

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

    public int getPoolState() {
        return poolState;
    }

    void setPoolState(int poolState) {
        this.poolState = poolState;
    }

    public int getMaxActive() {
        return maxActive;
    }

    void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
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

    public int getSemaphoreWaiterSize() {
        return semaphoreWaiterSize;
    }

    void setSemaphoreWaiterSize(int semaphoreWaiterSize) {
        this.semaphoreWaiterSize = semaphoreWaiterSize;
    }

    public int getTransferWaiterSize() {
        return transferWaiterSize;
    }

    void setTransferWaiterSize(int transferWaiterSize) {
        this.transferWaiterSize = transferWaiterSize;
    }
}
