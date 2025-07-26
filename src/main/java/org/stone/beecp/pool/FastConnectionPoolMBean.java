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

/**
 * Pool JMX Bean interface.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface FastConnectionPoolMBean {

    //return poolName
    String getPoolName();

    //return current size(using +idle)
    int getTotalSize();

    //return idle connection size
    int getIdleSize();

    //return borrowed connection size
    int getBorrowedSize();

    //return semaphore acquired successful size of pool
    int getSemaphoreAcquiredSize();

    //return waiting size to take semaphore
    int getSemaphoreWaitingSize();

    //return waiter size for transferred connection
    int getTransferWaitingSize();

    //set pool info debug switch
    void setPrintRuntimeLog(boolean indicator);

}

