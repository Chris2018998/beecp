/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import static cn.beecp.pool.PoolStaticCenter.*;

/**
 * ProxyStatementBase
 *
 * @author Chris.Liao
 * @version 1.0
 */
abstract class ProxyStatementBase extends ProxyBaseWrapper implements Statement {
    private final ProxyConnectionBase owner;
    protected Statement raw;
    boolean registered = true;
    private ProxyResultSetBase curRe;
    private ArrayList<ProxyResultSetBase> results;
    private int resultOpenCode = CLOSE_CURRENT_RESULT;

    public ProxyStatementBase(Statement raw, ProxyConnectionBase o, PooledConnection p) {
        super(p);
        o.registerStatement(this);
        this.raw = raw;
        this.owner = o;
    }

    //***************************************************************************************************************//
    //                                             Below are self-define methods                                     //
    //***************************************************************************************************************//
    void removeOpenResultSet(ProxyResultSetBase r) {//call by ProxyResultSetBase.constructor
        if (r == curRe) {
            curRe = null;
        } else if (results != null) {
            results.remove(r);
        }
    }

    void setOpenResultSet(ProxyResultSetBase r) {//call by ProxyResultSetBase.constructor
        switch (resultOpenCode) {
            case CLOSE_CURRENT_RESULT: {
                if (curRe != null && !curRe.isClosed) oclose(curRe);
                break;
            }
            case KEEP_CURRENT_RESULT: {
                if (curRe != null && !curRe.isClosed) {
                    if (results == null) results = new ArrayList<ProxyResultSetBase>(1);
                    results.add(curRe);
                }
                break;
            }
            case CLOSE_ALL_RESULTS: {
                if (curRe != null && !curRe.isClosed)
                    oclose(curRe);
                if (results != null) {
                    for (ProxyResultSetBase openRe : results)
                        if (!openRe.isClosed) oclose(openRe);
                    results.clear();
                }
                break;
            }
            default:
                break;
        }
        this.curRe = r;
    }

    //***************************************************************************************************************//
    //                                              Below are override methods                                       //
    //***************************************************************************************************************//
    public Connection getConnection() throws SQLException {
        if (isClosed) throw StatementClosedException;
        return owner;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void close() throws SQLException {
        if (isClosed) return;
        isClosed = true;
        if (curRe != null) oclose(curRe);
        if (results != null) {
            for (ProxyResultSetBase resultSetBase : results)
                oclose(resultSetBase);
            results.clear();
        }
        try {
            raw.close();
        } finally {
            raw = CLOSED_CSTM;//why? because Mysql's PreparedStatement just only remark as closed with useServerCache mode
            if (registered) owner.unregisterStatement(this);
        }
    }

    public boolean getMoreResults() throws SQLException {
        return getMoreResults(CLOSE_CURRENT_RESULT);
    }

    public boolean getMoreResults(int current) throws SQLException {
        resultOpenCode = current;
        return raw.getMoreResults(current);
    }

    public ResultSet getResultSet() throws SQLException {
        ResultSet re = raw.getResultSet();//rawConn resultSet
        if (re == null) return null;
        if (curRe != null && curRe.containsRaw(re)) return curRe;
        if (results != null) {
            for (ProxyResultSetBase resultSetBase : results) {
                if (resultSetBase.containsRaw(re)) return resultSetBase;
            }
        }
        return createProxyResultSet(re, this, p);
    }

    public void setPoolable(boolean var1) {
    }
}
