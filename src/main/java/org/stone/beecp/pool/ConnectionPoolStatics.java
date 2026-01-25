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

import org.stone.beecp.BeeConnectionPool;
import org.stone.beecp.exception.BeeDataSourceConfigException;
import org.stone.beecp.exception.BeeDataSourcePoolHasClosedException;
import org.stone.beecp.exception.BeeDataSourcePoolLazyInitializationException;
import org.stone.beecp.exception.ConnectionTestSqlExecutedException;

import javax.sql.CommonDataSource;
import javax.sql.XAConnection;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;

import static org.stone.tools.LogPrinter.DefaultLogPrinter;

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
    public static final String CONFIG_FACTORY_PROP = "connectionFactoryProperties";
    //connect properties count for driver or driver dataSource
    public static final String CONFIG_FACTORY_PROP_SIZE = "connectionFactoryProperties.size";
    //connect properties prefix for driver or driver dataSource
    public static final String CONFIG_FACTORY_PROP_KEY_PREFIX = "connectionFactoryProperties.";
    //sql exception fatal code
    public static final String CONFIG_SQL_EXCEPTION_CODE = "sqlExceptionCodeList";
    //sql exception fatal state
    public static final String CONFIG_SQL_EXCEPTION_STATE = "sqlExceptionStateList";
    //sql exception fatal state
    public static final String CONFIG_EXCLUSION_LIST_OF_PRINT = "exclusionListOfPrint";

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

    //connection state
    public static final int CON_CLOSED = 0;
    public static final int CON_IDLE = 1;
    public static final int CON_CREATING = 2;
    public static final int CON_BORROWED = 3;

    //pool state
    public static final int POOL_LAZY = -1;
    public static final int POOL_NEW = 0;
    public static final int POOL_STARTING = 1;
    public static final int POOL_READY = 2;
    public static final int POOL_CLOSING = 3;
    public static final int POOL_CLOSED = 4;
    public static final int POOL_RESTARTING = 5;
    public static final int POOL_RESTART_FAILED = 6;
    public static final int POOL_SUSPENDED = 7;


    public static final BeeConnectionPool LAZY_POOL = (BeeConnectionPool) Proxy.newProxyInstance(
            ConnectionPoolStatics.class.getClassLoader(),
            new Class[]{BeeConnectionPool.class},
            new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    String methodName = method.getName();
                    if ("isClosed".equals(methodName)) {
                        return true;
                    } else if ("toString".equals(methodName)) {
                        return getPoolStateDesc(POOL_LAZY);
                    } else {
                        throw new BeeDataSourcePoolLazyInitializationException("No operations allowed on lazy pool");
                    }
                }
            }
    );

    public static final BeeConnectionPool CLOSED_POOL = (BeeConnectionPool) Proxy.newProxyInstance(
            ConnectionPoolStatics.class.getClassLoader(),
            new Class[]{BeeConnectionPool.class},
            new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    String methodName = method.getName();
                    if ("isClosed".equals(methodName)) {
                        return true;
                    } else if ("toString".equals(methodName)) {
                        return getPoolStateDesc(POOL_CLOSED);
                    } else {
                        throw new BeeDataSourcePoolHasClosedException("No operations allowed on closed pool");
                    }
                }
            }
    );

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
    static final String DESC_RM_POOL_START = "pool_init";
    static final String DESC_RM_CON_BAD = "bad";
    static final String DESC_RM_CON_ABORT = "abort";
    static final String DESC_RM_CON_IDLE = "idle";
    static final String DESC_RM_POOL_RESTART = "pool_restart";
    static final String DESC_RM_POOL_SHUTDOWN = "pool_shutdown";
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
                        throw new SQLException("No operations allowed on closed connection");
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
                        throw new SQLException("No operations allowed on closed statement");
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
                        throw new SQLException("No operations allowed on closed resultSet");
                    }
                }
            }
    );

    static String getPoolStateDesc(int state) {
        switch (state) {
            case POOL_LAZY:
                return "Pool is lazy and initialized by calling one of its methods:getObjectHandle or getXAConnection";
            case POOL_NEW:
                return "Pool is new";
            case POOL_STARTING:
                return "Pool is starting";
            case POOL_READY:
                return "Pool is ready";
            case POOL_CLOSING:
                return "Pool is closing";
            case POOL_CLOSED:
                return "Pool has been closed";
            case POOL_RESTARTING:
                return "Pool is restarting";
            case POOL_RESTART_FAILED:
                return "Pool has restarted failed";
            case POOL_SUSPENDED:
                return "Pool has been suspended";
            default:
                return "Unknown state of pool";
        }
    }

    //***************************************************************************************************************//
    //                               2: JDBC close methods(4)                                                        //
    //***************************************************************************************************************//
    public static void oclose(ResultSet r) {
        try {
            r.close();
        } catch (Throwable e) {
            DefaultLogPrinter.warn("Warning:Error at closing resultSet", e);
        }
    }

    public static void oclose(Statement s) {
        try {
            s.close();
        } catch (Throwable e) {
            DefaultLogPrinter.warn("Warning:Error at closing statement", e);
        }
    }

    public static void oclose(Connection c) {
        try {
            c.close();
        } catch (SQLRecoverableException e) {
            DefaultLogPrinter.warn("Warning:Error at closing connection", e);
            oclose(c);//retry
        } catch (Throwable e) {
            DefaultLogPrinter.warn("Warning:Error at closing connection", e);
        }
    }

    public static void oclose(XAConnection c) {
        try {
            c.close();
        } catch (Throwable e) {
            DefaultLogPrinter.warn("Warning:Error at closing xaConnection", e);
        }
    }

    //***************************************************************************************************************//
    //                               3: JDBC body auto fill by javassist methods(2)                                  //
    //***************************************************************************************************************//
    static ResultSet createProxyResultSet(ResultSet raw, ProxyStatementBase owner, PooledConnection p) throws SQLException {
        throw new SQLException("Proxy classes not be generated,please execute 'ProxyClassGenerator' after compile");
    }

    //***************************************************************************************************************//
    //                               4: JDBC other help methods(3)                                                   //
    //***************************************************************************************************************//
    public static Driver loadDriver(String driverClassName) throws BeeDataSourceConfigException {
        try {
            return (Driver) Class.forName(driverClassName).getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            throw new BeeDataSourceConfigException("Failed to create jdbc driver by class:" + driverClassName, e);
        }
    }

    static void checkJdbcProxyClass() throws ClassNotFoundException {
        String[] classNames = {
                "org.stone.beecp.pool.ProxyConnection",
                "org.stone.beecp.pool.ProxyStatement",
                "org.stone.beecp.pool.ProxyPsStatement",
                "org.stone.beecp.pool.ProxyCsStatement",
                "org.stone.beecp.pool.ProxyResultSet",
                "org.stone.beecp.pool.ProxyDatabaseMetaData",
                "org.stone.beecp.pool.ProxyConnection4L",
                "org.stone.beecp.pool.ProxyStatement4L",
                "org.stone.beecp.pool.ProxyPsStatement4L",
                "org.stone.beecp.pool.ProxyCsStatement4L"};

        ClassLoader loader = ConnectionPoolStatics.class.getClassLoader();
        for (String className : classNames)
            Class.forName(className, true, loader);
    }

    //If driver not support 'isValid' method,this static method is called on first connection to validate test sql and whether supported 'setQueryTimeout' method
    static boolean validateTestSQL(String poolName, Connection rawCon, String testSql, int validTestTimeout, boolean isDefaultAutoCommit) throws SQLException {
        Statement st = null;
        boolean changed = false;

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
                DefaultLogPrinter.warn("BeeCP({})-driver not support 'queryTimeout'", poolName, e);
            }

            //step3: execute test sql
            try {
                st.execute(testSql);
            } catch (Throwable e) {
                throw new ConnectionTestSqlExecutedException("Invalid test sql:" + testSql, e);
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