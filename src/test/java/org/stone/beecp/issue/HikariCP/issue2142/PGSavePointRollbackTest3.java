package org.stone.beecp.issue.HikariCP.issue2142;

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
 * Test Result:passed
 */
public class PGSavePointRollbackTest3 {

    public static void main(String[] args) throws Exception {
        String driverClass = "org.postgresql.Driver";
        String url = "jdbc:postgresql://localhost:5432/postgres";
        String username = "postgres";
        String password = "root";


        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, username, password);
            conn.setAutoCommit(false);

            /**
             * drop table Home_Work;
             * create table Home_Work(name char(10));
             */
            conn.createStatement().execute(
                    "INSERT INTO Home_Work (Name) VALUES ('JAVA')");//table data is empty

            Savepoint savepoint = conn.setSavepoint();
            conn.rollback(savepoint);
        } finally {
            if (conn != null) conn.close();
        }

        //test data whether inserted
        Connection conn2 = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            conn2 = DriverManager.getConnection(url, username, password);
            statement = conn2.createStatement();
            resultSet = statement.executeQuery("select * from Home_Work");
            if (resultSet.next()) {//table still empty
                throw new SQLException("SavePoint not be rollback");//<--thrown
            }
        } finally {
            if (conn2 != null) conn2.close();
        }

        System.out.println("Test case passed on SavePoint rollback");
    }
}
