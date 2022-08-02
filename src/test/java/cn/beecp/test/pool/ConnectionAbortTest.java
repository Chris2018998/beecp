/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool;

import cn.beecp.*;

import java.sql.Connection;

public class ConnectionAbortTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        config.setInitialSize(6);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws Exception {
        Connection con = null;
        try {
            FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
            con = ds.getConnection();
            con.abort(null);

            if (pool.getTotalSize() != 5)
                TestUtil.assertError("Abort test failed");
        } finally {
            if (con != null)
                TestUtil.oclose(con);
        }
    }
}

