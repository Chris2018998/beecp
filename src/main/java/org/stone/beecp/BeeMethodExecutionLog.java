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
public interface BeeMethodExecutionLog extends Serializable {
    //All logs
    int Type_All = 0;
    //Log type represent method call that connection get from pool
    int Type_Connection_Get = 1;
    //Log type represent method call that sql preparation on pooled connections
    int Type_SQL_Preparation = 2;
    //Log type represent method call that sql execution on (Statement,PreparedStatement,CallableStatement)
    int Type_SQL_Execution = 3;


    /**
     * Get pool name of current log
     *
     * @return pool name
     */
    String getPoolName();

    /**
     * Get log type.
     *
     * @return type value,which is one of [Type_Connection_Get,Type_SQL_Execution]
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
     * Get fail cause of method call,this cause may be null.
     *
     * @return a result object
     */
    Throwable getFailCause();

    /**
     * Query log is whether removed from log manager.
     *
     * @return a boolean,true is removed,false is the log is still in log manager
     */
    boolean isRemoved();

    //***************************************************************************************************************//
    //                                         4: SQL Execution                                                      //
    //***************************************************************************************************************//

    /**
     * Get execution sql
     *
     * @return log sql,return null if log type is not {@link #Type_SQL_Execution}.
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
     * cancel a executing statement
     *
     * @return boolean is true that log is a statement log exist in manager and success to cancellation called on this statement.
     * @throws SQLException when failed to cancel statement
     */
    boolean cancelStatement() throws SQLException;
}
