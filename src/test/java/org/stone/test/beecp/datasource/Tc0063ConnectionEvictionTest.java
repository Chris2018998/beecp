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
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.test.beecp.driver.MockConnectionProperties;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;
import org.stone.test.beecp.objects.predicate.MockEvictConnectionPredicate1;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0063ConnectionEvictionTest {

    private final int errorCode = 0b010000;
    private final String errorState = "57P02";

    @Test
    public void testOnErrorCode() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.addSqlExceptionCode(errorCode);

        MockConnectionProperties conProperties = new MockConnectionProperties();
        conProperties.setErrorCode(errorCode);
        conProperties.throwsExceptionWhenCallMethod("createStatement");
        config.setConnectionFactory(new MockConnectionFactory(conProperties));

        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection con = ds.getConnection()) {
                try {
                    con.createStatement();
                    fail("testOnErrorCode");
                } catch (SQLException e) {
                    Assertions.assertEquals(errorCode, e.getErrorCode());
                }
                Assertions.assertEquals(1, ds.getPoolMonitorVo().getIdleSize());
            }
        }
    }

    @Test
    public void testOnErrorState() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.addSqlExceptionState(errorState);

        MockConnectionProperties conProperties = new MockConnectionProperties();
        conProperties.setErrorState(errorState);
        conProperties.throwsExceptionWhenCallMethod("createStatement");
        config.setConnectionFactory(new MockConnectionFactory(conProperties));

        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection con = ds.getConnection()) {
                try {
                    con.createStatement();
                    fail("testOnErrorState");
                } catch (SQLException e) {
                    Assertions.assertEquals(errorState, e.getSQLState());
                }
                Assertions.assertEquals(1, ds.getPoolMonitorVo().getIdleSize());
            }
        }
    }

    @Test
    public void testOnPredicate() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(2);
        config.setInitialSize(2);
        config.setPredicate(new MockEvictConnectionPredicate1(errorCode, errorState));

        MockConnectionProperties conProperties = new MockConnectionProperties();
        conProperties.setErrorState(errorState);
        conProperties.setErrorCode(errorCode);
        conProperties.throwsExceptionWhenCallMethod("createStatement");
        config.setConnectionFactory(new MockConnectionFactory(conProperties));

        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection con = ds.getConnection()) {
                try {
                    con.createStatement();
                    fail("testOnErrorState");
                } catch (SQLException e) {
                    Assertions.assertEquals(errorCode, e.getErrorCode());
                }
                Assertions.assertEquals(1, ds.getPoolMonitorVo().getIdleSize());
            }
        }
    }

    @Test
    public void testOnAbort() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setPoolName("test");
        config.setInitialSize(4);
        config.setMaxActive(4);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assertions.assertEquals("test", vo.getPoolName());
            Assertions.assertEquals(4, vo.getMaxSize());
            Assertions.assertFalse(vo.isFairMode());
            Assertions.assertTrue(vo.isReady());

            Assertions.assertEquals(0, vo.getBorrowedSize());
            Assertions.assertEquals(4, vo.getIdleSize());

            Connection con = ds.getConnection();
            vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(1, vo.getBorrowedSize());
            Assertions.assertEquals(1, vo.getBorrowedSize());
            con.abort(null);

            vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(0, vo.getBorrowedSize());
            Assertions.assertEquals(3, vo.getIdleSize());
        }
    }
}
