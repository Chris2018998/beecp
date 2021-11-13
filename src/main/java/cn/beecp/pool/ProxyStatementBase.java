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
abstract class ProxyStatementBase implements Statement {
    protected final PooledConnection p;//called by subclass to update time
    private final ProxyConnectionBase owner;
    protected Statement raw;
    boolean registered = true;
    private boolean isClosed;
    private ProxyResultSetBase curRe;
    private ArrayList<ProxyResultSetBase> results;
    private int resultOpenCode = CLOSE_CURRENT_RESULT;

    public ProxyStatementBase(final Statement raw, final ProxyConnectionBase o, final PooledConnection p) {
        o.registerStatement(this);
        this.raw = raw;
        this.owner = o;
        this.p = p;
    }

    /*******************************************************************************************
     *                                                                                         *
     *                         Below are self define methods                                          *
     *                                                                                         *
     *******************************************************************************************/

    final void removeOpenResultSet(final ProxyResultSetBase r) {//call by ProxyResultSetBase.constructor
        if (r == curRe) {
            curRe = null;
        } else if (results != null) {
            results.remove(r);
        }
    }

    final void setOpenResultSet(final ProxyResultSetBase r) {//call by ProxyResultSetBase.constructor
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
                    for (int i = 0, l = results.size(); i < l; i++) {
                        ProxyResultSetBase openRe = results.get(i);
                        if (!openRe.isClosed) oclose(openRe);
                    }
                    results.clear();
                }
                break;
            }
            default:
                break;
        }
        this.curRe = r;
    }

    /******************************************************************************************
     *                                                                                        *
     *                        Below are override methods                                      *
     *                                                                                        *
     ******************************************************************************************/

    public Connection getConnection() throws SQLException {
        if (isClosed) throw ConnectionClosedException;
        return owner;
    }

    public final boolean isClosed() throws SQLException {
        return isClosed;
    }

    public final void close() throws SQLException {
        if (isClosed) return;
        isClosed = true;
        if (curRe != null) oclose(curRe);
        if (results != null) {
            for (int i = 0, l = results.size(); i < l; i++)
                oclose(results.get(i));
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
        ResultSet re = raw.getResultSet();//raw resultSet
        if (re == null) return null;
        if (curRe != null && curRe.containsRaw(re)) return curRe;
        if (results != null) {
            for (ProxyResultSetBase resultSetBase : results) {
                if (resultSetBase.containsRaw(re)) return resultSetBase;
            }
        }
        return createProxyResultSet(re, this, p);
    }

    public void setPoolable(boolean var1) throws SQLException {
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this))
            return (T) this;
        else
            throw new SQLException("Wrapped object is not an instance of " + iface);
    }
}
