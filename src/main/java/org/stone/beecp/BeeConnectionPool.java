/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp;

import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * A Container maintains some borrowable connections for being reused.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionPool {

    /**
     * Pool initializes,if success,pool state become to be ready for working,then borrowers can get connections from it.
     *
     * @param config is a configuration object,some items of it are used in initialization
     * @throws SQLException when initializes failed
     */
    void init(BeeDataSourceConfig config) throws SQLException;

    /**
     * Attempts to get a connection from pool.
     *
     * @return a borrowed connection
     * @throws SQLException                      when pool creates a connection failed
     * @throws ConnectionGetTimeoutException     when borrower wait time out in pool
     * @throws ConnectionGetInterruptedException if interrupted while waiting in pool
     */
    Connection getConnection() throws SQLException;

    /**
     * Attempts to get a XAConnection from pool.
     *
     * @return a borrowed XAConnection
     * @throws SQLException                      when pool creates a xa connection failed
     * @throws ConnectionGetTimeoutException     when borrower wait time out in pool
     * @throws ConnectionGetInterruptedException if interrupted while waiting in pool
     */
    XAConnection getXAConnection() throws SQLException;

    /**
     * Closes all connections in pool and shutdown all work threads in pool,then pool state change to closed from working,
     * disable all operation on pool.
     */
    void close();

    /**
     * Query pool state whether is closed.
     *
     * @return true that pool is closed
     */
    boolean isClosed();

    /**
     * Changes switch of log print.
     *
     * @param indicator is true that prints logs of pool work,false that disable print
     */
    void setPrintRuntimeLog(boolean indicator);

    /**
     * Gets a monitor object of pool runtime info.
     *
     * @return monitor object of pool
     */
    BeeConnectionPoolMonitorVo getPoolMonitorVo();

    /**
     * Get start time of connection creation,if return value is not equal to zero,means that some thread is creating a connection.
     *
     * @return a nanoseconds time;if not exists a creating thread,then return 0
     */
    long getCreatingTime();

    /**
     * Query elapsed time of connection creation whether is timeout.
     *
     * @return true that creation is timeout
     */
    boolean isCreatingTimeout();

    /**
     * Interrupts a thread creating a connection and threads waiting to create connections.
     *
     * @return interrupted threads,if not exists creating thread and waiting threads,return an empty array
     */
    Thread[] interruptOnCreation();

    /**
     * Closes all connections and removes them from pool.
     *
     * @param forceCloseUsing is an indicator that close borrowed connections immediately,or that close them when them return to pool
     */
    void clear(boolean forceCloseUsing);

    /**
     * Closes all connections and removes them from pool,then re-initialize pool with new configuration.
     *
     * @param forceCloseUsing is an indicator that close borrowed connections immediately,or that close them when them return to pool
     * @param config          is a new configuration object
     * @throws BeeDataSourceConfigException when check failed on this new configuration
     * @throws SQLException                 when pool reinitialize failed
     */
    void clear(boolean forceCloseUsing, BeeDataSourceConfig config) throws SQLException;

}
	
