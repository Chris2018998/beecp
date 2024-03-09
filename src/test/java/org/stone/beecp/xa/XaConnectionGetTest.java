/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.xa;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import javax.sql.XAConnection;

public class XaConnectionGetTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        String dataSourceClassName = "org.stone.beecp.mock.MockXaDataSource";
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactoryClassName(dataSourceClassName);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws Exception {
        XAConnection con = null;
        try {
            con = ds.getXAConnection();
            if (con == null)
                TestUtil.assertError("Failed to get Connection");
        } finally {
            TestUtil.oclose(con);
        }
    }
}