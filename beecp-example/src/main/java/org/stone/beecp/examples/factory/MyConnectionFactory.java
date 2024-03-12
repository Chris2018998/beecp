/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.examples.factory;

import org.stone.beecp.RawConnectionFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * customization on connection creation
 *
 * @author chris liao
 */
public class MyConnectionFactory implements RawConnectionFactory {
    private final String url;
    private final String userName;
    private final String password;

    MyConnectionFactory() throws Exception {
        this("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost/test", "root", "");
    }

    MyConnectionFactory(String driver, String url, String userName, String password) throws Exception {
        this.url = url;
        this.userName = userName;
        this.password = password;
        Class.forName(driver);
    }

    public Connection create() throws SQLException {
        return DriverManager.getConnection(url, userName, password);
    }
}
