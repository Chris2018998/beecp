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
import org.stone.beecp.BeeDataSourceConfig;

import javax.sql.XAConnection;
import java.io.PrintWriter;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

/**
 * @author Chris Liao
 */
public class Tc0037XaConnectionGetTest {

    @Test
    public void testGetConnectionByDriverDs() throws Exception {
        String dataSourceClassName = "org.stone.test.beecp.driver.MockXaDataSource";
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactoryClassName(dataSourceClassName);
        BeeDataSource ds = new BeeDataSource(config);

        XAConnection con1 = null;
        XAConnection con2 = null;

        try {
            con1 = ds.getXAConnection();
            Assertions.assertNotNull(con1);
            Assertions.assertNull(ds.getLogWriter());
            Assertions.assertNull(ds.getParentLogger());
            ds.setLogWriter(new PrintWriter(System.out));
            Assertions.assertNotNull(ds.getLogWriter());
            Assertions.assertEquals(0, ds.getLoginTimeout());
            ds.setLoginTimeout(10);
            Assertions.assertEquals(10, ds.getLoginTimeout());

        } finally {
            oclose(con1);
        }

        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setConnectionFactoryClassName(dataSourceClassName);
        config2.setUsername("root");
        config2.setPassword("root");
        BeeDataSource ds2 = new BeeDataSource(config2);

        try {
            con1 = ds2.getXAConnection();
            Assertions.assertNotNull(con1);
            con2 = ds2.getXAConnection("root", "root");
            Assertions.assertNotNull(con2);
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }

    @Test
    public void testGetConnectionByFactory() throws Exception {
        String dataSourceClassName = "org.stone.test.beecp.objects.MockDriverXaConnectionFactory";
        BeeDataSource ds = new BeeDataSource();
        ds.setConnectionFactoryClassName(dataSourceClassName);

        XAConnection con1 = null;
        XAConnection con2 = null;
        try {
            con1 = ds.getXAConnection("root", "root");
            Assertions.assertNotNull(con1);

            con2 = ds.getXAConnection();
            Assertions.assertNotNull(con2);
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }
}
