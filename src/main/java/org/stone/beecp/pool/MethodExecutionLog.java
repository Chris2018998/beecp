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

import org.stone.beecp.BeeMethodExecutionLog;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Default implementation of {@link BeeMethodExecutionLog}
 *
 * @author Chris Liao
 */
public class MethodExecutionLog implements BeeMethodExecutionLog {
    //Method call is in executing
    static final int Status_Running = 0;
    //Method call is successful
    static final int Status_Successful = 1;
    //Method call is failed
    static final int Status_Failed = 2;

    //Log type
    private final int type;
    //log id
    private final Object id;

    //Method name of pool or (Statement,PreparedStatement,CallableStatement)
    private final String method;
    //Array of method parameters
    private final Object[] parameters;

    //Start time of method call,time unit:milliseconds
    private final long startTime;
    //End time of method call,time unit:milliseconds
    private long endTime;
    //End time of method call,time unit:milliseconds
    private int status = Status_Running;

    //Result object of method call
    private Object resultObject;
    //Fail exception when method call
    private Throwable failCause;

    //A prepared sql or a statement sql.
    private String sql;
    //elapsed time on prepared sql,refer to {@code connection.prepareStatement(String,...)} method or {@code connection.prepareCall(String,...)}method
    private long sqlPreparedTime;
    //a parameter array of prepared sql
    private Object[] sqlPreparedParameters;
    //if current log is a sql execution
    private transient Statement statement;


    //Flag of removed from log manager
    private boolean removed;
    //Flag of handled by Handler
    private boolean slow;

    public MethodExecutionLog(int type, String method, Object[] parameters) {
        this.type = type;
        this.method = method;
        this.parameters = parameters;
        this.id = UUID.randomUUID();
        this.startTime = System.currentTimeMillis();
    }

    public int getType() {
        return type;
    }

    public Object getId() {
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
        return this.status == Status_Running;
    }

    public boolean isSuccessful() {
        return this.status == Status_Successful;
    }

    public boolean isException() {
        return this.status == Status_Failed;
    }

    public boolean isSlow() {
        return slow;
    }

    void setAsSlow() {
        this.slow = true;
    }

    public Object getResult() {
        return resultObject;
    }

    public Throwable getFailCause() {
        return failCause;
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
            this.failCause = (Throwable) result;
            this.status = Status_Failed;
        } else {
            this.resultObject = result;
            this.status = Status_Successful;
        }
    }

    public boolean equals(Object v) {
        return (v instanceof MethodExecutionLog) && this.id.equals(((MethodExecutionLog) v).id);
    }
}
