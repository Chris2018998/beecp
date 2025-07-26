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
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.test.base.LogCollector;
import org.stone.test.beecp.driver.MockConnectionProperties;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0058PoolRecoveryTest {

    @Test
    public void testRecoveryAfterDBRestoreTest() throws SQLException {
        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        MockCommonConnectionFactory factory = new MockCommonConnectionFactory(connectionProperties);

        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(3);
        config.setMaxActive(3);
        config.setPrintRuntimeLog(true);
        config.setAliveAssumeTime(0L);
        config.setConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assertions.assertEquals(3, pool.getTotalSize());
        Assertions.assertEquals(3, pool.getIdleSize());

        LogCollector logCollector = LogCollector.startLogCollector();
        SQLException failException = null;
        try {
            LockSupport.parkNanos(MILLISECONDS.toNanos(600L));
            connectionProperties.enableExceptionOnMethod("isValid"); //<----db crashed here
            connectionProperties.setMockException1(new SQLException("network communication failed"));
            factory.setCreateException1(connectionProperties.getMockException1());
            pool.getConnection();//all pooled connections dead
        } catch (SQLException e) {
            failException = e;
        }

        //1:check exception
        Assertions.assertNotNull(failException);
        Assertions.assertEquals("network communication failed", failException.getMessage());//message from mock factory
        Assertions.assertEquals(0, pool.getTotalSize());
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("alive test failed on a borrowed connection"));

        //2: db restore here
        factory.setCreateException1(null);
        connectionProperties.disableExceptionOnMethod("isValid");//<--db restored

        //3: get connection after restore
        Connection con = pool.getConnection();
        Assertions.assertNotNull(con);
        con.close();
        Assertions.assertEquals(1, pool.getTotalSize());
        pool.close();
    }
}
