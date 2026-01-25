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

import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Default implementation of {@link BeeMethodLog}
 *
 * @author Chris Liao
 */
public class MethodExecutionLog implements BeeMethodLog {
    //Method call is in executing
    static final int State_Running = 0;
    //Method call is successful
    static final int State_Successful = 1;
    //Method call is failed
    static final int State_Failed = 2;

    //Log type
    private final int type;
    //log id
    private final String id;
    //pool name
    private final String poolName;

    //Method name of pool or (Statement,PreparedStatement,CallableStatement)
    private final String method;
    //Array of method parameters
    private final Object[] parameters;

    //Start time of method call,time unit:milliseconds
    private final long startTime;
    //End time of method call,time unit:milliseconds
    private long endTime;
    //End time of method call,time unit:milliseconds
    private int status = State_Running;

    //Result object of method call
    private transient Object resultObject;
    //Fail exception when method call
    private Throwable failureCause;

    //A prepared sql or a statement sql.
    private String sql;
    //elapsed time on prepared sql,refer to {@code connection.prepareStatement(String,...)} method or {@code connection.prepareCall(String,...)}method
    private long sqlPreparedTime;
    //a parameter array of prepared sql
    private Object[] sqlPreparedParameters;
    //if current log is a sql execution
    private transient Statement statement;

    //Removed flag
    private boolean removed;
    //Slow flag
    private boolean slow;
    //Long-running flag
    private boolean longRunning;
    //handled flag
    private boolean handled;

    public MethodExecutionLog(String poolName, int type, String method, Object[] parameters, Statement statement) {
        this.poolName = poolName;
        this.type = type;
        this.method = method;
        this.parameters = parameters;
        this.statement = statement;
        this.id = UUID.randomUUID().toString();
        this.startTime = System.currentTimeMillis();
    }

    public String getPoolName() {
        return this.poolName;
    }

    public int getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getMethod() {
        return method;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public boolean isRunning() {
        return this.status == State_Running;
    }

    public boolean isSuccessful() {
        return this.status == State_Successful;
    }

    public boolean isException() {
        return this.status == State_Failed;
    }

    public boolean isSlow() {
        return slow;
    }

    public boolean hasHandledByListener() {
        return handled;
    }

    void setHandled(boolean isHandled) {
        this.handled = isHandled;
    }

    public boolean isLongRunning() {
        return this.longRunning;
    }

    void setAsSlow(long curTime, long slowThreshold) {
        if (this.endTime != 0L) {
            this.slow = this.endTime - this.startTime - slowThreshold >= 0L;
        } else {
            this.slow = this.longRunning = curTime - this.startTime - slowThreshold >= 0L;
        }
    }

    public Object getResult() {
        return resultObject;
    }

    public Throwable getFailureCause() {
        return failureCause;
    }

    public String getSql() {
        return sql;
    }

    void setSql(String sql) {
        this.sql = sql;
    }

    public long getSqlPreparedTime() {
        return sqlPreparedTime;
    }

    public Object[] getSqlPreparedParameters() {
        return sqlPreparedParameters;
    }

    public boolean cancelStatement() throws SQLException {
        if (this.statement != null) {
            statement.cancel();
            return true;
        } else {
            return false;
        }
    }

    public boolean isRemoved() {
        return removed;
    }

    void setRemoved(boolean removed) {
        this.removed = removed;
    }

    void setStatement(Statement statement) {
        this.statement = statement;
    }

    void setResult(Object result, long sqlPreparedTime, Object[] sqlPreparedParameters) {
        this.sqlPreparedTime = sqlPreparedTime;
        this.sqlPreparedParameters = sqlPreparedParameters;
        this.statement = null;
        this.endTime = System.currentTimeMillis();
        if (result instanceof Throwable) {
            this.failureCause = (Throwable) result;
            this.status = State_Failed;
        } else {
            this.resultObject = result;
            this.status = State_Successful;
        }
    }

    public boolean equals(Object v) {
        return (v instanceof MethodExecutionLog) && this.id.equals(((MethodExecutionLog) v).id);
    }
}
