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
     * borrow a connection from pool
     *
     * @return If exists idle connection in pool,then return one;if not, waiting until other borrower release
     * @throws SQLException if pool is closed or waiting timeout,then throw exception
     */
    Connection getConnection() throws SQLException;

    /**
     * return connection to pool after used
     *
     * @param pConn target connection need release
     */
    void recycle(PooledConnection pConn);

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
    public void clearAllConnections();

    /**
     * Clear all connections from pool
     *
     * @param forceCloseUsingOnClear close using connection directly
     */
    public void clearAllConnections(boolean forceCloseUsingOnClear);

}
	
