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
import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Connection pool interface,a default implementation is provided for it,@see{@link org.stone.beecp.pool.FastConnectionPool}
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionPool extends Closeable {

    /**
     * Pool startup with a configuration object.
     *
     * @param config is a configuration object defines some items can be applied for pool
     * @throws BeeDataSourceConfigException when configuration check fail
     * @throws SQLException                 when fail to create initialization connection
     */
    void start(BeeDataSourceConfig config) throws SQLException;

    /**
     * Pool re-startup with last used configuration when pool is in ready state
     *
     * @param forceRecycleBorrowed is true that pool close borrowed connections immediately;false that pool wait borrowed released to pool,then close them
     * @throws SQLException when pool is closed or in clearing
     */
    void restart(boolean forceRecycleBorrowed) throws SQLException;

    /**
     * Pool re-startup with a new configuration when pool is in ready state
     *
     * @param forceRecycleBorrowed is true that pool close borrowed connections immediately;false that pool wait borrowed released to pool,then close them
     * @param config               is a new configuration object for reinitialization
     * @throws SQLException                 when pool is closed or in clearing
     * @throws BeeDataSourceConfigException when configuration check fail
     * @throws SQLException                 when pool reinitialize fail
     */
    void restart(boolean forceRecycleBorrowed, BeeDataSourceConfig config) throws SQLException;

    /**
     * Attempts to get a connection from pool.
     *
     * @return a borrowed connection
     * @throws ConnectionGetTimeoutException     when wait timeout in pool
     * @throws ConnectionGetInterruptedException while interruption occurred during waiting
     * @throws SQLException                      when fail to create a connection
     */
    Connection getConnection() throws SQLException;

    /**
     * Attempts to get a XAConnection from pool.
     *
     * @return a borrowed XAConnection
     * @throws ConnectionGetTimeoutException     when wait timeout in pool
     * @throws ConnectionGetInterruptedException while interruption occurred during waiting
     * @throws SQLException                      when fail to create a xa connection
     */
    XAConnection getXAConnection() throws SQLException;

    /**
     * Shutdown pool
     */
    void close();

    /**
     * Queries pool state whether is closed.
     *
     * @return true when pool is closed
     */
    boolean isClosed();

    /**
     * Queries pool state whether is ready to accept get request
     *
     * @return true when pool is closed
     */
    boolean isReady();

    /**
     * Interrupts all threads in waiting.
     *
     * @return a list of interrupted threads
     */
    List<Thread> interruptWaitingThreads();

    /**
     * Gets runtime monitoring object of pool,refer to {@link BeeConnectionPoolMonitorVo}.
     *
     * @return monitoring object of pool
     */
    BeeConnectionPoolMonitorVo getPoolMonitorVo();

    /**
     * Query logs print state whether in being enabled.
     *
     * @return boolean true is enabled,false is disabled
     */
    boolean isEnabledLogPrint();

    /**
     * A switch method to enable or disable logs print in pool.
     *
     * @param enable is true that log print is enabled, false is not print
     */
    void enableLogPrint(boolean enable);


    //***************************************************************************************************************//
    //                                         3: method execution logs                                              //
    //***************************************************************************************************************//

    /**
     * Queries method log cache whether being enabled in pool.
     *
     * @return boolean true is enabled,false is disabled
     */
    boolean isEnabledMethodExecutionLogCache();

    /**
     * A switch method to enable or disable method log cache
     *
     * @param enable is true that make cache to collect method logs;false that make it to stop work
     */
    void enableMethodExecutionLogCache(boolean enable);

    /**
     * Gets logs from pool with specified type.
     *
     * @param type should be one of[BeeMethodExecutionLog.Type_Connection_Get,BeeMethodExecutionLog.Type_SQL_Execution];if not,then return all logs
     * @return a result list
     */
    List<BeeMethodExecutionLog> getMethodExecutionLog(int type);

    /**
     * Clears logs from pool with specified type.
     *
     * @param type should be one of[BeeMethodExecutionLog.Type_Connection_Get,BeeMethodExecutionLog.Type_SQL_Execution];if not,then clear all logs
     * @return a cleared list
     */
    List<BeeMethodExecutionLog> clearMethodExecutionLog(int type);

    /**
     * Set a new log handler to pool.
     *
     * @param listener to handle method logs
     */
    void setMethodExecutionListener(BeeMethodExecutionListener listener);

    /**
     * Cancel statement in executing,this method may be support distribution network.
     *
     * @param logId log id
     * @return boolean is true that log is a statement log and success to cancellation called on this statement.
     * @throws SQLException when cancellation failed
     */
    boolean cancelStatement(Object logId) throws SQLException;
}
	
