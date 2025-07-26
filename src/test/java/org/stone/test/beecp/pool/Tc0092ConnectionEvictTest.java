/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.pool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.test.beecp.driver.MockConnectionProperties;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;
import org.stone.test.beecp.objects.MockEvictConnectionPredicate;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.beecp.pool.ConnectionPoolStatics.POOL_READY;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0092ConnectionEvictTest {
    private final int errorCode = 0b010000;
    private final String errorState = "57P02";

    @Test
    public void testOnErrorCode() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.addSqlExceptionCode(errorCode);

        MockConnectionProperties conProperties = new MockConnectionProperties();
        conProperties.setErrorCode(errorCode);
        conProperties.enableExceptionOnMethod("createStatement");
        config.setConnectionFactory(new MockCommonConnectionFactory(conProperties));

        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Connection con = pool.getConnection();

        try {
            con.createStatement();
            fail("testOnErrorCode");
        } catch (SQLException e) {
            Assertions.assertEquals(errorCode, e.getErrorCode());
        }
        Assertions.assertEquals(1, pool.getTotalSize());
        pool.close();
    }

    @Test
    public void testOnErrorState() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.addSqlExceptionState(errorState);

        MockConnectionProperties conProperties = new MockConnectionProperties();
        conProperties.setErrorState(errorState);
        conProperties.enableExceptionOnMethod("createStatement");
        config.setConnectionFactory(new MockCommonConnectionFactory(conProperties));

        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Connection con = pool.getConnection();

        try {
            con.createStatement();
            fail("testOnErrorState");
        } catch (SQLException e) {
            Assertions.assertEquals(errorState, e.getSQLState());
        }
        Assertions.assertEquals(1, pool.getTotalSize());
        pool.close();
    }

    @Test
    public void testOnPredicate() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(2);
        config.setInitialSize(2);
        config.setEvictPredicate(new MockEvictConnectionPredicate(errorCode, errorState));

        MockConnectionProperties conProperties = new MockConnectionProperties();
        conProperties.setErrorState(errorState);
        conProperties.setErrorCode(errorCode);
        conProperties.enableExceptionOnMethod("createStatement");
        config.setConnectionFactory(new MockCommonConnectionFactory(conProperties));
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Connection con = pool.getConnection();

        try {
            con.createStatement();
            fail("testOnPredicate");
        } catch (SQLException e) {
            Assertions.assertEquals(errorCode, e.getErrorCode());
            Assertions.assertEquals(errorState, e.getSQLState());
        }
        Assertions.assertEquals(1, pool.getTotalSize());
        pool.close();
    }

    @Test
    public void testOnAbort() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setPoolName("test");
        config.setInitialSize(4);
        config.setMaxActive(4);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        BeeConnectionPoolMonitorVo vo = pool.getPoolMonitorVo();
        Assertions.assertEquals("test", vo.getPoolName());
        Assertions.assertEquals(4, vo.getPoolMaxSize());
        Assertions.assertEquals("compete", vo.getPoolMode());
        Assertions.assertEquals(POOL_READY, vo.getPoolState());

        Assertions.assertEquals(0, vo.getBorrowedSize());
        Assertions.assertEquals(4, vo.getIdleSize());
        Connection con = pool.getConnection();
        vo = pool.getPoolMonitorVo();
        Assertions.assertEquals(1, vo.getBorrowedSize());
        con.abort(null);

        vo = pool.getPoolMonitorVo();
        Assertions.assertEquals(0, vo.getBorrowedSize());
        Assertions.assertEquals(3, vo.getIdleSize());
        pool.close();
    }
}
