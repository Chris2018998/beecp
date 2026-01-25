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

import java.io.Serializable;

/**
 * Pool monitoring interface.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionPoolMonitorVo extends Serializable {

    //***************************************************************************************************************//
    //                                        1: Unchangeable fields                                                 //
    //***************************************************************************************************************//

    String getPoolName();

    boolean isFairMode();

    boolean useThreadLocal();

    //***************************************************************************************************************//
    //                                     2: State`methods                                                           //
    //***************************************************************************************************************//
    boolean isLazy();

    boolean isNew();

    boolean isReady();

    boolean isClosing();

    boolean isStarting();

    boolean isRestarting();

    boolean isRestartFailed();

    boolean isSuspended();

    //***************************************************************************************************************//
    //                                     3: Other methods                                                          //
    //***************************************************************************************************************//

    int getMaxSize();

    int getIdleSize();

    int getBorrowedSize();

    int getCreatingSize();

    int getCreatingTimeoutSize();

    int getSemaphoreSize();

    int getSemaphoreRemainSize();

    int getSemaphoreWaitingSize();

    int getTransferWaitingSize();

    boolean isEnabledLogPrinter();

    boolean isEnabledLogCache();
}
