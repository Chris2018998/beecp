/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp.pool;

import cn.beecp.BeeDataSourceConfig;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection pool interface
 *
 * @author Chris.Liao
 * @version 1.0
 */
public interface ConnectionPool {

    /**
     * initialize pool with configuration
     *
     * @param config data source configuration
     * @throws SQLException check configuration fail or to create initiated connection
     */
    void init(BeeDataSourceConfig config) throws SQLException;

    /**
     * enable Runtime Log
     *
     * @param indicator indicator,whether print pool runtime info
     * @throws SQLException
     */
    void setEnableRuntimeLog(boolean indicator);

    /**
     * borrow a connection from pool
     *
     * @return If exists idle connection in pool,then return one;if not, waiting until other borrower release
     * @throws SQLException if pool is closed or waiting timeout,then throw exception
     */
    Connection getConnection() throws SQLException;

    /**
     * Connection return to pool after it end use,if exist waiter in pool,
     * then try to transfer the connection to one waiting borrower
     *
     * @param pCon target connection need release
     */
    public void recycle(PooledConnection pCon);

    /**
     * close pool
     *
     * @throws SQLException if fail to close
     */
    void close() throws SQLException;

    /**
     * check pool is closed
     *
     * @return true, closed, false active
     */
    boolean isClosed();

    /**
     * @return Pool Monitor Vo
     */
    ConnectionPoolMonitorVo getMonitorVo();

    /**
     * Clear all connections from pool
     */
    void clearAllConnections();

    /**
     * Clear all connections from pool
     *
     * @param forceCloseUsingOnClear close using connection directly
     */
    void clearAllConnections(boolean forceCloseUsingOnClear);

}
	
