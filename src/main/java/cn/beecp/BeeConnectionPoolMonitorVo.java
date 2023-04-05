/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package cn.beecp;

/**
 * Connection pool monitor interface
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

    int getUsingSize();

    int getSemaphoreWaitingSize();

    int getTransferWaitingSize();
}
