/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.dataSource;

import junit.framework.TestCase;
import org.stone.base.TestException;
import org.stone.beecp.*;

public class DsPoolClearTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setInitialSize(5);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testClear() throws Exception {
        BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
        if (vo.getIdleSize() != 5) throw new TestException();
        ds.clear(true);
        vo = ds.getPoolMonitorVo();
        if (vo.getIdleSize() != 0) throw new TestException();
    }

    public void testRestart() throws Exception {
        try {
            ds.clear(true, null);
        } catch (Exception e) {
            if (!(e instanceof BeeDataSourceConfigException)) throw new TestException();
        }

        try {
            BeeDataSourceConfig config = new BeeDataSourceConfig();
            config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
            config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
            config.setUsername(JdbcConfig.JDBC_USER);
            config.setInitialSize(20);
            config.setMaxActive(50);
            config.setMaxWait(30000);
            ds.clear(true, config);
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            if (vo.getIdleSize() != 20) throw new TestException();
        } catch (Exception e) {
            e.printStackTrace();
            if (!(e instanceof BeeDataSourceConfigException)) throw new TestException();
        }
    }
}
