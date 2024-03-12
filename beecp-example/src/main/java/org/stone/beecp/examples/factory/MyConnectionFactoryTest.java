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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.util.TestUtil;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * customization on connection creation
 *
 * @author chris liao
 */
public class MyConnectionFactoryTest {
    private static Logger logger = LoggerFactory.getLogger(MyConnectionFactoryTest.class);

    public static void main(String[] args) throws Exception {
        String userName = "root";
        String password = "";
        String driver = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://localhost/test";//url is necessary to link db

        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setRawConnectionFactory(new MyConnectionFactory(driver, url, userName, password));//set factory instance
        //config.setConnectionFactoryClass(MyConnectionFactory.class );//set factory class
        //config.setConnectionFactoryClassName("org.stone.beecp.RawConnectionFactory.MyConnectionFactory");//set factory class name
        BeeDataSource dataSource = new BeeDataSource(config);

        Connection con = null;
        try {
            con = dataSource.getConnection();
            logger.info("Success,customization on connection factory");
        } catch (SQLException e) {
            logger.error("Failed to get a connection", e);
        } finally {
            TestUtil.oclose(con);
        }
    }
}
