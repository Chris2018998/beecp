/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.test.beecp.driver.MockDataSource;
import org.stone.test.beecp.driver.MockXaDataSource;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import static org.stone.test.beecp.config.DsConfigFactory.JDBC_URL;

/**
 * @author Chris Liao
 */
public class Tc0045DsSubCommonDsTest {

    @Test
    public void testUpdateToConnectionFactoryByDriver() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setJdbcUrl(JDBC_URL);
            testBeeDataSource(ds);
        }
    }

    @Test
    public void testUpdateToConnectionFactoryByDriverDs() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setConnectionFactoryClassName(MockDataSource.class.getName());//driver data source
            testBeeDataSource(ds);
        }
    }

    @Test
    public void testUpdateToXaConnectionFactoryByDriverDs() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setConnectionFactoryClassName(MockXaDataSource.class.getName());//driver data source
            testBeeDataSource(ds);
        }
    }

    private void testBeeDataSource(BeeDataSource ds) throws SQLException {
        //ds pool ready
        Assertions.assertTrue(ds.isClosed());//ds pool not ready
        Assertions.assertNull(ds.getLogWriter());
        Assertions.assertNull(ds.getParentLogger());
        Assertions.assertEquals(0, ds.getLoginTimeout());

        //no impact
        ds.setLogWriter(new PrintWriter(System.out));
        Assertions.assertNull(ds.getLogWriter());
        ds.setLoginTimeout(5);
        Assertions.assertEquals(0, ds.getLoginTimeout());

        try (Connection con = ds.getConnection()) {
            Assertions.assertNotNull(con);
        }
        Assertions.assertFalse(ds.isClosed());//ds pool ready

        Assertions.assertNull(ds.getParentLogger());
        ds.setLogWriter(new PrintWriter(System.out));
        Assertions.assertNotNull(ds.getLogWriter());
        ds.setLoginTimeout(5);
        Assertions.assertEquals(5, ds.getLoginTimeout());
    }

    @Test
    public void testFeatureNotSupportedException() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactory(new MockConnectionFactory());
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try {
                ds.getLogWriter();
                Assertions.fail("[testSetOnNullCommonDs]test failed");
            } catch (SQLFeatureNotSupportedException e) {
                //do nothing
            }

            try {
                ds.setLogWriter(new PrintWriter(System.out));
                Assertions.fail("[testSetOnNullCommonDs]test failed");
            } catch (SQLFeatureNotSupportedException e) {
                //do nothing
            }

            try {
                ds.getParentLogger();
                Assertions.fail("[testSetOnNullCommonDs]test failed");
            } catch (SQLFeatureNotSupportedException e) {
                //do nothing
            }

            try {
                ds.getLoginTimeout();
                Assertions.fail("[testSetOnNullCommonDs]test failed");
            } catch (SQLFeatureNotSupportedException e) {
                //do nothing
            }

            try {
                ds.setLoginTimeout(7);
                Assertions.fail("[testSetOnNullCommonDs]test failed");
            } catch (SQLFeatureNotSupportedException e) {
                //do nothing
            }
        }
    }
}
