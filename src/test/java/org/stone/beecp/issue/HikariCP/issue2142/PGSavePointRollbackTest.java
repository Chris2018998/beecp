package org.stone.beecp.issue.HikariCP.issue2142;

import org.stone.beecp.BeeDataSource;

import java.sql.*;

/**
 * 1)Test Env:
 * Java: Java-1.8.0_65-b17
 * DB: PostgreSQL-10.4-1-win64-bigsql.exe
 * Driver: postgresql-42.7.1.jar
 * JDBC-Pool: stone-1.2.8
 * <p>
 * 2)Test Machines
 * Server: Os:Win7_64,CPU:2.8Hz*2(PG80),Memory(8G)
 * Client: Os:Win7_64,CPU:2.6Hz*4(I5-4210M),Memory(12G)
 * <p>
 * 3)Test info
 * Chris Liao(Stone project owner)
 * Test Date: 2024/02/27 in China
 * <p>
 * Test Result:Passed
 */
public class PGSavePointRollbackTest {

    public static void main(String[] args) throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setInitialSize(1);//only one
        ds.setMaxActive(1);//only one
        ds.setDefaultSchema("public");

        String driverClass = "org.postgresql.Driver";
        String url = "jdbc:postgresql://localhost:5432/postgres";
        String username = "postgres";
        String password = "root";

        ds.setDriverClassName(driverClass);
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDefaultAutoCommit(true);//default value is boolean true

        //step1:insert a data to table and set a save-point
        Connection conn1 = null;
        try {
            conn1 = ds.getConnection();
            conn1.setAutoCommit(false);
            /**
             * drop table Home_Work;
             * create table Home_Work(name char(10));
             */
            conn1.createStatement().execute(
                    "INSERT INTO Home_Work (Name) VALUES ('JAVA')");//table is empty

            Savepoint savepoint = conn1.setSavepoint();
            conn1.rollback(savepoint);
        } finally {
            if (conn1 != null) conn1.close();
        }

        /**
         * step2:check the autoCommit whether reset to be default(true)
         * Test Result:passed
         */
        Connection conn2 = null;
        try {//
            conn2 = ds.getConnection();
            if (!conn2.getAutoCommit()) throw new SQLException("AutoCommit not be reset to default(true)");
        } finally {
            if (conn2 != null) conn2.close();
        }

        /**
         * step3: test data whether inserted
         * Test Result:passed(not inserted)
         */
        Connection conn3 = null;
        Statement statement;
        ResultSet resultSet;
        try {
            conn3 = DriverManager.getConnection(url, username, password);
            statement = conn3.createStatement();
            resultSet = statement.executeQuery("select * from Home_Work");
            if (resultSet.next()) {//has inserted into db
                throw new SQLException("Data not inserted");//exception will be thrown from here,why?
            }
        } finally {
            if (conn3 != null) conn3.close();
        }

        System.out.println("Test case passed on SavePoint rollback");
    }
}
