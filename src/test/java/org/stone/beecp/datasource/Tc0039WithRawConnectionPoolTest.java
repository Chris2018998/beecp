/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.datasource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import javax.sql.XAConnection;
import java.sql.Connection;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

/**
 * @author Chris Liao
 */
public class Tc0039WithRawConnectionPoolTest extends TestCase {

    public void testGetConnectionByDriver() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setMaxActive(10);
        ds.setPoolName("beecp");
        ds.setUsername("root");
        ds.setPassword("root");
        ds.setUrl("jdbc:beecp://localhost/testdb");
        ds.setDriverClassName("org.stone.beecp.driver.MockDriver");
        ds.setPoolImplementClassName(org.stone.beecp.pool.RawConnectionPool.class.getName());
        Connection con1 = null;
        Connection con2 = null;

        try {
            con1 = ds.getConnection();
            Assert.assertNotNull(con1);
            con2 = ds.getConnection("root", "root");
            Assert.assertNotNull(con2);

            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assert.assertEquals("beecp", vo.getPoolName());
            Assert.assertEquals("compete", vo.getPoolMode());
            Assert.assertEquals(10, vo.getPoolMaxSize());
            Assert.assertEquals(0, vo.getIdleSize());
            Assert.assertEquals(0, vo.getBorrowedSize());
            Assert.assertEquals(0, vo.getSemaphoreWaitingSize());
            Assert.assertEquals(0, vo.getTransferWaitingSize());
            Assert.assertEquals(0, vo.getCreatingTimeoutCount());
            Assert.assertEquals(0, vo.getCreatingCount());
            Assert.assertEquals(0, vo.getCreatingTimeoutCount());
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }

    public void testGetConnectionByDriverDs() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setConnectionFactoryClassName("org.stone.beecp.driver.MockDataSource");
        ds.setPoolImplementClassName(org.stone.beecp.pool.RawConnectionPool.class.getName());
        Connection con1 = null;
        Connection con2 = null;

        try {
            con1 = ds.getConnection();
            Assert.assertNotNull(con1);
        } finally {
            oclose(con1);
        }
    }

    public void testGetConnectionByFactory() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setConnectionFactoryClassName("org.stone.beecp.objects.MockDriverConnectionFactory");
        ds.setPoolImplementClassName(org.stone.beecp.pool.RawConnectionPool.class.getName());

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

    public void testGetXaConnectionByDriverDs() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactoryClassName("org.stone.beecp.driver.MockXaDataSource");
        config.setPoolImplementClassName(org.stone.beecp.pool.RawConnectionPool.class.getName());
        BeeDataSource ds = new BeeDataSource(config);

        XAConnection con1 = null;
        XAConnection con2 = null;

        try {
            con1 = ds.getXAConnection();
            Assert.assertNotNull(con1);

            con2 = ds.getXAConnection("root", "root");
            Assert.assertNotNull(con2);
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }

    public void testGetXaConnectionByFactory() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setConnectionFactoryClassName("org.stone.beecp.objects.MockDriverXaConnectionFactory");
        ds.setPoolImplementClassName(org.stone.beecp.pool.RawConnectionPool.class.getName());

        XAConnection con1 = null;
        XAConnection con2 = null;
        try {
            con1 = ds.getXAConnection();
            Assert.assertNotNull(con1);

            con2 = ds.getXAConnection("root", "root");
            Assert.assertNotNull(con2);
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }
}
