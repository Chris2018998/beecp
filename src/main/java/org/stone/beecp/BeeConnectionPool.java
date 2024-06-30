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
     * @throws SQLException while initializing failed
     */
    void init(BeeDataSourceConfig config) throws SQLException;

    /**
     * Attempts to borrow a connection from pool,if not get one,then wait in pool for a released one util timeout or
     * interrupted.
     *
     * @return a borrowed connection
     * @throws SQLException when failed to create a new connection
     * @throws SQLException when timeout on wait
     * @throws SQLException when interruption on wait
     */
    Connection getConnection() throws SQLException;

    /**
     * Attempts to borrow a connection from pool,if not get one,then wait in pool for a released one util timeout or
     * interrupted.
     *
     * @return a borrowed connection
     * @throws SQLException when failed to create a new connection
     * @throws SQLException when timeout on wait
     * @throws SQLException when interruption on wait
     */
    XAConnection getXAConnection() throws SQLException;

    /**
     * This invocation cause pool to stop work,close all connections and removes them,pool state marked as closed value
     * when completion and all operations on pool are disabled.
     */
    void close();

    /**
     * Gets pool status whether in closed.
     *
     * @return a boolean value of pool close status
     */
    boolean isClosed();

    /**
     * A switch method on runtime logs print.
     *
     * @param indicator is true,pool prints runtime logs；false,not print
     */
    void setPrintRuntimeLog(boolean indicator);

    /**
     * Gets pool monitor object contains pool runtime info,for example:pool state,idle count,using count
     *
     * @return monitor vo
     */
    BeeConnectionPoolMonitorVo getPoolMonitorVo();

    /**
     * Get start time in creating a connection in pool,timeunit:milliseconds
     *
     * @return start time of creation
     */
    long getCreatingTime();

    /**
     * checks current creation timeout,refer to {@link #getCreatingTime()}
     *
     * @return an indicator of timeout,true or false
     */
    boolean isCreatingTimeout();

    /**
     * interrupt creation thread of connection and all waiters to create connections
     *
     * @return interrupted threads
     */
    Thread[] interruptOnCreation();

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
	
