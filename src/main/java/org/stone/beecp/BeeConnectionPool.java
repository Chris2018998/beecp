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
 * Connection pool interface.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionPool {

    /**
     * Pool initializes with a configuration object.
     *
     * @param config is a configuration object defines some items can be applied in pool
     * @throws SQLException when fail to initialize
     */
    void init(BeeDataSourceConfig config) throws SQLException;

    /**
     * Attempts to get a connection from pool.
     *
     * @return a borrowed connection
     * @throws SQLException                      when fail to create a connection
     * @throws ConnectionGetTimeoutException     when wait timeout in pool
     * @throws ConnectionGetInterruptedException while waiting is interrupted
     */
    Connection getConnection() throws SQLException;

    /**
     * Attempts to get a connection from pool.
     *
     * @param username link to database
     * @param password the user's password
     * @return a borrowed connection
     * @throws SQLException                      when fail to create a connection
     * @throws ConnectionGetTimeoutException     when wait timeout in pool
     * @throws ConnectionGetInterruptedException while waiting is interrupted
     */
    Connection getConnection(String username, String password) throws SQLException;

    /**
     * Attempts to get a XAConnection from pool.
     *
     * @return a borrowed XAConnection
     * @throws SQLException                      when fail to create a xa connection
     * @throws ConnectionGetTimeoutException     when wait timeout in pool
     * @throws ConnectionGetInterruptedException while waiting is interrupted
     */
    XAConnection getXAConnection() throws SQLException;

    /**
     * Attempts to get a XAConnection from pool.
     *
     * @param username link to database
     * @param password the user's password
     * @return a borrowed XAConnection
     * @throws SQLException                      when fail to create a xa connection
     * @throws ConnectionGetTimeoutException     when wait timeout in pool
     * @throws ConnectionGetInterruptedException while waiting is interrupted
     */
    XAConnection getXAConnection(String username, String password) throws SQLException;

    /**
     * Shutdown pool to not work(closed state),closes all maintained connections and removes them from pool.
     */
    void close();

    /**
     * Queries pool whether is closed.
     *
     * @return true that pool is closed
     */
    boolean isClosed();

    /**
     * A switch call to enable or disable logs print in pool.
     *
     * @param enable is true that print, false is not print
     */
    void setPrintRuntimeLog(boolean enable);

    /**
     * Gets runtime monitoring object of pool,refer to {@link BeeConnectionPoolMonitorVo}.
     *
     * @return monitoring object of pool
     */
    BeeConnectionPoolMonitorVo getPoolMonitorVo();

    /**
     * Interrupts connections creation in processing.
     *
     * @param onlyInterruptTimeout is true that only interrupts timeout creation,false that interrupts all creation in processing
     * @return interrupted threads
     */
    Thread[] interruptConnectionCreating(boolean onlyInterruptTimeout);

    /**
     * Physically closes all connections and removes them from pool which not accepts borrow requests before completion.
     * If exists borrowed connections,before close them,this method call to recycle them return to pool or wait them
     * return to pool with configured time value,refer to {@link BeeDataSourceConfig#getParkTimeForRetry()}.
     *
     * @param forceRecycleBorrowed is true that recycle borrowed connections immediately and roll back existing transactions
     *                             associated with them;false that wait them return to pool
     * @throws SQLException when pool is closed or in clearing
     */
    void clear(boolean forceRecycleBorrowed) throws SQLException;

    /**
     * Physically closes all connections and removes them from pool, then pool reinitialize with a new configuration.
     * If exists borrowed connections,before close them,this method call to recycle them return to pool or wait them
     * return to pool with configured time value,refer to {@link BeeDataSourceConfig#getParkTimeForRetry()}.
     *
     * @param forceRecycleBorrowed is true that recycle borrowed connections immediately and roll back existing transactions
     *                             associated with them;false that wait them return to pool
     * @param config               is a new configuration object for reinitialization
     * @throws BeeDataSourceConfigException when configuration check fail
     * @throws SQLException                 when pool is closed or in clearing
     * @throws SQLException                 when pool reinitialize fail
     */
    void clear(boolean forceRecycleBorrowed, BeeDataSourceConfig config) throws SQLException;

}
	
