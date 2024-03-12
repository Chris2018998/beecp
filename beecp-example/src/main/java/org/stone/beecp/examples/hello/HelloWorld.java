/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.examples.hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.util.TestUtil;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * hello example
 *
 * @author chris liao
 */
public class HelloWorld {
    public static void main(String[] args) {
        String userName = "root";
        String password = "";
        String driver = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://localhost/test";//url is necessary to link db

        BeeDataSourceConfig config = new BeeDataSourceConfig(driver, url, userName, password);
        //BeeDataSourceConfig config = new BeeDataSourceConfig(null, url, userName, password);//a matched driver will be applied by pool if null

        BeeDataSource dataSource = new BeeDataSource(config);
        Logger logger = LoggerFactory.getLogger(HelloWorld.class);

        Connection con = null;
        try {
            con = dataSource.getConnection();
            logger.info("Hello World! BeeCP is Coming.");
        } catch (SQLException e) {
            logger.error("Failed to get a connection", e);
        } finally {
            TestUtil.oclose(con);
        }
    }
}