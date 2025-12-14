/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeMethodExecutionLog;
import org.stone.test.base.TestUtil;
import org.stone.test.beecp.driver.MockConnectionProperties;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;
import org.stone.test.beecp.objects.listener.MockMethodExecutionListener1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.stone.beecp.BeeMethodExecutionLog.Type_SQL_Execution;

/**
 * @author Chris Liao
 */
public class Tc0081SQLExecutionLogTest {

    @Test
    public void testExceptionLog() throws SQLException {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setMethodExecutionListener(new MockMethodExecutionListener1());
            ds.setEnableMethodExecutionLogCache(true);

            MockConnectionProperties connectionProperties = new MockConnectionProperties();
            connectionProperties.throwsExceptionWhenCallMethod("execute,executeQuery,executeUpdate,executeLargeUpdate");
            connectionProperties.setMockException1(new SQLException("Failed to execute sql,because database was down"));
            MockConnectionFactory connectionFactory = new MockConnectionFactory(connectionProperties);
            ds.setConnectionFactory(connectionFactory);

            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                try {
                    st.execute("select 1");
                    Assertions.fail("[testExceptionLog]test failed");
                } catch (SQLException e) {
                    //do nothing
                }
                try {
                    st.executeQuery("select 1");
                    Assertions.fail("[testExceptionLog]test failed");
                } catch (SQLException e) {
                    //do nothing
                }
                try {
                    st.executeUpdate("update user set id=1");
                    Assertions.fail("[testExceptionLog]test failed");
                } catch (SQLException e) {
                    //do nothing
                }
                try {
                    st.executeLargeUpdate("update user set id=1");
                    Assertions.fail("[testExceptionLog]test failed");
                } catch (SQLException e) {
                    //do nothing
                }

                List<BeeMethodExecutionLog> logList = ds.getMethodExecutionLog(Type_SQL_Execution);
                Assertions.assertEquals(4, logList.size());
                for (BeeMethodExecutionLog log : logList) {
                    Assertions.assertNotNull(log.getParameters());
                    Assertions.assertNotNull(log.getSql());
                    Assertions.assertTrue(log.isException());
                }
                Assertions.assertEquals(4, ds.clearMethodExecutionLog(Type_SQL_Execution).size());
            }
        }
    }

    @Test
    public void testSlowLog() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setMethodExecutionListener(new MockMethodExecutionListener1());
            ds.setEnableMethodExecutionLogCache(true);
            ds.setSlowSQLThreshold(50L);

            MockConnectionProperties connectionProperties = new MockConnectionProperties();
            connectionProperties.parkWhenCallMethod("execute,executeQuery,executeUpdate,executeLargeUpdate,prepareStatement");
            connectionProperties.setParkNanos(TimeUnit.MILLISECONDS.toNanos(100L));
            MockConnectionFactory connectionFactory = new MockConnectionFactory(connectionProperties);
            ds.setConnectionFactory(connectionFactory);

            //1: test statement
            try (Connection con = ds.getConnection()) {
                Map<String, String> sqlMap = new HashMap<>(4);
                sqlMap.put("execute", "select 1");
                sqlMap.put("executeQuery", "select 1");
                sqlMap.put("executeUpdate", "update user set id=1");
                sqlMap.put("executeLargeUpdate", "update user set id=1");
                Thread statementThread = new StatementExecuteThread(con, sqlMap);
                statementThread.start();
                statementThread.join();

                List<BeeMethodExecutionLog> logList = ds.getMethodExecutionLog(Type_SQL_Execution);
                Assertions.assertEquals(4, logList.size());
                for (BeeMethodExecutionLog log : logList) {
                    Assertions.assertTrue(log.isSlow());
                }
            }//execute statement sql

            //2: clear
            ds.clearMethodExecutionLog(Type_SQL_Execution);
            Assertions.assertTrue(ds.getMethodExecutionLog(Type_SQL_Execution).isEmpty());

            //3: test PreparedStatement
            try (Connection con = ds.getConnection()) {
                Map<String, String> sqlMap = new HashMap<>(1);
                sqlMap.put("execute", "select 1");
                PrepareStatementThread preparedStatementThread = new PrepareStatementThread(con, sqlMap);
                preparedStatementThread.start();
                preparedStatementThread.join();

                List<BeeMethodExecutionLog> logList = ds.getMethodExecutionLog(Type_SQL_Execution);
                Assertions.assertEquals(2, logList.size());
                for (BeeMethodExecutionLog log : logList) {
                    // Assertions.assertTrue(log.getSqlPreparedTime() > 0L);
                    Assertions.assertTrue(log.isSlow());

                    try {
                        Assertions.assertFalse(log.cancelStatement());
                    } catch (Exception e) {
                        if (e instanceof NullPointerException) {
                            Assertions.fail("[testSlowLog]Test failed");
                        }
                    }
                }
            }//execute PreparedStatement sql
        }
    }

    @Test
    public void testCancelStatement() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setMethodExecutionListener(new MockMethodExecutionListener1());
            ds.setEnableMethodExecutionLogCache(true);//sync mode
            ds.setSlowSQLThreshold(1L);

            MockConnectionProperties connectionProperties = new MockConnectionProperties();
            connectionProperties.parkWhenCallMethod("execute");//blocking execute method
            connectionProperties.setParkNanos(TimeUnit.MILLISECONDS.toNanos(Long.MAX_VALUE));
            MockConnectionFactory connectionFactory = new MockConnectionFactory(connectionProperties);
            ds.setConnectionFactory(connectionFactory);

            try (Connection con = ds.getConnection()) {
                Map<String, String> sqlMap = new HashMap<>(1);
                sqlMap.put("execute", "select 1");
                PrepareStatementThread statementThread1 = new PrepareStatementThread(con, sqlMap);
                statementThread1.start();

                if (TestUtil.waitUtilWaiting(statementThread1)) {
                    List<BeeMethodExecutionLog> logList = ds.getMethodExecutionLog(Type_SQL_Execution);
                    Assertions.assertEquals(2, logList.size());
                    BeeMethodExecutionLog log = logList.get(0);
                    if (log.isRunning()) Assertions.assertTrue(log.cancelStatement());
                }

                Assertions.assertFalse(ds.cancelStatement(null));
                Assertions.assertFalse(ds.cancelStatement("Test"));
                PrepareStatementThread statementThread2 = new PrepareStatementThread(con, sqlMap);
                statementThread2.start();
                if (TestUtil.waitUtilWaiting(statementThread2)) {
                    List<BeeMethodExecutionLog> logList = ds.getMethodExecutionLog(Type_SQL_Execution);
                    Assertions.assertEquals(4, logList.size());
                    BeeMethodExecutionLog log = logList.get(0);
                    if (log.isRunning()) Assertions.assertTrue(ds.cancelStatement(log.getId()));
                }
            }//connection
        }
    }


    private static class StatementExecuteThread extends Thread {
        private final Connection con;
        private final Map<String, String> sqlMap;

        StatementExecuteThread(Connection con, Map<String, String> sqlMap) {
            this.con = con;
            this.sqlMap = sqlMap;
        }

        public void run() {
            try (Statement statement = con.createStatement()) {
                for (Map.Entry<String, String> entry : sqlMap.entrySet()) {
                    String methodName = entry.getKey();
                    if ("execute".equals(methodName)) {
                        statement.execute(entry.getValue());
                    } else if ("executeQuery".equals(methodName)) {
                        statement.executeQuery(entry.getValue());
                    } else if ("executeUpdate".equals(methodName)) {
                        statement.executeUpdate(entry.getValue());
                    } else if ("executeLargeUpdate".equals(methodName)) {
                        statement.executeLargeUpdate(entry.getValue());
                    }
                }
            } catch (SQLException e) {

            }
        }
    }

    private static class PrepareStatementThread extends Thread {
        private final Connection con;
        private final Map<String, String> sqlMap;

        PrepareStatementThread(Connection con, Map<String, String> sqlMap) {
            this.con = con;
            this.sqlMap = sqlMap;
        }

        public void run() {
            for (Map.Entry<String, String> entry : sqlMap.entrySet()) {
                String methodName = entry.getKey();
                try (PreparedStatement statement = con.prepareStatement(entry.getValue())) {
                    if ("execute".equals(methodName)) {
                        statement.execute();
                    } else if ("executeQuery".equals(methodName)) {
                        statement.executeQuery();
                    } else if ("executeUpdate".equals(methodName)) {
                        statement.executeUpdate();
                    } else if ("executeLargeUpdate".equals(methodName)) {
                        statement.executeLargeUpdate();
                    }
                } catch (SQLException e) {

                }
            }
        }
    }
}
