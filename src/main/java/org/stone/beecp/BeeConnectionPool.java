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

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * A container interface on maintaining pooled JDBC connection objects which can be borrowed out.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionPool {

    /**
     * Pool initializes on startup.
     *
     * @param config is a configuration object for pool initialization
     * @throws SQLException if initializes failed
     */
    void init(BeeDataSourceConfig config) throws SQLException;

    /**
     * Borrows a connection from pool,if no idle,borrower thread wait in pool until get one or timeout,the max wait
     * time(milliseconds) is a configurable item,refer to {@link BeeDataSourceConfig#getMaxWait} method.
     *
     * @return a borrowed connection
     * @throws SQLException when failed to create a new connection
     * @throws SQLException when timeout on wait
     * @throws SQLException when interruption on wait
     */
    Connection getConnection() throws SQLException;

    /**
     * Borrows a XA connection from pool,if no idle,borrower thread wait in pool until get one or timeout,the max wait
     * time(milliseconds) is a configurable item,refer to {@link BeeDataSourceConfig#getMaxWait} method.
     *
     * @return a borrowed connection
     * @throws SQLException when failed to create a new connection
     * @throws SQLException when timeout on wait
     * @throws SQLException when interruption on wait
     */
    XAConnection getXAConnection() throws SQLException;

    /**
     * This invocation cause pool to stop work,all connections are closed and removed,pool state marked as closed value
     * when completion and all operations on pool are disabled.
     */
    void close();

    /**
     * Gets pool status whether in closed
     *
     * @return a boolean value of pool close status
     */
    boolean isClosed();

    /**
     * A switch method on runtime logs print.
     *
     * @param indicator is true,pool prints runtime logs,when false,not print
     */
    void setPrintRuntimeLog(boolean indicator);

    /**
     * Gets pool monitor object contains pool runtime info,for example:pool state,idle count,using count
     *
     * @return monitor vo
     */
    BeeConnectionPoolMonitorVo getPoolMonitorVo();

    /**
     * Gets lock hold time on connection creation in a thread.
     *
     * @return lock hold time on creation
     */
    long getElapsedTimeSinceCreationLock();

    /**
     * Interrupts all threads on connection creation lock,include wait threads and lock owner thread.
     */
    void interruptThreadsOnCreationLock();

    /**
     * Closes all connections and removes them from pool.
     *
     * @param forceCloseUsing is true,connections in using are closed directly;is false,they are closed when return to pool
     */
    void clear(boolean forceCloseUsing);

    /**
     * Closes all connections and removes them from pool,then try to do reinitialization on pool with a new configuration object.
     *
     * @param forceCloseUsing is true,connections in using are closed directly;is false,they are closed when return to pool
     * @param config          is a configuration object for pool reinitialize
     * @throws BeeDataSourceConfigException when config is null
     * @throws SQLException                 when pool reinitialize failed
     */
    void clear(boolean forceCloseUsing, BeeDataSourceConfig config) throws SQLException;

}
	
