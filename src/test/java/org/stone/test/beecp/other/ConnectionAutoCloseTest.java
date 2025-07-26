/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.other;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.stone.tools.BeanUtil.loadClass;

/**
 * @author Chris Liao
 */
public class ConnectionAutoCloseTest {

    public static void main(String[] args) throws Exception {
        String driver = "com.mysql.jdbc.Driver";
        String url = "JDBC:MYSQL://localhost/test";
        String user = "root";
        String password = "";

        Connection con1 = null;
        Statement statement10 = null;
        Statement statement11 = null;

        Connection con2 = null;
        Statement statement2 = null;
        ResultSet resultSet2 = null;
        try {
            loadClass(driver);
            //1:create connection by driver
            con1 = DriverManager.getConnection(url, user, password);

            con1.setSavepoint();


            //2：delete all records from table
            statement10 = con1.createStatement();
            statement10.execute("delete from test");

            //3: change auto-commit from true to false
            con1.setAutoCommit(false);
            String testValue = "123456";
            statement11 = con1.createStatement();
            statement11.execute("insert into test values(" + testValue + ")");
            con1.setAutoCommit(true);

            //4:create a new connection to check data
            con2 = DriverManager.getConnection(url, user, password);
            statement2 = con2.createStatement();
            resultSet2 = statement2.executeQuery("select id from test where id=" + testValue);
            while (resultSet2.next())
                System.out.println(resultSet2.getString("id"));
        } finally {
            if (statement10 != null) statement10.close();
            if (statement11 != null) statement11.close();
            if (con1 != null) con1.close();

            if (resultSet2 != null) resultSet2.close();
            if (statement2 != null) statement2.close();
            if (con2 != null) con2.close();
        }
    }

}
