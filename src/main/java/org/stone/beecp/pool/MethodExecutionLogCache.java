/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.pool;

import org.stone.beecp.BeeMethodExecutionListener;
import org.stone.beecp.BeeMethodExecutionLog;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static org.stone.beecp.BeeMethodExecutionLog.*;

/**
 *
 * @author Chris Liao
 * @version 1.0
 */
public class MethodExecutionLogCache {
    //slow threshold value of connection get,time unit:milliseconds,refer to {@code BeeDataSourceConfig.slowConnectionGetThreshold}
    private final long slowConnectionGetThreshold;
    //slow threshold of sql execution,time unit:milliseconds,refer to {@code BeeDataSourceConfig.slowSQLExecutionThreshold}
    private final long slowSQLExecutionThreshold;
    private final int maxSize;
    //logs queue of connection get
    private final LinkedBlockingQueue<MethodExecutionLog> conLogQueue;
    //logs queue of sql execution
    private final LinkedBlockingQueue<MethodExecutionLog> sqlLogQueue;
    private BeeMethodExecutionListener logHandler;

    //***************************************************************************************************************//
    //                                         1: initialization                                                     //
    //***************************************************************************************************************//

    /**
     * initialize log manager.
     *
     * @param cacheSize  is capacity of logs cache
     * @param slowGet    is slow threshold value of connection get,time unit:milliseconds
     * @param slowExec   is slow threshold of sql execution,time unit:milliseconds
     * @param logHandler is a log handler
     */
    MethodExecutionLogCache(int cacheSize, long slowGet, long slowExec, BeeMethodExecutionListener logHandler) {
        this.logHandler = logHandler;
        this.slowConnectionGetThreshold = slowGet;
        this.slowSQLExecutionThreshold = slowExec;

        this.maxSize = cacheSize;
        this.conLogQueue = new LinkedBlockingQueue<>(cacheSize);
        this.sqlLogQueue = new LinkedBlockingQueue<>(cacheSize);
    }

    public void setMethodExecutionListener(BeeMethodExecutionListener handler) {
        this.logHandler = handler;
    }

    //***************************************************************************************************************//
    //                                         2: logs record                                                        //
    //***************************************************************************************************************//

    /**
     * Start to call a method and a log object is return this start method
     *
     * @param type       is method call type
     * @param method     is method name,for example:getConnection()
     * @param parameters is an array of method parameters
     */
    public BeeMethodExecutionLog beforeCall(int type, String method, Object[] parameters, String sql, Statement statement) throws SQLException {
        MethodExecutionLog log = new MethodExecutionLog(type, method, parameters);
        log.setStatement(statement);
        if (type != Type_SQL_Preparation) {
            if (logHandler != null) logHandler.onMethodStart(log);
            offerQueue(log, type, parameters, sql);
        }
        return log;
    }

    private void offerQueue(MethodExecutionLog log, int type, Object[] parameters, String sql) {
        if (type == Type_Connection_Get) {
            while (!conLogQueue.offer(log)) {
                if (conLogQueue.size() == this.maxSize) {
                    MethodExecutionLog other = conLogQueue.poll();
                    if (other != null) other.setRemoved(true);
                }
            }
        } else {//Type_Execute_SQL
            if (parameters == null || parameters.length == 0) {
                log.setSql(sql);
            } else {
                log.setSql((String) parameters[0]);
            }

            while (!sqlLogQueue.offer(log)) {
                if (sqlLogQueue.size() == this.maxSize) {
                    MethodExecutionLog other = sqlLogQueue.poll();
                    if (other != null) other.setRemoved(true);
                }
            }
        }
    }

    /**
     * update result info to log object
     *
     * @param callResult is result of target method call
     * @param log        generated from startCall method
     * @preparedParameters is a parameter array of PreparedSQL or CallableSQL
     */
    public void afterCall(Object callResult, long preparationTookTime, Object[] preparedParameters, BeeMethodExecutionLog log) throws SQLException {
        MethodExecutionLog defaultTypeLog = (MethodExecutionLog) log;
        defaultTypeLog.setResult(callResult, preparationTookTime, preparedParameters);

        if (log.getType() != Type_SQL_Preparation && log.isRemoved()) {
            defaultTypeLog.setRemoved(false);
            offerQueue(defaultTypeLog, log.getType(), log.getParameters(), log.getSql());
        }

        if (((Type_Connection_Get == log.getType() && log.getEndTime() - log.getStartTime() >= slowConnectionGetThreshold)
                || (Type_SQL_Execution == log.getType() && log.getEndTime() - log.getStartTime() >= slowSQLExecutionThreshold))) {
            defaultTypeLog.setAsSlow();
        }

        if (logHandler != null) logHandler.onMethodEnd(log);
    }

    //***************************************************************************************************************//
    //                                         1: Logs maintain                                                      //
    //***************************************************************************************************************//
    public List<BeeMethodExecutionLog> getLog(int type) {
        List<BeeMethodExecutionLog> logList = new LinkedList<>();
        switch (type) {
            case Type_Connection_Get: {
                logList.addAll(this.conLogQueue);
                break;
            }
            case Type_SQL_Execution: {
                logList.addAll(this.sqlLogQueue);
                break;
            }
            default: {
                logList.addAll(this.conLogQueue);
                logList.addAll(this.sqlLogQueue);
                break;
            }
        }
        return logList;
    }

    public List<BeeMethodExecutionLog> clear(int type) {
        List<BeeMethodExecutionLog> removedLogList = new LinkedList<>();
        switch (type) {
            case Type_Connection_Get: {
                conLogQueue.drainTo(removedLogList);
                break;
            }
            case Type_SQL_Execution: {
                sqlLogQueue.drainTo(removedLogList);
                break;
            }
            default: {
                conLogQueue.drainTo(removedLogList);
                sqlLogQueue.drainTo(removedLogList);
                break;
            }
        }

        for (BeeMethodExecutionLog log : removedLogList)
            ((MethodExecutionLog) log).setRemoved(true);
        return removedLogList;
    }

    /**
     * Clear timeout logs from manager.
     *
     * @param timeout to check timeout logs
     */
    public void clearTimeout(long timeout) {
        List<BeeMethodExecutionLog> longRunningLogList = new ArrayList<>(1);
        List<BeeMethodExecutionLog> conPendingRemovalLogList = new LinkedList<>();
        List<BeeMethodExecutionLog> sqlPendingRemovalLogList = new LinkedList<>();
        long currentTime = System.currentTimeMillis();

        //1: scan log list to find out all timeout logs to be removed
        for (MethodExecutionLog log : conLogQueue) {
            if (currentTime - log.getStartTime() >= timeout) {
                conPendingRemovalLogList.add(log);
            }

            if (log.getEndTime() == 0 && currentTime - log.getStartTime() >= slowConnectionGetThreshold) {
                log.setAsSlow();
                longRunningLogList.add(log);
            }
        }

        //2: timeout check on sql execution logs
        for (MethodExecutionLog log : sqlLogQueue) {
            if (currentTime - log.getStartTime() >= timeout) {
                sqlPendingRemovalLogList.add(log);
            }

            if (log.getEndTime() == 0 && currentTime - log.getStartTime() >= slowSQLExecutionThreshold) {
                log.setAsSlow();
                longRunningLogList.add(log);
            }
        }

        //3: remove timeout logs from connection log list
        if (!conPendingRemovalLogList.isEmpty()) {
            conLogQueue.removeAll(conPendingRemovalLogList);
            for (BeeMethodExecutionLog log : conPendingRemovalLogList) {
                ((MethodExecutionLog) log).setRemoved(true);
            }
        }

        //4: remove timeout logs from sql execution log list
        if (!sqlPendingRemovalLogList.isEmpty()) {
            sqlLogQueue.removeAll(sqlPendingRemovalLogList);
            for (BeeMethodExecutionLog log : sqlPendingRemovalLogList) {
                ((MethodExecutionLog) log).setRemoved(true);
            }
        }

        //5: handle slow log list
        if (logHandler != null) {
            try {
                logHandler.onLongRunningDetected(longRunningLogList);
            } catch (Throwable e) {
                //
            }
        }
    }

    /**
     * Cancel statement in executing
     *
     * @param logId is an id of statement log cached in manager.
     */
    public boolean cancelStatement(Object logId) throws SQLException {
        if (logId == null) return false;
        for (BeeMethodExecutionLog log : sqlLogQueue) {
            if (logId.equals(log.getId())) {
                return log.cancelStatement();
            }
        }
        return false;
    }
}
