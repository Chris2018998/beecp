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
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0065ConnectionTimeoutTest {

    @Test
    public void testIdleTimeout() throws Exception {
        final int initSize = 5;
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(initSize);
        config.setMaxActive(initSize);
        config.setIdleTimeout(1L);
        config.setPrintRuntimeLogs(true);
        config.setIntervalOfClearTimeout(100L);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertEquals(5, ds.getPoolMonitorVo().getIdleSize());
            synchronized (config) {
                config.wait(200L);
            }
            Assertions.assertEquals(0, ds.getPoolMonitorVo().getIdleSize());
        }
    }
}
