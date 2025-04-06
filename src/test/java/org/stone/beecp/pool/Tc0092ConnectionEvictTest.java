/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.driver.MockConnectionProperties;
import org.stone.beecp.objects.MockCommonConnectionFactory;
import org.stone.beecp.objects.MockEvictConnectionPredicate;

import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.pool.ConnectionPoolStatics.POOL_READY;

/**
 * @author Chris Liao
 */
public class Tc0092ConnectionEvictTest extends TestCase {
    private final int errorCode = 0b010000;
    private final String errorState = "57P02";

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
        } catch (SQLException e) {
            Assert.assertEquals(errorCode, e.getErrorCode());
        }
        Assert.assertEquals(1, pool.getTotalSize());
        pool.close();
    }

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
        } catch (SQLException e) {
            Assert.assertEquals(errorState, e.getSQLState());
        }
        Assert.assertEquals(1, pool.getTotalSize());
        pool.close();
    }

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
        } catch (SQLException e) {
            Assert.assertEquals(errorCode, e.getErrorCode());
            Assert.assertEquals(errorState, e.getSQLState());
        }
        Assert.assertEquals(1, pool.getTotalSize());
        pool.close();
    }

    public void testOnAbort() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setPoolName("test");
        config.setInitialSize(4);
        config.setMaxActive(4);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        BeeConnectionPoolMonitorVo vo = pool.getPoolMonitorVo();
        Assert.assertEquals("test", vo.getPoolName());
        Assert.assertEquals(4, vo.getPoolMaxSize());
        Assert.assertEquals("compete", vo.getPoolMode());
        Assert.assertEquals(POOL_READY, vo.getPoolState());

        Assert.assertEquals(0, vo.getBorrowedSize());
        Assert.assertEquals(4, vo.getIdleSize());
        Connection con = pool.getConnection();
        vo = pool.getPoolMonitorVo();
        Assert.assertEquals(1, vo.getBorrowedSize());
        con.abort(null);

        vo = pool.getPoolMonitorVo();
        Assert.assertEquals(0, vo.getBorrowedSize());
        Assert.assertEquals(3, vo.getIdleSize());
        pool.close();
    }
}
