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
     * @param config is a configuration object defines some items applied in pool
     * @throws SQLException when initializes fail
     */
    void init(BeeDataSourceConfig config) throws SQLException;

    /**
     * Attempts to get a connection from pool.
     *
     * @return a borrowed connection
     * @throws SQLException                      when fail to create a connection
     * @throws ConnectionGetTimeoutException     when wait time out in pool
     * @throws ConnectionGetInterruptedException while waiting interrupted
     */
    Connection getConnection() throws SQLException;

    /**
     * Attempts to get a XAConnection from pool.
     *
     * @return a borrowed XAConnection
     * @throws SQLException                      when fail to create a xa connection
     * @throws ConnectionGetTimeoutException     when wait time out in pool
     * @throws ConnectionGetInterruptedException while waiting interrupted
     */
    XAConnection getXAConnection() throws SQLException;


    /**
     * Shutdown pool and make it to be in closed state,all pooled connections are physically closed and removed from pool.
     */
    void close();

    /**
     * Query pool state whether is closed.
     *
     * @return true that pool is closed
     */
    boolean isClosed();

    /**
     * Enable or disable log print in pool.
     *
     * @param enable is true that print, false is not print
     */
    void setPrintRuntimeLog(boolean enable);

    /**
     * Get monitoring object bring out some runtime info of pool,refer to {@link BeeConnectionPoolMonitorVo}
     *
     * @return monitor of pool
     */
    BeeConnectionPoolMonitorVo getPoolMonitorVo();

    /**
     * Interrupts processing of connections creation.
     *
     * @param onlyInterruptTimeout is true that only interrupt timeout creation,false that interrupt all creation in processing
     * @return interrupted threads
     */
    Thread[] interruptConnectionCreating(boolean onlyInterruptTimeout);

    /**
     * Physically close all pooled connections and remove them from pool,not accept requests before completion of this operation call.
     *
     * @param forceRecycleBorrowed is that recycle borrowed connections immediately,false that wait borrowed connections released by callers
     * @throws SQLException when pool closed or in cleaning
     */
    void clear(boolean forceRecycleBorrowed) throws SQLException;

    /**
     * Physically close all pooled connections and remove them from pool,then pool reinitialize with new configuration,not accept
     * requests before completion of this operation call.
     *
     * @param forceRecycleBorrowed is that recycle borrowed connections immediately,false that wait borrowed connections released by callers
     * @param config               is a new configuration object for pool reinitialization
     * @throws BeeDataSourceConfigException when configuration check fail
     * @throws SQLException                 when pool is closed or in cleaning
     * @throws SQLException                 when pool reinitialize fail
     */
    void clear(boolean forceRecycleBorrowed, BeeDataSourceConfig config) throws SQLException;

}
	
