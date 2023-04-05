/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package cn.beecp;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection pool interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionPool {

    //***************************************************************************************************************//
    //                1: pool initialize method(1)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    //initialize pool with configuration
    void init(BeeDataSourceConfig config) throws SQLException;

    //***************************************************************************************************************//
    //                2: objects methods(2)                                                                          //                                                                                  //
    //***************************************************************************************************************//
    //borrow a connection from pool
    Connection getConnection() throws SQLException;

    //borrow a connection from pool
    XAConnection getXAConnection() throws SQLException;

    //***************************************************************************************************************//
    //                3: Pool runtime maintain methods(6)                                                            //                                                                                  //
    //***************************************************************************************************************//
    //close pool
    void close();

    //check pool is whether closed
    boolean isClosed();

    //enable Runtime Log
    void setPrintRuntimeLog(boolean indicator);

    //get pool monitor vo
    BeeConnectionPoolMonitorVo getPoolMonitorVo();

    //clear all connections from pool,forceCloseUsingOnClear is true,then close using connection directly
    void clear(boolean forceCloseUsing) throws SQLException;

    //clear all connections from pool,forceCloseUsingOnClear is true,then close using connection directly
    void clear(boolean forceCloseUsing, BeeDataSourceConfig config) throws SQLException;

}
	
