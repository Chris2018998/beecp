/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.datasource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;

import java.io.PrintWriter;
import java.sql.Connection;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

/**
 * @author Chris Liao
 */
public class Tc0036ConnectionGetTest extends TestCase {

    public void testGetConnectionByDriver() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setUsername("root");
        ds.setPassword("root");
        ds.setUrl("jdbc:beecp://localhost/testdb");
        ds.setDriverClassName("org.stone.beecp.driver.MockDriver");
        Connection con1 = null;
        Connection con2 = null;

        try {
            con1 = ds.getConnection();
            Assert.assertNotNull(con1);
            con2 = ds.getConnection("root", "root");
            Assert.assertNotNull(con2);
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }

    public void testGetConnectionByDriverDs() throws Exception {
        BeeDataSource ds1 = new BeeDataSource();
        ds1.setConnectionFactoryClassName("org.stone.beecp.driver.MockDataSource");
        Connection con1 = null;
        Connection con2 = null;

        try {
            con1 = ds1.getConnection();
            Assert.assertNotNull(con1);
        } finally {
            oclose(con1);
        }

        Assert.assertNull(ds1.getLogWriter());
        Assert.assertNull(ds1.getParentLogger());
        ds1.setLogWriter(new PrintWriter(System.out));
        Assert.assertNotNull(ds1.getLogWriter());
        Assert.assertEquals(0, ds1.getLoginTimeout());
        ds1.setLoginTimeout(10);
        Assert.assertEquals(10, ds1.getLoginTimeout());

        BeeDataSource ds2 = new BeeDataSource();
        ds2.setUsername("root");
        ds2.setPassword("root");
        ds2.setConnectionFactoryClassName("org.stone.beecp.driver.MockDataSource");
        try {
            con1 = ds2.getConnection();
            Assert.assertNotNull(con1);
            con2 = ds2.getConnection("root", "root");
            Assert.assertNotNull(con2);
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }

    public void testGetConnectionByFactory() throws Exception {
        String dataSourceClassName = "org.stone.beecp.objects.MockDriverConnectionFactory";
        BeeDataSource ds = new BeeDataSource();
        ds.setConnectionFactoryClassName(dataSourceClassName);

        Connection con1 = null;
        Connection con2 = null;

        try {
            con1 = ds.getConnection(null, null);
            Assert.assertNotNull(con1);

            con2 = ds.getConnection();
            Assert.assertNotNull(con2);
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }
}