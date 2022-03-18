/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test.xa;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;

import javax.sql.XAConnection;

public class XaConnectionGetTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        String dataSourceClassName = "cn.beecp.test.mock.MockXaDataSource";
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