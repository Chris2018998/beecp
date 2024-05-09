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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import static org.stone.beecp.pool.ConnectionPoolStatics.*;

/**
 * ProxyStatementBase
 *
 * @author Chris Liao
 * @version 1.0
 */
abstract class ProxyStatementBase extends ProxyBaseWrapper implements Statement {
    private final ProxyConnectionBase owner;
    protected Statement raw;
    boolean registered = true;
    private ProxyResultSetBase curRe;
    private ArrayList<ProxyResultSetBase> results;
    private int resultOpenCode = Statement.CLOSE_CURRENT_RESULT;

    public ProxyStatementBase(Statement raw, ProxyConnectionBase o, PooledConnection p) {
        super(p);
        this.raw = raw;
        this.owner = o;
        owner.registerStatement(this);
    }

    //***************************************************************************************************************//
    //                                             Below are self-define methods                                     //
    //***************************************************************************************************************//
    void removeOpenResultSet(ProxyResultSetBase r) {//call by ProxyResultSetBase.constructor
        if (r == this.curRe) {
            this.curRe = null;
        } else if (this.results != null) {
            this.results.remove(r);
        }
    }

    void setOpenResultSet(ProxyResultSetBase r) {//call by ProxyResultSetBase.constructor
        switch (this.resultOpenCode) {
            case Statement.CLOSE_CURRENT_RESULT: {
                if (this.curRe != null && !this.curRe.isClosed) oclose(this.curRe);
                break;
            }
            case Statement.KEEP_CURRENT_RESULT: {
                if (this.curRe != null && !this.curRe.isClosed) {
                    if (this.results == null) this.results = new ArrayList<ProxyResultSetBase>(1);
                    this.results.add(this.curRe);
                }
                break;
            }
            case Statement.CLOSE_ALL_RESULTS: {
                if (this.curRe != null && !this.curRe.isClosed)
                    oclose(this.curRe);
                if (this.results != null) {
                    for (ProxyResultSetBase openRe : this.results)
                        if (!openRe.isClosed) oclose(openRe);
                    this.results.clear();
                }
                break;
            }
            default:
                break;
        }
        curRe = r;
    }

    //***************************************************************************************************************//
    //                                              Below are override methods                                       //
    //***************************************************************************************************************//
    public Connection getConnection() throws SQLException {
        if (this.isClosed) throw new SQLException("No operations allowed after statement closed");
        return this.owner;
    }

    public boolean isClosed() {
        return this.isClosed;
    }

    public final void close() throws SQLException {
        if (this.isClosed) return;
        this.isClosed = true;
        if (this.curRe != null) oclose(this.curRe);
        if (this.results != null) {
            for (ProxyResultSetBase resultSetBase : this.results)
                oclose(resultSetBase);
            this.results.clear();
        }
        try {
            this.raw.close();
        } finally {
            this.raw = CLOSED_CSTM;//why? because Mysql's PreparedStatement just only remark as closed with useServerCache mode
            if (this.registered) this.owner.unregisterStatement(this);
        }
    }

    public boolean getMoreResults() throws SQLException {
        return this.getMoreResults(Statement.CLOSE_CURRENT_RESULT);
    }

    public boolean getMoreResults(int current) throws SQLException {
        this.resultOpenCode = current;
        return this.raw.getMoreResults(current);
    }

    public ResultSet getResultSet() throws SQLException {
        ResultSet re = this.raw.getResultSet();//rawConn resultSet
        if (re == null) return null;
        if (this.curRe != null && this.curRe.containsRaw(re)) return this.curRe;
        if (this.results != null) {
            for (ProxyResultSetBase resultSetBase : this.results) {
                if (resultSetBase.containsRaw(re)) return resultSetBase;
            }
        }
        return createProxyResultSet(re, this, this.p);
    }

    public void setPoolable(boolean var1) {
        //do nothing
    }

    public void closeOnCompletion() {
        //do nothing
    }

    public boolean isCloseOnCompletion() {
        return false;
    }

    public String toString() {
        return raw.toString();
    }
}
