/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
