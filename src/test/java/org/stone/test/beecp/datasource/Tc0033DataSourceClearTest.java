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
import org.stone.beecp.BeeDataSourceConfigException;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0033DataSourceClearTest {

    @Test
    public void testOnClear1() throws Exception {
        BeeDataSource ds = null;
        try {
            ds = new BeeDataSource(createDefault());
            ds.clear(true);
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(0, vo.getIdleSize());
        } finally {
            if (ds != null) ds.close();
        }
    }

    @Test
    public void testOnClear2() throws Exception {
        BeeDataSource ds = null;
        try {
            ds = new BeeDataSource(createDefault());

            try {
                ds.clear(true, null);
                fail("testOnClear2");
            } catch (BeeDataSourceConfigException e) {
                Assertions.assertTrue(e.getMessage().contains("Pool configuration object can't be null"));
            }

            BeeDataSourceConfig config2 = createDefault();
            config2.setInitialSize(2);
            ds.clear(true, config2);
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(2, vo.getIdleSize());
        } finally {
            if (ds != null) ds.close();
        }
    }
}
