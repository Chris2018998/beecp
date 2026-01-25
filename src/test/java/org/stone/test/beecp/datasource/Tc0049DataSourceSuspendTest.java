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
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0049DataSourceSuspendTest {

    @Test
    public void testSuspend() throws Exception {
        BeeDataSourceConfig config = createDefault();
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertTrue(ds.getPoolMonitorVo().isReady());
            Assertions.assertFalse(ds.getPoolMonitorVo().isSuspended());

            Assertions.assertTrue(ds.suspend());
            Assertions.assertTrue(ds.getPoolMonitorVo().isSuspended());
            Assertions.assertEquals("Pool has been suspended", ds.toString());

            Assertions.assertTrue(ds.resume());
            Assertions.assertTrue(ds.getPoolMonitorVo().isReady());
            Assertions.assertEquals("Pool is ready", ds.toString());
        }
    }
}
