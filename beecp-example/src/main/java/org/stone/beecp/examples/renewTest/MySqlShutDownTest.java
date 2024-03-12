/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.examples.renewTest;

import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.util.TestUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * DB Shutdown test
 *
 * @author chris liao
 */
public class MySqlShutDownTest {
    public static void main(String[] args) throws Exception {
        String userName = "root";
        String password = "";
        String driver = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://localhost/test";//url is necessary to link db

        BeeDataSourceConfig config = new BeeDataSourceConfig(driver, url, userName, password);
        config.setInitialSize(1);
        config.setMaxActive(1);
        BeeDataSource dataSource = new BeeDataSource(config);

        BeeConnectionPoolMonitorVo vo = dataSource.getPoolMonitorVo();
        if (vo.getIdleSize() != 1) throw new AssertionError("Idle connections is not expected count(1)");

        //Operation: shut down mysql server by manual
        System.out.println("Shutdown Db server.........................");
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));

        Connection connection = null;
        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {//should be an exception related with creation failure
            vo = dataSource.getPoolMonitorVo();
            if ((vo.getIdleSize() + vo.getUsingSize()) == 0) {
                System.out.println("All deal connections were removed from pool");
            } else {
                throw new AssertionError("Total connections is not expected count(0)");
            }
        } finally {
            if (connection != null) {
                TestUtil.oclose(connection);
                connection = null;
            }
        }

        //Operation: restart mysql server
        System.out.println("Startup Db server.....................");
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));

        try {
            connection = dataSource.getConnection();
            System.out.println("got a renewed connection from pool");
        } catch (SQLException e) {
        } finally {
            if (connection != null) TestUtil.oclose(connection);
        }
    }
}


