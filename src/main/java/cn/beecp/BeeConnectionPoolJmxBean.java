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
 * Pool JMX Bean interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionPoolJmxBean {

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

