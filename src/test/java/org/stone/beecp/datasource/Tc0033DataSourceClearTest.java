/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.datasource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0033DataSourceClearTest extends TestCase {

    public void testOnClear1() throws Exception {
        BeeDataSource ds = null;
        try {
            ds = new BeeDataSource(createDefault());
            ds.clear(true);
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assert.assertEquals(0, vo.getIdleSize());
        } finally {
            if (ds != null) ds.close();
        }
    }

    public void testOnClear2() throws Exception {
        BeeDataSource ds = null;
        try {
            ds = new BeeDataSource(createDefault());

            try {
                ds.clear(true, null);
            } catch (BeeDataSourceConfigException e) {
                Assert.assertTrue(e.getMessage().contains("Pool configuration object can't be null"));
            }

            BeeDataSourceConfig config2 = createDefault();
            config2.setInitialSize(2);
            ds.clear(true, config2);
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assert.assertEquals(2, vo.getIdleSize());
        } finally {
            if (ds != null) ds.close();
        }
    }
}
