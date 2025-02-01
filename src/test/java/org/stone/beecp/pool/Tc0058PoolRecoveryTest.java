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
import org.stone.base.StoneLogAppender;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.driver.MockConnectionProperties;
import org.stone.beecp.objects.MockCommonConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.base.TestUtil.getStoneLogAppender;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0058PoolRecoveryTest extends TestCase {

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
        Assert.assertEquals(3, pool.getTotalSize());
        Assert.assertEquals(3, pool.getIdleSize());

        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();

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
        Assert.assertNotNull(failException);
        Assert.assertEquals("network communication failed", failException.getMessage());//message from mock factory
        Assert.assertEquals(0, pool.getTotalSize());
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains("alive test failed on a borrowed connection"));

        //2: db restore here
        factory.setCreateException1(null);
        connectionProperties.disableExceptionOnMethod("isValid");//<--db restored

        //3: get connection after restore
        Connection con = pool.getConnection();
        Assert.assertNotNull(con);
        con.close();
        Assert.assertEquals(1, pool.getTotalSize());
        pool.close();
    }
}
