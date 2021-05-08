/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp.pool;

/**
 * Pool JMX Bean interface
 *
 * @author Chris.Liao
 * @version 1.0
 */
public interface ConnectionPoolJmxBean {

    //return current size(using +idle)
    int getConnTotalSize();

    //return idle connection size
    int getConnIdleSize();

    //return using connection size
    int getConnUsingSize();

    //return permit size taken from semaphore
    int getSemaphoreAcquiredSize();

    //return waiting size to take semaphore permit
    int getSemaphoreWaitingSize();

    //return waiter size for transferred connection
    int getTransferWaitingSize();

}

