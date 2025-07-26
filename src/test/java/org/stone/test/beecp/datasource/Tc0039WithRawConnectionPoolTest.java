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
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.test.beecp.objects.MockRawConnectionPool;

import javax.sql.XAConnection;
import java.sql.Connection;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

/**
 * @author Chris Liao
 */
public class Tc0039WithRawConnectionPoolTest {

    @Test
    public void testGetConnectionByDriver() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setMaxActive(10);
        ds.setPoolName("file/beecp");
        ds.setUsername("root");
        ds.setPassword("root");
        ds.setUrl("jdbc:beecp://localhost/testdb");
        ds.setDriverClassName("org.stone.test.beecp.driver.MockDriver");
        ds.setPoolImplementClassName(MockRawConnectionPool.class.getName());
        Connection con1 = null;
        Connection con2 = null;

        try {
            con1 = ds.getConnection();
            Assertions.assertNotNull(con1);
            con2 = ds.getConnection("root", "root");
            Assertions.assertNotNull(con2);

            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assertions.assertEquals("file/beecp", vo.getPoolName());
            Assertions.assertEquals("compete", vo.getPoolMode());
            Assertions.assertEquals(10, vo.getPoolMaxSize());
            Assertions.assertEquals(0, vo.getIdleSize());
            Assertions.assertEquals(0, vo.getBorrowedSize());
            Assertions.assertEquals(0, vo.getSemaphoreWaitingSize());
            Assertions.assertEquals(0, vo.getTransferWaitingSize());
            Assertions.assertEquals(0, vo.getCreatingTimeoutCount());
            Assertions.assertEquals(0, vo.getCreatingCount());
            Assertions.assertEquals(0, vo.getCreatingTimeoutCount());
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }

    @Test
    public void testGetConnectionByDriverDs() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setConnectionFactoryClassName("org.stone.test.beecp.driver.MockDataSource");
        ds.setPoolImplementClassName(MockRawConnectionPool.class.getName());
        Connection con1 = null;
        Connection con2 = null;

        try {
            con1 = ds.getConnection();
            Assertions.assertNotNull(con1);
        } finally {
            oclose(con1);
        }
    }

    @Test
    public void testGetConnectionByFactory() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setConnectionFactoryClassName("org.stone.test.beecp.objects.MockDriverConnectionFactory");
        ds.setPoolImplementClassName(MockRawConnectionPool.class.getName());

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
    public void testGetXaConnectionByDriverDs() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactoryClassName("org.stone.test.beecp.driver.MockXaDataSource");
        config.setPoolImplementClassName(MockRawConnectionPool.class.getName());
        BeeDataSource ds = new BeeDataSource(config);

        XAConnection con1 = null;
        XAConnection con2 = null;

        try {
            con1 = ds.getXAConnection();
            Assertions.assertNotNull(con1);

            con2 = ds.getXAConnection("root", "root");
            Assertions.assertNotNull(con2);
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }

    @Test
    public void testGetXaConnectionByFactory() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setConnectionFactoryClassName("org.stone.test.beecp.objects.MockDriverXaConnectionFactory");
        ds.setPoolImplementClassName(MockRawConnectionPool.class.getName());

        XAConnection con1 = null;
        XAConnection con2 = null;
        try {
            con1 = ds.getXAConnection();
            Assertions.assertNotNull(con1);

            con2 = ds.getXAConnection("root", "root");
            Assertions.assertNotNull(con2);
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }
}
