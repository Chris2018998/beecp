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

import org.stone.beecp.BeeMethodLog;
import org.stone.beecp.BeeMethodLogListener;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static org.stone.beecp.BeeMethodLog.*;

/**
 * Method logs cache.
 *
 * @author Chris Liao
 * @version 1.0
 */
final class MethodExecutionLogCache {
    //pool name
    private String poolName;
    //cache size
    private int maxSize;
    //slow threshold value of connection get,time unit:milliseconds,refer to {@code BeeDataSourceConfig.slowConnectionGetThreshold}
    private long slowConnectionThreshold;
    //slow threshold of sql execution,time unit:milliseconds,refer to {@code BeeDataSourceConfig.slowSQLExecutionThreshold}
    private long slowSQLThreshold;

    //queue to store logs of connections getting
    private LinkedBlockingQueue<MethodExecutionLog> connectionGetLogsQueue;
    //queue to store logs of sql preparation and sql execution
    private LinkedBlockingQueue<MethodExecutionLog> sqlExecutionLogsQueue;

    private BeeMethodLogListener listener;

    //***************************************************************************************************************//
    //                                         1: initialization                                                     //
    //***************************************************************************************************************//

    /**
     * initialize log cache.
     *
     * @param cacheSize is capacity of logs cache
     * @param slowGet   is slow threshold value of connection get,time unit:milliseconds
     * @param slowExec  is slow threshold of sql execution,time unit:milliseconds
     * @param listener  is an execution listener
     */
    void init(String poolName, int cacheSize, long slowGet, long slowExec, BeeMethodLogListener listener) {
        this.poolName = poolName;
        this.listener = listener;
        this.slowConnectionThreshold = slowGet;
        this.slowSQLThreshold = slowExec;

        this.maxSize = cacheSize;
        this.connectionGetLogsQueue = new LinkedBlockingQueue<>(cacheSize);
        this.sqlExecutionLogsQueue = new LinkedBlockingQueue<>(cacheSize);
    }

    public void setMethodExecutionListener(BeeMethodLogListener handler) {
        this.listener = handler;
    }

    //***************************************************************************************************************//
    //                                         2: logs record                                                        //
    //***************************************************************************************************************//
    public BeeMethodLog beforeCall(int type, String method, Object[] parameters, String sql, Statement statement) throws SQLException {
        MethodExecutionLog log = new MethodExecutionLog(poolName, type, method, parameters, statement);
        this.offerQueue(log, type, parameters, sql);
        if (listener != null) listener.onMethodStart(log);//can put sql check,sql parse....
        return log;
    }

    private void offerQueue(MethodExecutionLog log, int type, Object[] parameters, String sql) {
        LinkedBlockingQueue<MethodExecutionLog> queue;
        if (type == Type_Pool_Log) {//connection logs
            queue = connectionGetLogsQueue;
        } else {//sql execution logs
            queue = sqlExecutionLogsQueue;
            if (parameters == null || parameters.length == 0) {
                log.setSql(sql);
            } else {
                log.setSql((String) parameters[0]);
            }
        }

        while (!queue.offer(log)) {
            if (queue.size() == this.maxSize) {
                MethodExecutionLog firstLog = queue.poll();
                if (firstLog != null) firstLog.setRemoved(true);
            }
        }
    }

    public void afterCall(Object callResult, long preparationTookTime, Object[] preparedParameters, BeeMethodLog log) throws SQLException {
        MethodExecutionLog defaultTypeLog = (MethodExecutionLog) log;
        defaultTypeLog.setResult(callResult, preparationTookTime, preparedParameters);
        int logType = defaultTypeLog.getType();

        if (defaultTypeLog.isRemoved()) {
            defaultTypeLog.setRemoved(false);
            offerQueue(defaultTypeLog, logType, defaultTypeLog.getParameters(), defaultTypeLog.getSql());
        }

        defaultTypeLog.setAsSlow(0L, Type_Pool_Log == logType ? this.slowConnectionThreshold : this.slowSQLThreshold);
        if (listener != null) listener.onMethodEnd(log);
    }

    //***************************************************************************************************************//
    //                                         1: Logs maintain                                                      //
    //***************************************************************************************************************//
    public List<BeeMethodLog> getLog(int type) {
        List<BeeMethodLog> logList = new LinkedList<>();
        switch (type) {
            case Type_Pool_Log: {
                logList.addAll(this.connectionGetLogsQueue);
                break;
            }
            case Type_Connection_Log: {
                for (BeeMethodLog log : this.sqlExecutionLogsQueue) {
                    if (log.getType() == Type_Connection_Log)
                        logList.add(log);
                }
                break;
            }
            case Type_Statement_Log: {
                logList.addAll(this.sqlExecutionLogsQueue);
                break;
            }
            default: {
                logList.addAll(this.connectionGetLogsQueue);
                logList.addAll(this.sqlExecutionLogsQueue);
                break;
            }
        }
        return logList;
    }

    public void clear(int type) {
        List<BeeMethodLog> removedLogList = new LinkedList<>();
        switch (type) {
            case Type_Pool_Log: {
                connectionGetLogsQueue.drainTo(removedLogList);
                break;
            }
            case Type_Connection_Log: {
                for (BeeMethodLog log : sqlExecutionLogsQueue) {
                    if (log.getType() == Type_Connection_Log) {
                        removedLogList.add(log);
                    }
                }

                if (!removedLogList.isEmpty())
                    sqlExecutionLogsQueue.removeAll(removedLogList);
                break;
            }
            case Type_Statement_Log: {
                sqlExecutionLogsQueue.drainTo(removedLogList);
                break;
            }
            default: {
                connectionGetLogsQueue.drainTo(removedLogList);
                sqlExecutionLogsQueue.drainTo(removedLogList);
                break;
            }
        }

        for (BeeMethodLog log : removedLogList)
            ((MethodExecutionLog) log).setRemoved(true);
    }


    public void clearTimeout(long timeout) {
        List<BeeMethodLog> longRunningLogList = new ArrayList<>(1);
        List<BeeMethodLog> conPendingRemovalLogList = new LinkedList<>();
        List<BeeMethodLog> sqlPendingRemovalLogList = new LinkedList<>();
        long currentTime = System.currentTimeMillis();

        //1: scan log list to find out all timeout logs to be removed
        for (MethodExecutionLog log : connectionGetLogsQueue) {
            if (currentTime - log.getStartTime() - timeout >= 0L) {//timeout
                conPendingRemovalLogList.add(log);
            }

            log.setAsSlow(currentTime, this.slowConnectionThreshold);
            if (log.isLongRunning() && !log.hasHandledByListener()) {
                longRunningLogList.add(log);
            }
        }

        //2: timeout check on sql execution logs
        for (MethodExecutionLog log : sqlExecutionLogsQueue) {
            if (currentTime - log.getStartTime() - timeout >= 0L) {
                sqlPendingRemovalLogList.add(log);
                if (log.getEndTime() == 0L) conPendingRemovalLogList.add(log);
            }

            log.setAsSlow(currentTime, this.slowSQLThreshold);
            if (log.isLongRunning() && !log.hasHandledByListener()) {
                longRunningLogList.add(log);
            }
        }

        //3: remove timeout logs from connection log list
        if (!conPendingRemovalLogList.isEmpty()) {
            connectionGetLogsQueue.removeAll(conPendingRemovalLogList);
            for (BeeMethodLog log : conPendingRemovalLogList) {
                ((MethodExecutionLog) log).setRemoved(true);
            }
        }

        //4: remove timeout logs from sql execution log list
        if (!sqlPendingRemovalLogList.isEmpty()) {
            sqlExecutionLogsQueue.removeAll(sqlPendingRemovalLogList);
            for (BeeMethodLog log : sqlPendingRemovalLogList) {
                ((MethodExecutionLog) log).setRemoved(true);
            }
        }

        //5: handle slow log list
        if (!longRunningLogList.isEmpty() && listener != null) {
            try {
                List<Boolean> processFlags = listener.onLongRunningDetected(longRunningLogList);
                if (processFlags != null && !processFlags.isEmpty()) {
                    for (int i = 0, l = processFlags.size(); i < l; i++) {
                        ((MethodExecutionLog) longRunningLogList.get(i)).setHandled(processFlags.get(i).booleanValue());
                    }
                }
            } catch (Throwable e) {
                //log.error();
            }
        }
    }

    public boolean cancelStatement(Object logId) throws SQLException {
        if (logId == null) return false;
        for (BeeMethodLog log : sqlExecutionLogsQueue) {
            if (logId.equals(log.getId())) {
                return log.cancelStatement();
            }
        }
        return false;
    }
}
