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

import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.pool.exception.TestSqlExecFailedException;

import javax.sql.CommonDataSource;
import javax.sql.XAConnection;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;

import static org.stone.tools.BeanUtil.CommonLog;

/**
 * Pool Static Center
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ConnectionPoolStatics {
    //transaction manager jndi name in configuration
    public static final String CONFIG_TM_JNDI = "transactionManagerName";
    //connect properties for driver or driver dataSource
    public static final String CONFIG_CONNECT_PROP = "connectProperties";
    //connect properties count for driver or driver dataSource
    public static final String CONFIG_CONNECT_PROP_SIZE = "connectProperties.size";
    //connect properties prefix for driver or driver dataSource
    public static final String CONFIG_CONNECT_PROP_KEY_PREFIX = "connectProperties.";
    //sql exception fatal code
    public static final String CONFIG_SQL_EXCEPTION_CODE = "sqlExceptionCodeList";
    //sql exception fatal state
    public static final String CONFIG_SQL_EXCEPTION_STATE = "sqlExceptionStateList";
    //sql exception fatal state
    public static final String CONFIG_CONFIG_PRINT_EXCLUSION_LIST = "configPrintExclusionList";

    //dummy impl on CommonDataSource
    public static final CommonDataSource Dummy_CommonDataSource = new CommonDataSource() {
        public PrintWriter getLogWriter() throws SQLException {
            throw new SQLFeatureNotSupportedException("Not supported");
        }

        public void setLogWriter(PrintWriter out) throws SQLException {
            throw new SQLFeatureNotSupportedException("Not supported");
        }

        public int getLoginTimeout() throws SQLException {
            throw new SQLFeatureNotSupportedException("Not supported");
        }

        public void setLoginTimeout(int seconds) throws SQLException {
            throw new SQLFeatureNotSupportedException("Not supported");
        }

        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException("Not supported");
        }
    };

    //pool state
    static final int POOL_NEW = 0;
    static final int POOL_STARTING = 1;
    static final int POOL_READY = 2;
    static final int POOL_CLOSING = 3;
    static final int POOL_CLOSED = 4;
    static final int POOL_CLEARING = 5;
    //connection state
    static final int CON_IDLE = 0;
    static final int CON_USING = 1;
    static final int CON_CLOSED = 2;
    //pool thread state
    static final int THREAD_WORKING = 0;
    static final int THREAD_WAITING = 1;
    static final int THREAD_EXIT = 2;
    //Connection reset pos in array
    static final int PS_AUTO = 0;
    static final int PS_TRANS = 1;
    static final int PS_READONLY = 2;
    static final int PS_CATALOG = 3;
    static final int PS_SCHEMA = 4;
    static final int PS_NETWORK = 5;
    //eviction status
    static final String DESC_RM_INIT = "init";
    static final String DESC_RM_BAD = "bad";
    static final String DESC_RM_ABORT = "abort";
    static final String DESC_RM_IDLE = "idle";
    static final String DESC_RM_CLOSED = "closed";
    static final String DESC_RM_CLEAR = "clear";
    static final String DESC_RM_DESTROY = "destroy";

    //***************************************************************************************************************//
    //                                1: jdbc global proxy (3)                                                       //
    //***************************************************************************************************************//
    static final Connection CLOSED_CON = (Connection) Proxy.newProxyInstance(
            ConnectionPoolStatics.class.getClassLoader(),
            new Class[]{Connection.class},
            new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if ("toString".equals(method.getName())) {
                        return "Connection has been closed";
                    } else {
                        throw new SQLException("No operations allowed after connection closed");
                    }
                }
            }
    );
    static final CallableStatement CLOSED_CSTM = (CallableStatement) Proxy.newProxyInstance(
            ConnectionPoolStatics.class.getClassLoader(),
            new Class[]{CallableStatement.class},
            new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if ("toString".equals(method.getName())) {
                        return "Statement has been closed";
                    } else {
                        throw new SQLException("No operations allowed after statement closed");
                    }
                }
            }
    );
    static final ResultSet CLOSED_RSLT = (ResultSet) Proxy.newProxyInstance(
            ConnectionPoolStatics.class.getClassLoader(),
            new Class[]{ResultSet.class},
            new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if ("toString".equals(method.getName())) {
                        return "ResultSet has been closed";
                    } else {
                        throw new SQLException("No operations allowed after resultSet closed");
                    }
                }
            }
    );

    //***************************************************************************************************************//
    //                               2: JDBC close methods(4)                                                        //
    //***************************************************************************************************************//
    public static void oclose(ResultSet r) {
        try {
            r.close();
        } catch (Throwable e) {
            CommonLog.debug("Warning:Error at closing resultSet", e);
        }
    }

    public static void oclose(Statement s) {
        try {
            s.close();
        } catch (Throwable e) {
            CommonLog.debug("Warning:Error at closing statement", e);
        }
    }

    public static void oclose(Connection c) {
        try {
            c.close();
        } catch (Throwable e) {
            CommonLog.debug("Warning:Error at closing connection", e);
        }
    }

    public static void oclose(XAConnection c) {
        try {
            c.close();
        } catch (Throwable e) {
            CommonLog.debug("Warning:Error at closing connection", e);
        }
    }

    //***************************************************************************************************************//
    //                               3: JDBC body auto fill by javassist methods(2)                                  //
    //***************************************************************************************************************//
    static ProxyConnectionBase createProxyConnection(PooledConnection p) throws SQLException {
        throw new SQLException("Proxy classes not be generated,please execute 'ProxyClassGenerator' after compile");
    }

    static ResultSet createProxyResultSet(ResultSet raw, ProxyStatementBase owner, PooledConnection p) throws SQLException {
        throw new SQLException("Proxy classes not be generated,please execute 'ProxyClassGenerator' after compile");
    }

    //***************************************************************************************************************//
    //                               4: JDBC other help methods(3)                                                   //
    //***************************************************************************************************************//
    public static Driver loadDriver(String driverClassName) throws BeeDataSourceConfigException {
        try {
            return (Driver) Class.forName(driverClassName).newInstance();
        } catch (Throwable e) {
            throw new BeeDataSourceConfigException("Failed to create jdbc driver by class:" + driverClassName, e);
        }
    }

    static void checkJdbcProxyClass() {
        String[] classNames = {
                "org.stone.beecp.pool.Borrower",
                "org.stone.beecp.pool.PooledConnection",
                "org.stone.beecp.pool.ProxyConnection",
                "org.stone.beecp.pool.ProxyStatement",
                "org.stone.beecp.pool.ProxyPsStatement",
                "org.stone.beecp.pool.ProxyCsStatement",
                "org.stone.beecp.pool.ProxyDatabaseMetaData",
                "org.stone.beecp.pool.ProxyResultSet"};
        try {
            ClassLoader loader = ConnectionPoolStatics.class.getClassLoader();
            for (String className : classNames)
                Class.forName(className, true, loader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Jdbc proxy classes missed", e);
        }
    }

    static boolean validateTestSql(String poolName, Connection rawCon, String testSql, int validTestTimeout, boolean isDefaultAutoCommit) throws SQLException {
        boolean changed = false;
        Statement st = null;
        try {
            //step1: setAutoCommit to 'false'
            if (isDefaultAutoCommit) {
                try {
                    rawCon.setAutoCommit(false);
                    changed = true;
                } catch (Throwable e) {
                    throw new SQLException("Failed to setAutoCommit(false)", e);
                }
            }

            //step2: create statement and test 'QueryTimeout'
            st = rawCon.createStatement();
            boolean supportQueryTimeout = true;
            try {
                st.setQueryTimeout(validTestTimeout);
            } catch (Throwable e) {
                supportQueryTimeout = false;
                CommonLog.warn("BeeCP({})driver not support 'queryTimeout'", poolName, e);
            }

            //step3: execute test sql
            try {
                st.execute(testSql);
            } catch (Throwable e) {
                throw new TestSqlExecFailedException("Invalid test sql:" + testSql, e);
            } finally {
                rawCon.rollback();//why? maybe store procedure in test sql
            }

            return supportQueryTimeout;
        } finally {
            if (st != null) oclose(st);
            if (changed) rawCon.setAutoCommit(true);//reset to default
        }
    }
}