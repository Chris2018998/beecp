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
import org.stone.base.TestException;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;
import org.stone.beecp.pool.ConnectionPoolStatics;
import org.stone.beecp.pool.exception.PoolCreateFailedException;

import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.beecp.JdbcConfig.*;

/**
 * Test Pool Creation in pool
 */
public class DsPoolCreateFailedTest extends TestCase {

    public void testFailureOnDsConstructor() throws Exception {
        try {
            BeeDataSourceConfig config = new BeeDataSourceConfig();
            config.setDriverClassName(JDBC_DRIVER);
            config.setJdbcUrl(JDBC_URL);
            config.setUsername(JDBC_USER);
            config.setUsername(JdbcConfig.JDBC_PASSWORD);
            config.setPoolImplementClassName("xx.xx.xx");//dummy class
            new BeeDataSource(config);
            throw new TestException("Test failed on case[DsPoolCreateFailedTest.testCreateFailByConstruct]");
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof PoolCreateFailedException) {
                PoolCreateFailedException poolException = (PoolCreateFailedException) cause;
                Throwable poolCause = poolException.getCause();
                if (!(poolCause instanceof ClassNotFoundException))
                    throw new TestException("Test Failure exception type is not ClassNotFoundException ");
            }
        }
    }

    //lazy creation test
    public void testFailureOnDsGetConnection() throws Exception {
        Connection con = null;
        try {
            BeeDataSource ds = new BeeDataSource(JDBC_DRIVER, JDBC_URL, JDBC_USER, JDBC_PASSWORD);
            ds.setPoolImplementClassName("xx.xx.xx");//dummy class
            con = ds.getConnection();
            throw new TestException("Test failed on case[DsPoolCreateFailedTest.testCreateFailByConstruct]");
        } catch (SQLException e) {
            Throwable poolCause = e.getCause();
            Assert.assertTrue(poolCause instanceof ClassNotFoundException);
//            if (!(poolCause instanceof ClassNotFoundException))
//                throw new TestException("Test Failure exception type is not ClassNotFoundException ");
//
        } finally {
            if (con != null) ConnectionPoolStatics.oclose(con);
        }
    }
}
