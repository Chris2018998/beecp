/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp;

import java.io.Serializable;
import java.sql.SQLException;

/**
 * Method execution log interface.
 *
 * @author Chris Liao
 */
public interface BeeMethodLog extends Serializable {
    int Type_All = 0;
    int Type_Pool_Log = 1;
    int Type_Connection_Log = 2;
    int Type_Statement_Log = 3;

    /**
     * Get pool name of current log
     *
     * @return pool name
     */
    String getPoolName();

    /**
     * Get log type.
     *
     * @return type value,which is one of [Pool_Logs,Connection_Logs,Statement_Logs]
     */
    int getType();

    /**
     * Get Log id.
     *
     * @return log id
     */
    String getId();

    /**
     * Get method name of method call
     *
     * @return method name of method
     */
    String getMethod();

    /**
     * Get method parameter values,which may be null.
     *
     * @return method name of method
     */
    Object[] getParameters();

    /**
     * Get start time of method call.
     *
     * @return start time point,which is milliseconds
     */
    long getStartTime();

    /**
     * Get end time of method call.
     *
     * @return end time point,which is milliseconds
     */
    long getEndTime();


    //***************************************************************************************************************//
    //                                         2: Status                                                             //
    //***************************************************************************************************************//

    /**
     * Query log owner is whether running.
     *
     * @return a boolean,true is running
     */
    boolean isRunning();

    /**
     * Query log owner is successful to end call.
     *
     * @return a boolean,true is slow
     */
    boolean isSuccessful();

    /**
     * Query log owner is failed to call.
     *
     * @return a boolean,true is exception
     */
    boolean isException();

    /**
     * Query log is whether slow.
     *
     * @return a boolean,true is slow
     */
    boolean isSlow();

    /**
     * Query log is long-running.
     *
     * @return a boolean,true is slow
     */
    boolean isLongRunning();

    /**
     * Query log is whether handled by listener.
     *
     * @return a boolean,true is that log has been handled
     */
    boolean hasHandledByListener();

    /**
     * Query log is whether removed from log cache.
     *
     * @return a boolean,true is removed,false is the log is still in log cache
     */
    boolean isRemoved();

    //***************************************************************************************************************//
    //                                         3: Result                                                             //
    //***************************************************************************************************************//

    /**
     * Get result of method call,this result may be null.
     *
     * @return a result object
     */
    Object getResult();

    /**
     * Get failure cause of method call,this cause may be null.
     *
     * @return a result object
     */
    Throwable getFailureCause();

    //***************************************************************************************************************//
    //                                         4: SQL Execution                                                      //
    //***************************************************************************************************************//

    /**
     * Get execution sql.
     *
     * @return log sql,return null if log type is not {@link #Type_Statement_Log}.
     */
    String getSql();

    /**
     * Get elapsed time on sql preparation.
     *
     * @return elapsed time;return 0 if sql is not from connection preparation
     */
    long getSqlPreparedTime();

    /**
     * Get parameter values of preparation sql.
     *
     * @return array of parameter values;return null if sql is not from connection preparation
     */
    Object[] getSqlPreparedParameters();

    /**
     * Cancel a executing statement.
     *
     * @return boolean is true that log is a statement log exist in cache and success to cancellation called on this statement.
     * @throws SQLException when failed to cancel statement
     */
    boolean cancelStatement() throws SQLException;
}
