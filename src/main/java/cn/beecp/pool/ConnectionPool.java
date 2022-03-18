/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool;

import cn.beecp.BeeDataSourceConfig;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection pool interface
 *
 * @author Chris.Liao
 * @version 1.0
 */
public interface ConnectionPool {

    //initialize pool with configuration
    void init(BeeDataSourceConfig config) throws SQLException;

    //borrow a connection from pool
    Connection getConnection() throws SQLException;

    //borrow a connection from pool
    XAConnection getXAConnection() throws SQLException;

    //recycle one pooled Connection
    void recycle(PooledConnection p);

    //close pool
    void close();

    //remove all pooled connections,if exists using connections,then wait util them idle,and close them and remove
    void clear();

    //clear all connections from pool,forceCloseUsingOnClear is true,then close using connection directly
    void clear(boolean forceCloseUsingOnClear);

    //check pool is closed
    boolean isClosed();

    //enable Runtime Log
    void setPrintRuntimeLog(boolean indicator);

    //get pool monitor vo
    ConnectionPoolMonitorVo getPoolMonitorVo();

}
	
