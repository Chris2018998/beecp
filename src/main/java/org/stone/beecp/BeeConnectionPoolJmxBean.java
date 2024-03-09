/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp;

/**
 * JMX Bean interface on {@link BeeConnectionPool}
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionPoolJmxBean {

    //return poolName
    String getPoolName();

    //return current size(using +idle)
    int getTotalSize();

    //return idle connection size
    int getIdleSize();

    //return using connection size
    int getUsingSize();

    //return semaphore acquired successful size of pool
    int getSemaphoreAcquiredSize();

    //return waiting size to take semaphore
    int getSemaphoreWaitingSize();

    //return waiter size for transferred connection
    int getTransferWaitingSize();

    //set pool info debug switch
    void setPrintRuntimeLog(boolean indicator);

}

