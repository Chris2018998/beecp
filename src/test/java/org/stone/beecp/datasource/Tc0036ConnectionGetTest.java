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
import org.stone.beecp.BeeDataSourceConfig;

import javax.sql.XAConnection;
import java.sql.Connection;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0036ConnectionGetTest extends TestCase {

    public void testGetConnectionByDriverDs() throws Exception {
        String dataSourceClassName = "org.stone.beecp.driver.MockDataSource";
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactoryClassName(dataSourceClassName);
        BeeDataSource ds = new BeeDataSource(config);

        Connection con1 = null;
        Connection con2 = null;
        try {
            con1 = ds.getConnection();
            Assert.assertNotNull(con1);

            con2 = ds.getConnection(null, null);
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

            con2 =  ds.getConnection();
            Assert.assertNotNull(con2);
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }

    public void testGetXaConnectionByDriverDs() throws Exception {
        String dataSourceClassName = "org.stone.beecp.driver.MockXaDataSource";
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactoryClassName(dataSourceClassName);
        BeeDataSource ds = new BeeDataSource(config);

        XAConnection con1 = null;
        XAConnection con2 = null;
        try {
            con1 = ds.getXAConnection();
            Assert.assertNotNull(con1);

            con2 = ds.getXAConnection(null, null);
            Assert.assertNotNull(con2);
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }

    public void testGetXaConnectionByFactory() throws Exception {
        String dataSourceClassName = "org.stone.beecp.objects.MockDriverXaConnectionFactory";
        BeeDataSource ds = new BeeDataSource();
        ds.setConnectionFactoryClassName(dataSourceClassName);

        XAConnection con1 = null;
        XAConnection con2 = null;
        try {
            con2 = ds.getXAConnection(null, null);
            Assert.assertNotNull(con2);

            con1 = ds.getXAConnection();
            Assert.assertNotNull(con1);
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }
}