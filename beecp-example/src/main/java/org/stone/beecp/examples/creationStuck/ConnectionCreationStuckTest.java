/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.examples.creationStuck;

import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.util.TestUtil;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Blocking Mock in creating connection
 *
 * @author chris liao
 */

public class ConnectionCreationStuckTest {

    public static void main(String[] args) throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setInitialSize(0);
        config.setRawConnectionFactory(new BlockingConnectionFactory());
        BeeDataSource dataSource = new BeeDataSource(config);

        Connection con = null;
        try {
            new InterruptionMockThread(dataSource).start();
            con = dataSource.getConnection();//exit blocking after three seconds,and get an exception
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            TestUtil.oclose(con);
        }
    }
}
