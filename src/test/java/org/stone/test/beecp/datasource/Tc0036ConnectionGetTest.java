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

import java.io.PrintWriter;
import java.sql.Connection;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

/**
 * @author Chris Liao
 */
public class Tc0036ConnectionGetTest {

    @Test
    public void testGetConnectionByDriver() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setUsername("root");
        ds.setPassword("root");
        ds.setUrl("jdbc:beecp://localhost/testdb");
        ds.setDriverClassName("org.stone.test.beecp.driver.MockDriver");
        Connection con1 = null;
        Connection con2 = null;

        try {
            con1 = ds.getConnection();
            Assertions.assertNotNull(con1);
            con2 = ds.getConnection("root", "root");
            Assertions.assertNotNull(con2);
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }

    @Test
    public void testGetConnectionByDriverDs() throws Exception {
        BeeDataSource ds1 = new BeeDataSource();
        ds1.setConnectionFactoryClassName("org.stone.test.beecp.driver.MockDataSource");
        Connection con1 = null;
        Connection con2 = null;

        try {
            con1 = ds1.getConnection();
            Assertions.assertNotNull(con1);
        } finally {
            oclose(con1);
        }

        Assertions.assertNull(ds1.getLogWriter());
        Assertions.assertNull(ds1.getParentLogger());
        ds1.setLogWriter(new PrintWriter(System.out));
        Assertions.assertNotNull(ds1.getLogWriter());
        Assertions.assertEquals(0, ds1.getLoginTimeout());
        ds1.setLoginTimeout(10);
        Assertions.assertEquals(10, ds1.getLoginTimeout());

        BeeDataSource ds2 = new BeeDataSource();
        ds2.setUsername("root");
        ds2.setPassword("root");
        ds2.setConnectionFactoryClassName("org.stone.test.beecp.driver.MockDataSource");
        try {
            con1 = ds2.getConnection();
            Assertions.assertNotNull(con1);
            con2 = ds2.getConnection("root", "root");
            Assertions.assertNotNull(con2);
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }

    @Test
    public void testGetConnectionByFactory() throws Exception {
        String dataSourceClassName = "org.stone.test.beecp.objects.MockDriverConnectionFactory";
        BeeDataSource ds = new BeeDataSource();
        ds.setConnectionFactoryClassName(dataSourceClassName);

        Connection con1 = null;
        Connection con2 = null;

        try {
            con1 = ds.getConnection(null, null);
            Assertions.assertNotNull(con1);

            con2 = ds.getConnection();
            Assertions.assertNotNull(con2);
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }
}