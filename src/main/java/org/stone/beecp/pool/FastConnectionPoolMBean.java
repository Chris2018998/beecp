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

    String getPoolName();

    int getSemaphoreSize();

    int getSemaphoreAcquiredSize();

    int getSemaphoreWaitingSize();

    int getTransferWaitingSize();

    int getMaxSize();

    int getIdleSize();

    int getBorrowedSize();

    boolean isPrintRuntimeLog();

    void setPrintRuntimeLog(boolean enable);

}

