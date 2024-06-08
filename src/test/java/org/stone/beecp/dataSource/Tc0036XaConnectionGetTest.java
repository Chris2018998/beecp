/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.dataSource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import javax.sql.XAConnection;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0036XaConnectionGetTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        String dataSourceClassName = "org.stone.beecp.driver.MockXaDataSource";
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactoryClassName(dataSourceClassName);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void test() throws Exception {
        XAConnection con = null;
        try {
            con = ds.getXAConnection();
            Assert.assertNotNull(con);
        } finally {
            oclose(con);
        }
    }
}