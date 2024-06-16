/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.dataSource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.config.DsConfigFactory;
import org.stone.beecp.pool.exception.PoolCreateFailedException;

import static org.stone.beecp.config.DsConfigFactory.*;

/**
 * @author Chris Liao
 */
public class Tc0030DataSourceCreateTest extends TestCase {

    public void testWithoutParameter() {
        new BeeDataSource();
    }

    public void testOnConfig() {
        new BeeDataSource(DsConfigFactory.createDefault());
    }

    public void testOnJdbcInfo() {
        String driver = JDBC_DRIVER;
        String url = JDBC_URL;
        String user = JDBC_USER;
        String password = JDBC_PASSWORD;
        BeeDataSource ds = null;
        try {
            ds = new BeeDataSource(driver, url, user, password);
        } finally {
            if (ds != null) ds.close();
        }
    }

    public void testDataSourceCreateFailed() {
        BeeDataSource ds = null;
        try {
            BeeDataSourceConfig config = DsConfigFactory.createDefault();
            config.setPoolImplementClassName("xx.xx.xx");//invalid pool class name
            ds = new BeeDataSource(config);
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof PoolCreateFailedException);

            PoolCreateFailedException poolException = (PoolCreateFailedException) cause;
            Throwable poolCause = poolException.getCause();
            Assert.assertTrue(poolCause instanceof ClassNotFoundException);
        } finally {
            if (ds != null) ds.close();
        }
    }
}
