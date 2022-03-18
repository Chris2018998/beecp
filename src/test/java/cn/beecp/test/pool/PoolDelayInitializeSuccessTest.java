/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test.pool;

import cn.beecp.BeeDataSource;
import cn.beecp.pool.FastConnectionPool;
import cn.beecp.test.JdbcConfig;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;

import java.sql.Connection;

public class PoolDelayInitializeSuccessTest extends TestCase {
    private final int initSize = 5;

    public void setUp() throws Throwable {
    }

    public void tearDown() throws Throwable {
    }

    public void testPoolInit() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setJdbcUrl(JdbcConfig.JDBC_URL);
        ds.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        ds.setUsername(JdbcConfig.JDBC_USER);
        ds.setPassword(JdbcConfig.JDBC_PASSWORD);
        ds.setInitialSize(initSize);

        Connection con = null;
        try {
            con = ds.getConnection();
            FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
            if (pool.getTotalSize() != initSize)
                TestUtil.assertError("Total connections expected:%s,current is s%", initSize, pool.getTotalSize());
        } catch (ExceptionInInitializerError e) {
            e.getCause().printStackTrace();
        } finally {
            if (con != null)
                TestUtil.oclose(con);
            if (ds != null) ds.close();
        }
    }
}
