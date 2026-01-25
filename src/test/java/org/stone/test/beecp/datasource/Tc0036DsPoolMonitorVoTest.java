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

import java.sql.SQLException;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0036DsPoolMonitorVoTest {

    @Test
    public void testPoolMonitorVo() throws SQLException {
        BeeDataSource ds1 = new BeeDataSource();
        Assertions.assertTrue(ds1.getPoolMonitorVo().isLazy());

        BeeDataSourceConfig config = createDefault();
        config.setPoolName("fastPool");
        config.setInitialSize(10);
        config.setMaxActive(20);
        config.setFairMode(true);
        config.setSemaphoreSize(10);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assertions.assertTrue(ds.getPoolMonitorVo().isReady());
            Assertions.assertFalse(ds.getPoolMonitorVo().isNew());
            Assertions.assertFalse(ds.getPoolMonitorVo().isStarting());
            Assertions.assertFalse(ds.getPoolMonitorVo().isRestarting());
            Assertions.assertFalse(ds.getPoolMonitorVo().isLazy());
            Assertions.assertFalse(ds.getPoolMonitorVo().isClosing());

            Assertions.assertEquals("fastPool", vo.getPoolName());
            Assertions.assertTrue(vo.isFairMode());
            Assertions.assertTrue(vo.isReady());
            Assertions.assertEquals(20, vo.getMaxSize());
            Assertions.assertEquals(10, vo.getSemaphoreSize());
            Assertions.assertEquals(10, vo.getSemaphoreRemainSize());

            Assertions.assertEquals(10, vo.getIdleSize());
            Assertions.assertEquals(0, vo.getSemaphoreWaitingSize());
            Assertions.assertEquals(0, vo.getCreatingSize());
            Assertions.assertEquals(0, vo.getCreatingTimeoutSize());
        }
    }
}
